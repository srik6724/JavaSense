package com.example;

import com.example.gpu.GpuMode;
import com.example.gpu.GpuReasoningEngine;

/**
 * Comprehensive example demonstrating GPU-accelerated reasoning in JavaSense v1.4.
 *
 * <p>This example shows:</p>
 * <ul>
 *   <li>GPU detection and configuration</li>
 *   <li>Performance comparison (CPU vs GPU)</li>
 *   <li>AUTO mode for automatic optimization</li>
 *   <li>Fallback behavior for unsupported operations</li>
 *   <li>Best practices for GPU reasoning</li>
 * </ul>
 */
public class ExampleGpuComprehensive {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("JavaSense v1.4 - Comprehensive GPU Acceleration Demo");
        System.out.println("=".repeat(80));
        System.out.println();

        // Example 1: GPU Detection
        example1GpuDetection();
        System.out.println();

        // Example 2: Performance Comparison
        example2PerformanceComparison();
        System.out.println();

        // Example 3: AUTO Mode
        example3AutoMode();
        System.out.println();

        // Example 4: Fallback Behavior
        example4FallbackBehavior();
        System.out.println();

        // Example 5: Real-World Use Case
        example5RealWorldUseCase();
        System.out.println();

        System.out.println("=".repeat(80));
        System.out.println("Demo complete!");
        System.out.println("=".repeat(80));
    }

    /**
     * Example 1: GPU Detection and Configuration
     */
    private static void example1GpuDetection() {
        System.out.println("Example 1: GPU Detection and Configuration");
        System.out.println("-".repeat(40));

        // Check if GPU is available
        GpuReasoningEngine gpu = new GpuReasoningEngine();

        if (gpu.isGpuAvailable()) {
            System.out.println("✓ GPU detected!");
            System.out.println("  GPU Info: " + gpu.getGpuInfo());
            System.out.println();

            // Show configuration options
            System.out.println("GPU Configuration:");
            System.out.println("  - Minimum facts for GPU: 1000 (default)");
            System.out.println("  - Minimum rules for GPU: 10 (default)");
            System.out.println("  - Can be customized via setGpuThresholds()");
        } else {
            System.out.println("✗ No GPU detected");
            System.out.println("  GPU Info: " + gpu.getGpuInfo());
            System.out.println("  The system will automatically use CPU");
        }

        gpu.cleanup();
    }

    /**
     * Example 2: Performance Comparison (CPU vs GPU)
     */
    private static void example2PerformanceComparison() {
        System.out.println("Example 2: Performance Comparison (CPU vs GPU)");
        System.out.println("-".repeat(40));

        int[] sizes = {100, 1000, 5000};

        System.out.printf("%-15s %-15s %-15s %-15s%n",
            "Dataset Size", "CPU Time", "GPU Time", "Speedup");
        System.out.println("-".repeat(60));

        for (int size : sizes) {
            // Benchmark CPU
            OptimizedReasoner cpuReasoner = new OptimizedReasoner();
            cpuReasoner.setGpuMode(GpuMode.CPU_ONLY);

            Rule rule = new Rule("derived(X,Y) <- 1 base(X,Y)", "rule");
            cpuReasoner.addRule(rule);

            for (int i = 0; i < size; i++) {
                cpuReasoner.addFact(new TimedFact(
                    Atom.parse("base(n" + i + ",m" + i + ")"),
                    "f" + i, 0, 3));
            }

            long cpuStart = System.nanoTime();
            cpuReasoner.reason(3);
            long cpuTime = System.nanoTime() - cpuStart;

            // Benchmark GPU
            OptimizedReasoner gpuReasoner = new OptimizedReasoner();
            gpuReasoner.setGpuMode(GpuMode.GPU_ONLY);
            gpuReasoner.addRule(rule);

            for (int i = 0; i < size; i++) {
                gpuReasoner.addFact(new TimedFact(
                    Atom.parse("base(n" + i + ",m" + i + ")"),
                    "f" + i, 0, 3));
            }

            long gpuStart = System.nanoTime();
            gpuReasoner.reason(3);
            long gpuTime = System.nanoTime() - gpuStart;

            double cpuMs = cpuTime / 1_000_000.0;
            double gpuMs = gpuTime / 1_000_000.0;
            double speedup = gpuTime > 0 ? cpuTime / (double) gpuTime : 0;

            String speedupStr;
            if (speedup > 1) {
                speedupStr = String.format("%.1fx faster", speedup);
            } else {
                speedupStr = String.format("%.1fx slower", 1.0 / speedup);
            }

            System.out.printf("%-15s %-15s %-15s %-15s%n",
                String.format("%,d", size),
                String.format("%.2f ms", cpuMs),
                String.format("%.2f ms", gpuMs),
                speedupStr);

            cpuReasoner.cleanup();
            gpuReasoner.cleanup();
        }

        System.out.println();
        System.out.println("Observation: GPU excels at large datasets (5K+ facts)");
    }

    /**
     * Example 3: AUTO Mode for Automatic Optimization
     */
    private static void example3AutoMode() {
        System.out.println("Example 3: AUTO Mode for Automatic Optimization");
        System.out.println("-".repeat(40));

        OptimizedReasoner reasoner = new OptimizedReasoner();
        reasoner.setGpuMode(GpuMode.AUTO);  // Automatically chooses CPU or GPU

        Rule rule = new Rule("result(X) <- 1 input(X)", "rule");
        reasoner.addRule(rule);

        // Test 1: Small dataset
        System.out.println("Test 1: Small dataset (100 facts)");
        for (int i = 0; i < 100; i++) {
            reasoner.addFact(new TimedFact(
                Atom.parse("input(n" + i + ")"), "f" + i, 0, 5));
        }

        boolean willUseGpuSmall = reasoner.willUseGpu(5);
        System.out.println("  Decision: " + (willUseGpuSmall ? "GPU" : "CPU"));
        System.out.println("  Reason: " + (willUseGpuSmall ?
            "Dataset large enough for GPU" :
            "Dataset too small - CPU overhead lower"));

        ReasoningInterpretation result1 = reasoner.reason(5);
        System.out.println("  Results: " + result1.getMaxTime() + " timesteps processed");
        System.out.println();

        // Test 2: Large dataset
        System.out.println("Test 2: Large dataset (10,000 facts)");
        OptimizedReasoner reasoner2 = new OptimizedReasoner();
        reasoner2.setGpuMode(GpuMode.AUTO);
        reasoner2.addRule(rule);

        for (int i = 0; i < 10000; i++) {
            reasoner2.addFact(new TimedFact(
                Atom.parse("input(n" + i + ")"), "f" + i, 0, 5));
        }

        boolean willUseGpuLarge = reasoner2.willUseGpu(5);
        System.out.println("  Decision: " + (willUseGpuLarge ? "GPU" : "CPU"));
        System.out.println("  Reason: " + (willUseGpuLarge ?
            "Large dataset benefits from GPU parallelism" :
            "GPU not available"));

        long start = System.nanoTime();
        ReasoningInterpretation result2 = reasoner2.reason(5);
        long elapsed = System.nanoTime() - start;

        System.out.println("  Results: " + result2.getMaxTime() + " timesteps in " +
            String.format("%.2f", elapsed / 1_000_000.0) + " ms");

        reasoner.cleanup();
        reasoner2.cleanup();
    }

    /**
     * Example 4: Fallback Behavior for Unsupported Operations
     */
    private static void example4FallbackBehavior() {
        System.out.println("Example 4: Fallback Behavior");
        System.out.println("-".repeat(40));

        OptimizedReasoner reasoner = new OptimizedReasoner();
        reasoner.setGpuMode(GpuMode.GPU_ONLY);

        // Rule 1: Simple pattern (GPU supported)
        Rule rule1 = new Rule("derived(X) <- 1 base(X)", "gpu_rule");
        reasoner.addRule(rule1);

        // Rule 2: Multi-literal pattern (GPU not yet supported - auto fallback)
        Rule rule2 = new Rule("joined(X,Z) <- 1 a(X,Y), b(Y,Z)", "cpu_rule");
        reasoner.addRule(rule2);

        // Rule 3: Negation (GPU not yet supported - auto fallback)
        Rule rule3 = new Rule("allowed(X) <- 1 person(X), not blocked(X)", "neg_rule");
        reasoner.addRule(rule3);

        // Add facts
        for (int i = 0; i < 1000; i++) {
            reasoner.addFact(new TimedFact(
                Atom.parse("base(n" + i + ")"), "b" + i, 0, 5));
            reasoner.addFact(new TimedFact(
                Atom.parse("a(n" + i + ",m" + i + ")"), "a" + i, 0, 5));
            reasoner.addFact(new TimedFact(
                Atom.parse("b(m" + i + ",o" + i + ")"), "bb" + i, 0, 5));
            reasoner.addFact(new TimedFact(
                Atom.parse("person(n" + i + ")"), "p" + i, 0, 5));
        }

        System.out.println("Processing 3 rules with 4000 facts:");
        System.out.println("  Rule 1 (simple pattern):  GPU");
        System.out.println("  Rule 2 (multi-literal):   CPU (automatic fallback)");
        System.out.println("  Rule 3 (negation):        CPU (automatic fallback)");
        System.out.println();

        long start = System.nanoTime();
        ReasoningInterpretation result = reasoner.reason(5);
        long elapsed = System.nanoTime() - start;

        System.out.println("Results:");
        System.out.println("  Total time: " + String.format("%.2f", elapsed / 1_000_000.0) + " ms");
        System.out.println("  Timesteps processed: " + result.getMaxTime());
        System.out.println();
        System.out.println("Note: System automatically chose optimal execution");
        System.out.println("      (GPU where supported, CPU fallback elsewhere)");

        reasoner.cleanup();
    }

    /**
     * Example 5: Real-World Use Case - Social Network Analysis
     */
    private static void example5RealWorldUseCase() {
        System.out.println("Example 5: Real-World Use Case - Social Network Analysis");
        System.out.println("-".repeat(40));

        OptimizedReasoner reasoner = new OptimizedReasoner();
        reasoner.setGpuMode(GpuMode.AUTO);

        // Rules for social network analysis
        Rule popularityRule = new Rule("popular(X) <- 1 likes(Y,X)", "popularity");
        Rule influencerRule = new Rule("influencer(X) <- 1 popular(X)", "influencer");

        reasoner.addRule(popularityRule);
        reasoner.addRule(influencerRule);

        // Simulate social network (5000 users, 20000 likes)
        System.out.println("Generating social network:");
        System.out.println("  Users: 5,000");
        System.out.println("  Likes: 20,000");
        System.out.println();

        // Generate random likes
        for (int i = 0; i < 20000; i++) {
            int from = (int) (Math.random() * 5000);
            int to = (int) (Math.random() * 5000);
            if (from != to) {
                reasoner.addFact(new TimedFact(
                    Atom.parse("likes(user" + from + ",user" + to + ")"),
                    "like" + i,
                    0, 10));
            }
        }

        System.out.println("Processing with GPU acceleration...");
        long start = System.nanoTime();
        ReasoningInterpretation result = reasoner.reason(10);
        long elapsed = System.nanoTime() - start;

        // Count popular users and influencers
        int popularCount = 0;
        int influencerCount = 0;

        for (int t = 0; t <= result.getMaxTime(); t++) {
            for (Atom atom : result.getFactsAt(t)) {
                if (atom.getPredicate().equals("popular")) {
                    popularCount++;
                } else if (atom.getPredicate().equals("influencer")) {
                    influencerCount++;
                }
            }
        }

        System.out.println();
        System.out.println("Analysis complete!");
        System.out.println("  Time: " + String.format("%.2f", elapsed / 1_000_000.0) + " ms");
        System.out.println("  Popular users: " + popularCount);
        System.out.println("  Influencers: " + influencerCount);
        System.out.println();

        // Show a few examples
        System.out.println("Sample results:");
        int count = 0;
        for (Atom atom : result.getFactsAt(result.getMaxTime())) {
            if (atom.getPredicate().equals("influencer") && count < 5) {
                System.out.println("  - " + atom);
                count++;
            }
        }

        reasoner.cleanup();
    }
}
