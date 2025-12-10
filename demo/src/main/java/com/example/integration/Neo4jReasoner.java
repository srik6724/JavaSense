package com.example.integration;

import com.example.*;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

/**
 * Production Neo4j Integration for JavaSense - Reason over graph databases
 *
 * <p>Load knowledge graphs from Neo4j, perform temporal reasoning, and write results back.
 * Perfect for combining JavaSense's temporal reasoning with Neo4j's graph storage.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>Load from Cypher:</b> Import nodes/edges via Cypher queries</li>
 *   <li><b>Automatic conversion:</b> Neo4j nodes/edges → JavaSense facts</li>
 *   <li><b>Write back:</b> Store derivations in Neo4j</li>
 *   <li><b>Property mapping:</b> Flexible mapping of properties to predicates</li>
 *   <li><b>Production-ready:</b> Full Neo4j driver integration with connection pooling</li>
 * </ul>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Connect to Neo4j
 * Neo4jReasoner reasoner = Neo4jReasoner.connect(
 *     "bolt://localhost:7687",
 *     "neo4j",
 *     "password"
 * );
 *
 * // Load supply chain graph
 * reasoner.loadFromCypher(
 *     "MATCH (s:Supplier)-[:SUPPLIES]->(p:Part) RETURN s, p",
 *     (node) -> nodeToFact(node),      // Convert nodes to facts
 *     (edge) -> edgeToFact(edge)       // Convert edges to facts
 * );
 *
 * // Add reasoning rules
 * reasoner.addRule(new Rule("atRisk(x) <-1 disrupted(y), dependsOn(x,y)"));
 *
 * // Reason
 * ReasoningInterpretation result = reasoner.reason(10);
 *
 * // Write results back to Neo4j
 * reasoner.writeDerivationsToNeo4j(result, "atRisk", "RiskAlert");
 *
 * // Close connection
 * reasoner.close();
 * }</pre>
 */
