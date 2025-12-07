package com.example;

import org.w3c.dom.Element;
import java.util.List;
import java.util.ArrayList;

/**
 * Benchmark Comparison: Baseline vs Optimized Reasoner
 *
 * <p>Tests all optimization levels on the Honda supply chain network.</p>
 *
 * <p><b>Optimizations tested:</b></p>
 * <ul>
 *   <li>Baseline: Original Reasoner</li>
 *   <li>Level 1: + Rule Indexing</li>
 *   <li>Level 2: + Sparse Storage</li>
 *   <li>Level 3: + Semi-Naive Evaluation</li>
 *   <li>Level 4: + Parallel Processing (future)</li>
 * </ul>
 */
public class BenchmarkOptimizations {

    public static void main(String[] args) {
        System.out.println("=== Optimization Benchmark: Honda Supply Chain ===\n");
        System.out.println("Network: 10,893 companies, 47,247 relationships");
        System.out.println("Timesteps: 5\n");

        // Load graph once
        System.out.println("Loading Honda supply chain network...");
        long startLoad = System.currentTimeMillis();
        Graph kb = Interpretation.loadKnowledgeBase("JP3854600008_honda.graphml");
        long loadTime = System.currentTimeMillis() - startLoad;
        System.out.println("✓ Loaded in " + (loadTime / 1000.0) + " seconds\n");

        System.out.println("Running benchmarks...\n");
        System.out.println("=" .repeat(80));

        // Benchmark 1: Baseline (original Reasoner)
        runBenchmark("Baseline (Original Reasoner)", kb, false, false, false, false);

        // Benchmark 2: + Rule Indexing
        runBenchmark("Level 1: + Rule Indexing", kb, true, false, false, false);

        // Benchmark 3: + Sparse Storage
        runBenchmark("Level 2: + Sparse Storage", kb, true, true, false, false);

        // Benchmark 4: + Semi-Naive Evaluation
        runBenchmark("Level 3: + Semi-Naive Evaluation", kb, true, true, true, false);

        System.out.println("=" .repeat(80));
        System.out.println("\n=== Summary ===");
        System.out.println("Expected speedups:");
        System.out.println("  Level 1 (Indexing):    5-10x faster");
        System.out.println("  Level 2 (+ Sparse):    5-10x faster, 6x less memory");
        System.out.println("  Level 3 (+ Semi-Naive): 20-50x faster, 6x less memory");
        System.out.println("\nNote: Actual speedups depend on graph structure and rule complexity.");
    }

    private static void runBenchmark(String name, Graph kb, boolean useIndexing,
                                    boolean useSparseStorage, boolean useSemiNaive,
                                    boolean useParallel) {
        System.out.println("\n--- " + name + " ---");

        // Create reasoner
        OptimizedReasoner reasoner = new OptimizedReasoner();

        // Add disruptions
        reasoner.addFact(convertFactToTimedFact(new Fact("disrupted(n3)", "steel_disruption", 1, 5)));
        reasoner.addFact(convertFactToTimedFact(new Fact("disrupted(n9)", "battery_disruption", 2, 5)));

        // Add base facts from graph (convert graph to facts)
        addGraphFacts(reasoner, kb);

        // Add rules
        reasoner.addRule(new Rule("cantDeliver(x) <-1 disrupted(x)", "direct_impact"));
        reasoner.addRule(new Rule("atRisk(y) <-1 cantDeliver(x), type(x,y)", "risk_via_type"));
        reasoner.addRule(new Rule("atRisk(y) <-1 cantDeliver(x), cost(x,y)", "risk_via_cost"));
        reasoner.addRule(new Rule("atRisk(y) <-1 cantDeliver(x), value(x,y)", "risk_via_value"));

        // Measure reasoning time
        System.out.println("Configuration:");
        System.out.println("  Indexing:      " + (useIndexing ? "✓" : "✗"));
        System.out.println("  Sparse Storage: " + (useSparseStorage ? "✓" : "✗"));
        System.out.println("  Semi-Naive:    " + (useSemiNaive ? "✓" : "✗"));
        System.out.println("  Parallel:      " + (useParallel ? "✓" : "✗"));
        System.out.println();

        // Suppress output for cleaner benchmark
        System.out.print("Reasoning...");
        long startReason = System.currentTimeMillis();

        ReasoningInterpretation result = reasoner.reason(5, useIndexing, useSparseStorage,
                                                         useSemiNaive, useParallel);

        long reasonTime = System.currentTimeMillis() - startReason;
        System.out.println(" done!");

        // Measure memory
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();  // Suggest garbage collection
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

        // Results
        long atRiskCount = Query.parse("atRisk(x)").atTime(5).execute(result).size();

        System.out.println("\nResults:");
        System.out.println("  Reasoning time: " + (reasonTime / 1000.0) + "s");
        System.out.println("  Avg per step:   " + (reasonTime / 5) + "ms");
        System.out.println("  Memory used:    ~" + usedMemory + " MB");
        System.out.println("  Companies at risk: " + atRiskCount);
        System.out.println("  Facts at t=5:   " + result.getFactsAt(5).size());
    }

