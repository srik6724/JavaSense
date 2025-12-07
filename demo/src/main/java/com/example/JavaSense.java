package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaSense - A temporal logical reasoning engine for knowledge graphs.
 *
 * <p>JavaSense provides a high-level API for performing time-aware forward chaining
 * inference on knowledge graphs. It supports temporal facts, rules with delays,
 * and reasoning across discrete time steps.</p>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Load knowledge graph
 * Graph kb = Interpretation.loadKnowledgeBase("graph.graphml");
 *
 * // Add initial facts
 * JavaSense.addFact(new Fact("popular(Mary)", "fact1", 0, 2));
 *
 * // Define reasoning rules
 * JavaSense.addRule(new Rule(
 *     "popular(x) <-1 popular(y), Friends(x,y)",
 *     "popularity_spread"
 * ));
 *
 * // Run inference
 * ReasoningInterpretation result = JavaSense.reason(kb, 10);
 * }</pre>
 *
 * @version 1.0
 * @since 1.0
 */
public class JavaSense {
    private static final Logger logger = LoggerFactory.getLogger(JavaSense.class);

    private static final List<Fact> facts = new ArrayList<>();
    private static final List<Rule> rules = new ArrayList<>();
    private static final List<TimedFact> directTimedFacts = new ArrayList<>(); 

