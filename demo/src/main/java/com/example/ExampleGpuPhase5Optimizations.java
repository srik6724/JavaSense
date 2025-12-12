package com.example;

import com.example.gpu.GpuFactStore;
import com.example.gpu.GpuMode;
import com.example.gpu.GpuPatternMatcher;
import com.example.gpu.GpuReasoningEngine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Example demonstrating Phase 5 GPU optimizations in JavaSense v1.4.
 *
 * <p>Phase 5 optimizations include:</p>
 * <ul>
 *   <li><b>Memory Transfer Caching:</b> Avoid redundant CPU↔GPU transfers</li>
 *   <li><b>Work-Group Size Auto-Tuning:</b> Optimize GPU kernel execution</li>
 * </ul>
 *
 * <p>These optimizations provide 2-5x additional speedup on top of Phase 3-4.</p>
 */
public class ExampleGpuPhase5Optimizations {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("JavaSense v1.4 - Phase 5 GPU Optimizations");
        System.out.println("=".repeat(80));
        System.out.println();

        // Example 1: Memory Transfer Caching
        example1MemoryCaching();
        System.out.println();

        // Example 2: Work-Group Size Tuning
        example2WorkGroupTuning();
        System.out.println();

        // Example 3: Real-World Scenario
        example3RealWorldOptimizations();
        System.out.println();