    /**
     * Micro-benchmark: Test individual optimization components
     */
    public static void microBenchmark() {
        System.out.println("=== Micro-Benchmarks ===\n");

        // Test 1: Fact lookup with vs without indexing
        System.out.println("Test 1: Fact Lookup Speed");
        System.out.println("Creating 10,000 facts...");
        // ... micro-benchmark code ...

        // Test 2: Memory usage with sparse vs dense storage
        System.out.println("\nTest 2: Memory Usage");
        // ... micro-benchmark code ...

        // Test 3: Semi-naive iteration count
        System.out.println("\nTest 3: Iteration Count");
        // ... micro-benchmark code ...
    }

    /**
     * Helper: Convert Fact to TimedFact
     */
    private static TimedFact convertFactToTimedFact(Fact f) {
        Atom atom = Atom.parse(f.getText());
        List<Interval> intervals = List.of(new Interval(f.getStartTime(), f.getEndTime()));
        return new TimedFact(atom, f.getName(), intervals);
    }

    /**
     * Helper: Add graph facts to reasoner
     */
    private static void addGraphFacts(OptimizedReasoner reasoner, Graph kb) {
        // Process edges from graph
        for (Element edge : kb.getEdges()) {
            String source = edge.getAttribute("source");
            String target = edge.getAttribute("target");

            // Add edge predicates (type, cost, value)
            for (Element dataEl : getChildElements(edge, "data")) {
                String key = dataEl.getAttribute("key");
                String value = dataEl.getTextContent();
                String attrName = kb.getKeyIdToName().getOrDefault(key, key);

                // Create fact for this edge attribute
                Atom atom = new Atom(attrName, List.of(source, target));
                TimedFact timedFact = new TimedFact(atom, "graph_" + source + "_" + target + "_" + attrName,
                    List.of(new Interval(0, Integer.MAX_VALUE)));  // Static facts
                reasoner.addFact(timedFact);
            }
        }

        // Process node attributes
        for (Element node : kb.getNodes()) {
            String nodeId = node.getAttribute("id");

            for (Element dataEl : getChildElements(node, "data")) {
                String key = dataEl.getAttribute("key");
                String value = dataEl.getTextContent();
                String attrName = kb.getKeyIdToName().getOrDefault(key, key);

                // Create fact for node attribute
                Atom atom = new Atom(attrName, List.of(nodeId, value));
                TimedFact timedFact = new TimedFact(atom, "graph_node_" + nodeId + "_" + attrName,
                    List.of(new Interval(0, Integer.MAX_VALUE)));
                reasoner.addFact(timedFact);
            }
        }
    }

    /**
     * Helper: Get child elements with specific tag name
     */
    private static List<Element> getChildElements(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();
        org.w3c.dom.NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child instanceof Element && child.getNodeName().equals(tagName)) {
                result.add((Element) child);
            }
        }
        return result;
    }
}
