package com.example;

import com.example.integration.Neo4jReasoner;

/**
 * Production Neo4j Integration Example
 *
 * <p>This example shows how to use JavaSense with a real Neo4j database
 * for supply chain risk analysis.</p>
 *
 * <h2>Prerequisites:</h2>
 * <ul>
 *   <li>Neo4j running at bolt://localhost:7687</li>
 *   <li>Database with supply chain data</li>
 * </ul>
 *
 * <h2>To run:</h2>
 * <pre>
 * # Start Neo4j (if not running)
 * # On macOS/Linux:
 * neo4j start
 *
 * # Access Neo4j Browser: http://localhost:7474
 * # Default credentials: neo4j / neo4j (change on first login)
 *
 * # Create sample supply chain data:
 * CREATE (s1:Supplier {id: 'ACME', name: 'ACME Corp', status: 'active'})
 * CREATE (s2:Supplier {id: 'GLOBEX', name: 'Globex Inc', status: 'disrupted'})
 * CREATE (p1:Part {id: 'ENGINE', name: 'Engine'})
 * CREATE (p2:Part {id: 'WHEEL', name: 'Wheel'})
 * CREATE (s1)-[:SUPPLIES]->(p1)
 * CREATE (s2)-[:SUPPLIES]->(p2)
 *
 * # Run this example
 * mvn exec:java -Dexec.mainClass="com.example.ExampleProductionNeo4j"
 * </pre>
 */
public class ExampleProductionNeo4j {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("JavaSense + Neo4j - Production Supply Chain Risk Analysis");
        System.out.println("=".repeat(80));
        System.out.println();

        // Connect to Neo4j
        System.out.println("Connecting to Neo4j at bolt://localhost:7687...");
        try (Neo4jReasoner reasoner = Neo4jReasoner.connect(
                "bolt://localhost:7687",
                "neo4j",
                "password"  // Change to your password
        )) {
            System.out.println("✓ Connected to Neo4j\n");

            // Load supply chain graph
            System.out.println("Loading supply chain from Neo4j...");
            reasoner.loadFromCypher(
                "MATCH (s:Supplier)-[r:SUPPLIES]->(p:Part) RETURN s, r, p",
                Neo4jReasoner.simpleNodeConverter(100),
                Neo4jReasoner.simpleRelConverter(100)
            );

            // Load supplier status
            reasoner.loadFromCypher(
                "MATCH (s:Supplier) WHERE s.status = 'disrupted' RETURN s",
                node -> {
                    String id = node.get("id").asString();
                    return new TimedFact(
                        Atom.parse("disrupted(" + id + ")"),
                        "disrupted_" + id,
                        java.util.List.of(new Interval(0, 100))
                    );
                },
                null
            );

            System.out.println("✓ Loaded supply chain data\n");
            System.out.println("Statistics:");
            reasoner.getStatistics().forEach((key, value) ->
                System.out.println("  " + key + ": " + value)
            );

            // Add risk analysis rules
            System.out.println("\nAdding risk analysis rules...");
            reasoner.addRule(new Rule(
                "atRisk(part) <-1 disrupted(supplier), supplies(supplier,part)",
                "supply_chain_risk"
            ));

            reasoner.addRule(new Rule(
                "critical(part) <-1 atRisk(part), essential(part)",
                "critical_part_risk"
            ));

            System.out.println("✓ Rules added\n");

            // Perform reasoning
            System.out.println("Performing risk analysis...");
            ReasoningInterpretation result = reasoner.reason(10);
            System.out.println("✓ Reasoning complete\n");

            // Query results
            System.out.println("=== Risk Analysis Results ===\n");

            Query atRiskQuery = Query.parse("atRisk(x)").atTime(1);
            java.util.List<QueryResult> atRiskResults = atRiskQuery.execute(result);

            if (atRiskResults.isEmpty()) {
                System.out.println("No parts at risk found.");
            } else {
                System.out.println("Parts at risk:");
                for (QueryResult r : atRiskResults) {
                    String part = r.getBinding("x");
                    System.out.println("  • " + part);
                }
            }

            // Write results back to Neo4j
            System.out.println("\nWriting risk alerts back to Neo4j...");
            reasoner.writeDerivationsToNeo4j(result, "atRisk", "RiskAlert");
            System.out.println("✓ Results written to Neo4j as :RiskAlert nodes\n");

            // Final statistics
            System.out.println("=== Final Statistics ===");
            reasoner.getStatistics().forEach((key, value) ->
                System.out.println("  " + key + ": " + value)
            );

            System.out.println("\n✓ Analysis complete!");
            System.out.println("\nTo view results in Neo4j Browser:");
            System.out.println("  MATCH (r:RiskAlert) RETURN r");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n" + "=".repeat(80));
    }
}