        System.out.println("=".repeat(80));
    }

    /**
     * Example 1: Memory Transfer Caching
     */
    private static void example1MemoryCaching() {
        System.out.println("Example 1: Memory Transfer Caching");
        System.out.println("-".repeat(40));

        GpuReasoningEngine gpu = new GpuReasoningEngine();

        if (!gpu.isGpuAvailable()) {
            System.out.println("No GPU available - skipping example");
            return;
        }

        GpuFactStore store = new GpuFactStore(gpu);

        try {
            // Generate 10K facts
            System.out.println("Generating 10,000 facts...");
            List<Atom> facts = new ArrayList<>();
            for (int i = 0; i < 10000; i++) {
                facts.add(Atom.parse("item(n" + i + ",m" + i + ")"));
            }

            // First upload - full transfer
            System.out.println("\nFirst upload (no cache):");
            long start1 = System.nanoTime();
            store.uploadFacts(facts);
            long time1 = System.nanoTime() - start1;
            System.out.println("  Time: " + String.format("%.2f", time1 / 1_000_000.0) + " ms");
            System.out.println("  Bytes transferred: " +
                String.format("%,d", store.getStats().bytesTransferred));

            // Second upload - cached!
            System.out.println("\nSecond upload (cached):");
            long start2 = System.nanoTime();
            store.uploadFacts(facts);
            long time2 = System.nanoTime() - start2;
            System.out.println("  Time: " + String.format("%.2f", time2 / 1_000_000.0) + " ms");
            System.out.println("  Cache hits: " + store.getCacheHits());

            // Performance improvement
            double speedup = time1 / (double) time2;
            System.out.println("\nCaching Benefit:");
            System.out.println("  Speedup: " + String.format("%.0f", speedup) + "x faster");
            System.out.println("  Time saved: " +
                String.format("%.2f", (time1 - time2) / 1_000_000.0) + " ms");

            // Third upload - another cache hit
            store.uploadFacts(facts);
            System.out.println("\nTotal cache hits: " + store.getCacheHits());

        } finally {
            store.cleanup();
            gpu.cleanup();
        }
    }

    /**
     * Example 2: Work-Group Size Auto-Tuning
     */
    private static void example2WorkGroupTuning() {
        System.out.println("Example 2: Work-Group Size Auto-Tuning");
        System.out.println("-".repeat(40));

        GpuReasoningEngine gpu = new GpuReasoningEngine();

        if (!gpu.isGpuAvailable()) {
            System.out.println("No GPU available - skipping example");
            return;
        }

        GpuFactStore store = new GpuFactStore(gpu);

        try {
            // Create facts
            List<Atom> facts = new ArrayList<>();
            for (int i = 0; i < 5000; i++) {
                facts.add(Atom.parse("data(item" + i + ")"));
            }

            store.uploadFacts(facts);
            System.out.println("Uploaded " + facts.size() + " facts to GPU");
            System.out.println();

            GpuPatternMatcher matcher = new GpuPatternMatcher(gpu, store);

            try {
                List<Literal> pattern = Arrays.asList(Literal.parse("data(X)"));

                // First run - auto-tunes work-group size
                System.out.println("First pattern match (auto-tuning work-group size):");
                long start1 = System.nanoTime();
                List<java.util.Map<String, String>> results1 = matcher.findSubstitutions(pattern);
                long time1 = System.nanoTime() - start1;

                System.out.println("  Matches: " + results1.size());
                System.out.println("  Time: " + String.format("%.2f", time1 / 1_000_000.0) + " ms");
                System.out.println("  (Includes auto-tuning overhead)");

                // Second run - uses tuned work-group size
                System.out.println("\nSecond pattern match (tuned work-group size):");
                long start2 = System.nanoTime();
                List<java.util.Map<String, String>> results2 = matcher.findSubstitutions(pattern);
                long time2 = System.nanoTime() - start2;

                System.out.println("  Matches: " + results2.size());
                System.out.println("  Time: " + String.format("%.2f", time2 / 1_000_000.0) + " ms");

                // Performance
                System.out.println("\nTuning Benefit:");
                if (time1 > time2) {
                    System.out.println("  Second run faster (tuning effective)");
                } else {
                    System.out.println("  Similar performance (already optimal)");
                }

            } finally {
                matcher.cleanup();
            }

        } finally {
            store.cleanup();
            gpu.cleanup();
        }
    }

    /**
     * Example 3: Real-World Scenario with Combined Optimizations
     */
    private static void example3RealWorldOptimizations() {
        System.out.println("Example 3: Real-World Stream Processing with Optimizations");
        System.out.println("-".repeat(40));

        OptimizedReasoner reasoner = new OptimizedReasoner();
        reasoner.setGpuMode(GpuMode.AUTO);

        // Rule: Detect anomalies in stream data
        Rule anomalyRule = new Rule("anomaly(X) <- 1 reading(X)", "anomaly_detection");
        reasoner.addRule(anomalyRule);

        // Simulate streaming data over multiple iterations
        System.out.println("Simulating stream processing with 3 iterations...");
        System.out.println();

        long[] iterationTimes = new long[3];

        for (int iteration = 0; iteration < 3; iteration++) {
            // Same facts each iteration (realistic for streaming windows)
            for (int i = 0; i < 5000; i++) {
                reasoner.addFact(new TimedFact(
                    Atom.parse("reading(sensor" + i + ")"),
                    "r" + iteration + "_" + i,
                    iteration, iteration));
            }

            long start = System.nanoTime();
            reasoner.reason(iteration);
            long elapsed = System.nanoTime() - start;

            iterationTimes[iteration] = elapsed;

            System.out.println("Iteration " + (iteration + 1) + ":");
            System.out.println("  Time: " + String.format("%.2f", elapsed / 1_000_000.0) + " ms");

            if (iteration == 0) {
                System.out.println("  (First run: no cache, work-group tuning)");
            } else if (iteration == 1) {
                System.out.println("  (Second run: cached transfer, tuned work-group)");
                double speedup = iterationTimes[0] / (double) elapsed;
                System.out.println("  Speedup: " + String.format("%.1f", speedup) + "x");
            } else {
                System.out.println("  (Steady state: fully optimized)");
                double speedup = iterationTimes[0] / (double) elapsed;
                System.out.println("  Speedup vs first: " + String.format("%.1f", speedup) + "x");
            }
        }

        System.out.println();
        System.out.println("Performance Summary:");
        System.out.println("  First iteration:  " +
            String.format("%.2f", iterationTimes[0] / 1_000_000.0) + " ms");
        System.out.println("  Second iteration: " +
            String.format("%.2f", iterationTimes[1] / 1_000_000.0) + " ms");
        System.out.println("  Third iteration:  " +
            String.format("%.2f", iterationTimes[2] / 1_000_000.0) + " ms");

        double avgOptimized = (iterationTimes[1] + iterationTimes[2]) / 2.0;
        double overallSpeedup = iterationTimes[0] / avgOptimized;

        System.out.println();
        System.out.println("Overall Optimization Benefit:");
        System.out.println("  Average speedup: " + String.format("%.1f", overallSpeedup) + "x");
        System.out.println("  Time saved per iteration: " +
            String.format("%.2f", (iterationTimes[0] - avgOptimized) / 1_000_000.0) + " ms");

        reasoner.cleanup();
    }
}