public class Neo4jReasoner implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(Neo4jReasoner.class);

    private final String uri;
    private final OptimizedReasoner reasoner;
    private Driver driver;
    private int defaultMaxTimesteps = 100;

    // Statistics
    private long nodesLoaded = 0;
    private long edgesLoaded = 0;
    private long factsWritten = 0;

    private Neo4jReasoner(String uri, String username, String password) {
        this.uri = uri;
        this.reasoner = new OptimizedReasoner();

        logger.info("Connecting to Neo4j at {}", uri);
        this.driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));

        // Verify connectivity
        try {
            driver.verifyConnectivity();
            logger.info("Successfully connected to Neo4j");
        } catch (Exception e) {
            logger.error("Failed to connect to Neo4j: {}", e.getMessage(), e);
            throw new RuntimeException("Neo4j connection failed", e);
        }
    }

    /**
     * Connects to Neo4j database.
     *
     * @param uri Neo4j URI (e.g., "bolt://localhost:7687" or "neo4j://localhost:7687")
     * @param username database username
     * @param password database password
     * @return configured Neo4jReasoner
     */
    public static Neo4jReasoner connect(String uri, String username, String password) {
        return new Neo4jReasoner(uri, username, password);
    }

    /**
     * Loads nodes and edges from Neo4j using Cypher query.
     *
     * <p>Example converters:</p>
     * <pre>{@code
     * // Node converter
     * nodeConverter = (node) -> {
     *     String id = node.get("id").asString();
     *     String type = node.get("type").asString();
     *     return new TimedFact(
     *         Atom.parse("type(" + id + "," + type + ")"),
     *         "node_" + id,
     *         List.of(new Interval(0, 100))
     *     );
     * };
     *
     * // Relationship converter
     * relConverter = (rel) -> {
     *     long sourceId = rel.startNodeElementId();
     *     long targetId = rel.endNodeElementId();
     *     String relType = rel.type();
     *     return new TimedFact(
     *         Atom.parse(relType + "(" + sourceId + "," + targetId + ")"),
     *         "rel_" + rel.elementId(),
     *         List.of(new Interval(0, 100))
     *     );
     * };
     * }</pre>
     *
     * @param cypherQuery the Cypher query to execute
     * @param nodeConverter converts Neo4j nodes to TimedFacts (can be null)
     * @param relConverter converts Neo4j relationships to TimedFacts (can be null)
     */
    public void loadFromCypher(String cypherQuery,
                              Function<Node, TimedFact> nodeConverter,
                              Function<Relationship, TimedFact> relConverter) {
        logger.info("Loading from Neo4j with query: {}", cypherQuery);

        try (Session session = driver.session()) {
            Result result = session.run(cypherQuery);

            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();

                // Process all values in the record
                for (Value value : record.values()) {
                    if (value.type().name().equals("NODE") && nodeConverter != null) {
                        Node node = value.asNode();
                        TimedFact fact = nodeConverter.apply(node);
                        if (fact != null) {
                            reasoner.addFact(fact);
                            nodesLoaded++;
                        }
                    } else if (value.type().name().equals("RELATIONSHIP") && relConverter != null) {
                        Relationship rel = value.asRelationship();
                        TimedFact fact = relConverter.apply(rel);
                        if (fact != null) {
                            reasoner.addFact(fact);
                            edgesLoaded++;
                        }
                    }
                }
            }

            logger.info("Loaded {} nodes and {} edges from Neo4j", nodesLoaded, edgesLoaded);
        } catch (Exception e) {
            logger.error("Error loading from Neo4j: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load from Neo4j", e);
        }
    }

    /**
     * Loads a simple property graph from Neo4j.
     *
     * <p>Automatically creates facts:</p>
     * <ul>
     *   <li>Node labels → hasLabel(nodeId, Label)</li>
     *   <li>Node properties → property(nodeId, key, value)</li>
     *   <li>Edges → edge(sourceId, targetId, relType)</li>
     * </ul>
     *
     * @param nodeLabel filter by node label (null for all nodes)
     * @param relationType filter by relationship type (null for all)
     */
    public void loadPropertyGraph(String nodeLabel, String relationType) {
        String query = buildPropertyGraphQuery(nodeLabel, relationType);
        logger.info("Loading property graph with query: {}", query);

        try (Session session = driver.session()) {
            Result result = session.run(query);

            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();

                // Process node n
                if (record.containsKey("n")) {
                    Node node = record.get("n").asNode();
                    processNodeAsProperties(node);
                    nodesLoaded++;
                }

                // Process relationship r
                if (record.containsKey("r")) {
                    Relationship rel = record.get("r").asRelationship();
                    processRelationshipAsProperty(rel);
                    edgesLoaded++;
                }

                // Process node m
                if (record.containsKey("m")) {
                    Node node = record.get("m").asNode();
                    processNodeAsProperties(node);
                    nodesLoaded++;
                }
            }

            logger.info("Loaded property graph: {} nodes, {} edges", nodesLoaded, edgesLoaded);
        } catch (Exception e) {
            logger.error("Error loading property graph: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load property graph", e);
        }
    }

    /**
     * Writes derived facts back to Neo4j as nodes.
     *
     * @param interpretation the reasoning result
     * @param predicate the predicate to write (e.g., "atRisk")
     * @param nodeLabel the Neo4j label for created nodes (e.g., "RiskAlert")
     */
    public void writeDerivationsToNeo4j(ReasoningInterpretation interpretation,
                                       String predicate,
                                       String nodeLabel) {
        logger.info("Writing derivations for predicate '{}' to Neo4j as :{}", predicate, nodeLabel);

        int count = 0;
        try (Session session = driver.session()) {
            for (int t = 0; t <= interpretation.getMaxTime(); t++) {
                Set<Atom> factsAtTime = interpretation.getFactsAt(t);
                for (Atom atom : factsAtTime) {
                    if (atom.getPredicate().equals(predicate)) {
                        // Create Cypher query to create node
                        String cypher = "CREATE (n:" + nodeLabel + " {atom: $atom, time: $time, args: $args})";
                        Map<String, Object> params = new HashMap<>();
                        params.put("atom", atom.toString());
                        params.put("time", t);
                        params.put("args", atom.getArgs());

                        session.run(cypher, params);
                        count++;
                        factsWritten++;
                    }
                }
            }

            logger.info("Wrote {} derivations to Neo4j as :{} nodes", count, nodeLabel);
        } catch (Exception e) {
            logger.error("Error writing to Neo4j: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to write to Neo4j", e);
        }
    }

    /**
     * Sets the default max timesteps for reasoning.
     */
    public void setDefaultMaxTimesteps(int timesteps) {
        this.defaultMaxTimesteps = timesteps;
    }

    /**
     * Adds a rule to the reasoner.
     */
    public void addRule(Rule rule) {
        reasoner.addRule(rule);
    }

    /**
     * Adds a fact to the reasoner.
     */
    public void addFact(TimedFact fact) {
        reasoner.addFact(fact);
    }

    /**
     * Performs reasoning.
     *
     * @param timesteps number of timesteps
     * @return reasoning result
     */
    public ReasoningInterpretation reason(int timesteps) {
        return reasoner.reason(timesteps);
    }

    /**
     * Performs reasoning with default timesteps.
     *
     * @return reasoning result
     */
    public ReasoningInterpretation reason() {
        return reasoner.reason(defaultMaxTimesteps);
    }

    /**
     * Gets statistics about loading/writing.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("nodesLoaded", nodesLoaded);
        stats.put("edgesLoaded", edgesLoaded);
        stats.put("factsWritten", factsWritten);
        stats.put("connected", driver != null);
        return stats;
    }

    /**
     * Closes the Neo4j connection.
     */
    @Override
    public void close() {
        if (driver != null) {
            logger.info("Closing Neo4j connection");
            driver.close();
            driver = null;
        }
    }

    // --- Internal Methods ---

    private String buildPropertyGraphQuery(String nodeLabel, String relationType) {
        StringBuilder query = new StringBuilder("MATCH (n");
        if (nodeLabel != null) {
            query.append(":").append(nodeLabel);
        }
        query.append(")-[r");
        if (relationType != null) {
            query.append(":").append(relationType);
        }
        query.append("]->(m) RETURN n, r, m");
        return query.toString();
    }

    private void processNodeAsProperties(Node node) {
        String nodeId = String.valueOf(node.elementId());

        // Add label facts: hasLabel(nodeId, Label)
        for (String label : node.labels()) {
            TimedFact fact = new TimedFact(
                Atom.parse("hasLabel(" + nodeId + "," + label + ")"),
                "label_" + nodeId + "_" + label,
                List.of(new Interval(0, defaultMaxTimesteps))
            );
            reasoner.addFact(fact);
        }

        // Add property facts: property(nodeId, key, value)
        for (String key : node.keys()) {
            Object value = node.get(key).asObject();
            TimedFact fact = new TimedFact(
                Atom.parse("property(" + nodeId + "," + key + "," + value + ")"),
                "prop_" + nodeId + "_" + key,
                List.of(new Interval(0, defaultMaxTimesteps))
            );
            reasoner.addFact(fact);
        }
    }

    private void processRelationshipAsProperty(Relationship rel) {
        String sourceId = String.valueOf(rel.startNodeElementId());
        String targetId = String.valueOf(rel.endNodeElementId());
        String relType = rel.type();

        // Add edge fact: relType(sourceId, targetId)
        TimedFact fact = new TimedFact(
            Atom.parse(relType + "(" + sourceId + "," + targetId + ")"),
            "rel_" + rel.elementId(),
            List.of(new Interval(0, defaultMaxTimesteps))
        );
        reasoner.addFact(fact);
    }

    // --- Helper: Default Converters ---

    /**
     * Creates a simple node converter that extracts an "id" property.
     *
     * <p>Creates facts: node(id, label1, label2, ...)</p>
     */
    public static Function<Node, TimedFact> simpleNodeConverter(int maxTimesteps) {
        return node -> {
            // Try to get "id" property, fallback to elementId
            String id;
            if (node.containsKey("id")) {
                id = node.get("id").asString();
            } else {
                id = String.valueOf(node.elementId());
            }
            List<String> labels = new ArrayList<>();
            node.labels().forEach(labels::add);

            String atomStr = "node(" + id + (labels.isEmpty() ? "" : "," + String.join(",", labels)) + ")";
            return new TimedFact(
                Atom.parse(atomStr),
                "node_" + id,
                List.of(new Interval(0, maxTimesteps))
            );
        };
    }

    /**
     * Creates a simple relationship converter.
     *
     * <p>Creates facts: relType(sourceId, targetId)</p>
     */
    public static Function<Relationship, TimedFact> simpleRelConverter(int maxTimesteps) {
        return rel -> {
            String source = String.valueOf(rel.startNodeElementId());
            String target = String.valueOf(rel.endNodeElementId());
            String relType = rel.type().toLowerCase();

            String atomStr = relType + "(" + source + "," + target + ")";
            return new TimedFact(
                Atom.parse(atomStr),
                "rel_" + rel.elementId(),
                List.of(new Interval(0, maxTimesteps))
            );
        };
    }
}