    /**
     * Adds a reasoning rule to the engine.
     *
     * <p>Rules define how new facts can be inferred from existing facts. They follow
     * Horn clause syntax with temporal extensions:</p>
     *
     * <pre>head(vars) [: interval] &lt;-delay body1(vars), body2(vars), ...</pre>
     *
     * @param rule the rule to add
     * @throws NullPointerException if rule is null
     * @see Rule
     */
    public static void addRule(Rule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Rule cannot be null");
        }
        rules.add(rule);
        logger.info("Added rule: {}", rule);
    }

    /**
     * Loads reasoning rules from a text file.
     *
     * <p>File format: One rule per line. Lines starting with '#' are treated as comments.
     * Empty lines are ignored.</p>
     *
     * <p>Example file content:</p>
     * <pre>
     * # Popularity spreading rule
     * popular(x) &lt;-1 popular(y), Friends(x,y)
     * # Trendy customer rule
     * trendy(x) &lt;- owns(x,y), Car(y)
     * </pre>
     *
     * @param fileName path to the rules file
     * @throws IOException if the file cannot be read
     */
    public static void addRulesFromFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            int counter = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String name = "rule_" + counter++;
                addRule(new Rule(line, name));
            }
        } catch (IOException e) {
            logger.error("Failed to load rules from {}", fileName, e);
        }
    }

    /**
     * Adds a simple fact to the knowledge base.
     *
     * <p>Facts represent ground truth statements that are valid within a specific
     * time interval [startTime, endTime].</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Mary is popular from timestep 0 to 2
     * JavaSense.addFact(new Fact("popular(Mary)", "mary_popular", 0, 2));
     * }</pre>
     *
     * @param fact the fact to add
     * @throws NullPointerException if fact is null
     * @see Fact
     * @see TimedFact
     */
    public static void addFact(Fact fact) {
        if (fact == null) {
            throw new IllegalArgumentException("Fact cannot be null");
        }
        if (fact.getStartTime() < 0) {
            throw new IllegalArgumentException("Fact start time cannot be negative: " + fact.getStartTime());
        }
        if (fact.getEndTime() < fact.getStartTime()) {
            throw new IllegalArgumentException(
                "Fact end time (" + fact.getEndTime() + ") cannot be less than start time (" + fact.getStartTime() + ")"
            );
        }
        facts.add(fact);
        logger.info("Added fact: {}", fact);
    }

    /**
     * Adds a timed fact with multiple disjoint time intervals.
     *
     * <p>Timed facts allow specifying multiple time windows when a fact is valid.
     * This is useful for modeling facts that are true at different, non-contiguous periods.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Mary is popular during [0,2] and [5,8]
     * JavaSense.addTimedFact(new TimedFact(
     *     Atom.parse("popular(Mary)"),
     *     "mary_popular",
     *     List.of(new Interval(0, 2), new Interval(5, 8))
     * ));
     * }</pre>
     *
     * @param timedFact the timed fact to add
     * @throws NullPointerException if timedFact is null
     * @see TimedFact
     * @see Interval
     */
    public static void addTimedFact(TimedFact timedFact) {
    if (timedFact == null) {
        throw new IllegalArgumentException("TimedFact cannot be null");
    }
    if (timedFact.getIntervals() == null || timedFact.getIntervals().isEmpty()) {
        throw new IllegalArgumentException("TimedFact must have at least one interval");
    }
    // Convert TimedFact into a Fact placeholder or store it separately
    // but easiest is: store TimedFact right away
    directTimedFacts.add(timedFact);
    logger.info("Added timed fact: {}", timedFact);
}

    /**
     * Loads facts from a CSV-formatted text file.
     *
     * <p>File format: One fact per line in CSV format. Lines starting with '#' are comments.
     * Empty lines are ignored.</p>
     *
     * <p>Format: {@code predicate(args),fact_name,start_time,end_time}</p>
     *
     * <p>Example file content:</p>
     * <pre>
     * # Initial facts
     * popular(Mary),mary_fact,0,2
     * popular(John),john_fact,1,3
     * owns(Alice,Car123),alice_car,0,10
     * </pre>
     *
     * @param fileName path to the facts file
     * @throws IOException if the file cannot be read
     * @throws NumberFormatException if time values are not valid integers
     */
    public static void addFactsFromFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                // Parse format: "predicate(args),fact_name,start_time,end_time"
                // Example: "popular(Mary),popular_fact,0,2"
                String[] parts = line.split(",");
                if (parts.length < 4) {
                    logger.warn("Invalid fact format (expected 4 parts): {}", line);
                    continue;
                }

                String factText = parts[0].trim();
                String factName = parts[1].trim();
                int startTime = Integer.parseInt(parts[2].trim());
                int endTime = Integer.parseInt(parts[3].trim());

                addFact(new Fact(factText, factName, startTime, endTime));
            }
        } catch (IOException e) {
            logger.error("Failed to load facts from {}", fileName, e);
        } catch (NumberFormatException e) {
            logger.error("Invalid time format in facts file {}", fileName, e);
        }
    }

    /**
     * Executes temporal reasoning over the knowledge base.
     *
     * <p>This method performs forward chaining inference across the specified number
     * of timesteps. At each timestep, rules are applied to derive new facts based on
     * existing facts and the knowledge graph.</p>
     *
     * <p>Process:</p>
     * <ol>
     *   <li>Load facts from the knowledge graph (edges become facts)</li>
     *   <li>Add all explicitly defined facts</li>
     *   <li>Apply rules iteratively at each timestep</li>
     *   <li>Derive new facts through unification and pattern matching</li>
     *   <li>Return all facts organized by timestep</li>
     * </ol>
     *
     * @param kb the knowledge graph (can be null if reasoning without a graph)
     * @param timesteps number of discrete time steps to reason over (must be >= 0)
     * @return interpretation containing all derived facts organized by timestep
     * @throws IllegalArgumentException if timesteps is negative
     * @see ReasoningInterpretation
     * @see Graph
     */
    public static ReasoningInterpretation reason(Graph kb, int timesteps) {
    if (timesteps < 0) {
        throw new IllegalArgumentException("Timesteps cannot be negative: " + timesteps);
    }
    if (timesteps > 10000) {
        logger.warn("Large timestep value ({}), this may take significant time and memory", timesteps);
    }

    logger.info("=== JavaSense reasoning started ===");
    logger.info("Timesteps: {}", timesteps);
    logger.info("Rules: {}", rules.size());
    logger.info("Facts: {}", facts.size());

    Reasoner engine = new Reasoner();

    if (kb != null) {
        List<TimedFact> graphFacts = GraphToFactsConverter.fromGraph(kb, timesteps);
        for (TimedFact tf : graphFacts) {
            engine.addFact(tf);
        }
    }

    for (TimedFact tf : directTimedFacts) {
    engine.addFact(tf);
}

    // 1) convert your Fact objects -> TimedFact + Atom
    for (Fact f : facts) {
        Atom atom = Atom.parse(f.getText()); // e.g. "popular(Mary)"
        TimedFact tf = new TimedFact(
                atom,
                f.getName(),
                f.getStartTime(),
                f.getEndTime()
        );
        engine.addFact(tf);
    }

    // 2) add rules
    for (Rule r : rules) {
        engine.addRule(r);
    }

    // 3) run engine
    ReasoningInterpretation interp = engine.reason(timesteps);

    logger.info("=== JavaSense reasoning completed ===");
    return interp;
}
}
