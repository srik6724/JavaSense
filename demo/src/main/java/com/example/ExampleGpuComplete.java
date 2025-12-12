package com.example;

import com.example.gpu.GpuMode;
import com.example.gpu.GpuReasoningEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Complete GPU acceleration example demonstrating all features of JavaSense v1.4.
 *
 * <p>This example showcases:</p>
 * <ul>
 *   <li>GPU detection and configuration</li>
 *   <li>AUTO mode for automatic GPU/CPU selection</li>
 *   <li>Phase 5 optimizations (caching, work-group tuning)</li>
 *   <li>Performance comparison and reporting</li>
 *   <li>Real-world scenario: fraud detection system</li>
 * </ul>
 */
public class ExampleGpuComplete {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("JavaSense v1.4 - Complete GPU Acceleration Demonstration");
        System.out.println("=".repeat(80));
        System.out.println();

        // Check GPU availability
        GpuReasoningEngine gpu = new GpuReasoningEngine();
        System.out.println("GPU Detection:");
        if (gpu.isGpuAvailable()) {
            System.out.println("  ✓ GPU detected: " + gpu.getGpuInfo());
        } else {
            System.out.println("  ✗ No GPU detected: " + gpu.getGpuInfo());
            System.out.println("  Running in CPU-only mode for demonstration");
        }
        gpu.cleanup();
        System.out.println();

        // Run comprehensive demonstration
        demonstrateFraudDetection();

        System.out.println("=".repeat(80));
        System.out.println("Demo complete!");
        System.out.println("=".repeat(80));
    }

    /**
     * Real-world scenario: Financial fraud detection system
     */
    private static void demonstrateFraudDetection() {
        System.out.println("Real-World Scenario: Fraud Detection System");
        System.out.println("-".repeat(40));
        System.out.println();

        System.out.println("System Requirements:");
        System.out.println("  - Process 100K transactions per day");
        System.out.println("  - Detect suspicious patterns in real-time");
        System.out.println("  - Respond within 100ms per transaction batch");
        System.out.println();

        // Define fraud detection rules
        System.out.println("Fraud Detection Rules:");
        System.out.println("  1. Large transaction -> flag_review(T)");
        System.out.println("  2. Multiple transactions -> flag_velocity(T)");
        System.out.println("  3. Unusual time -> flag_anomaly(T)");
        System.out.println();

        OptimizedReasoner reasoner = new OptimizedReasoner();
        reasoner.setGpuMode(GpuMode.AUTO);  // Let system decide GPU vs CPU

        // Add fraud detection rules
        Rule largeTransactionRule = new Rule(
            "flag_review(T) <- 1 transaction(T,Amt), large_amount(Amt)",
            "large_transaction");
        Rule velocityRule = new Rule(
            "flag_velocity(User) <- 1 transaction(T,User)",
            "velocity_check");
        Rule anomalyRule = new Rule(
            "flag_anomaly(T) <- 1 transaction(T,Time), unusual_time(Time)",
            "anomaly_detection");

        reasoner.addRule(largeTransactionRule);
        reasoner.addRule(velocityRule);
        reasoner.addRule(anomalyRule);

        // Simulate transaction data
        System.out.println("Generating test data:");
        int transactionCount = 50000;
        System.out.println("  Transactions: " + String.format("%,d", transactionCount));

        List<TimedFact> transactions = generateTransactions(transactionCount);
        for (TimedFact tf : transactions) {
            reasoner.addFact(tf);
        }

        // Check if GPU will be used
        boolean usingGpu = reasoner.willUseGpu(10);
        System.out.println("  Processing mode: " + (usingGpu ? "GPU" : "CPU"));
        System.out.println();

        // Process transactions
        System.out.println("Processing fraud detection...");
        long startTime = System.nanoTime();
        ReasoningInterpretation result = reasoner.reason(10);
        long elapsedTime = System.nanoTime() - startTime;

        // Analyze results
        System.out.println();
        System.out.println("Results:");
        System.out.println("  Processing time: " +
            String.format("%.2f", elapsedTime / 1_000_000.0) + " ms");
        System.out.println("  Performance: " +
            String.format("%,.0f", transactionCount / (elapsedTime / 1_000_000_000.0)) +
            " transactions/sec");

        // Count flagged transactions
        int flaggedCount = 0;
        for (int t = 0; t <= result.getMaxTime(); t++) {
            for (Atom atom : result.getFactsAt(t)) {
                if (atom.getPredicate().startsWith("flag_")) {
                    flaggedCount++;
                }
            }
        }

        System.out.println("  Flagged transactions: " + String.format("%,d", flaggedCount));
        System.out.println("  Flag rate: " +
            String.format("%.2f%%", (flaggedCount * 100.0) / transactionCount));

        // SLA check
        boolean meetsSLA = (elapsedTime / 1_000_000.0) < 100.0;
        System.out.println("  SLA (< 100ms): " +
            (meetsSLA ? "✓ PASS" : "✗ FAIL"));

        if (meetsSLA && usingGpu) {
            System.out.println();
            System.out.println("✓ GPU acceleration enables real-time fraud detection!");
        }

        // Performance comparison
        System.out.println();
        System.out.println("Performance Comparison:");
        System.out.println("-".repeat(40));

        // Run CPU-only for comparison
        OptimizedReasoner cpuReasoner = new OptimizedReasoner();
        cpuReasoner.setGpuMode(GpuMode.CPU_ONLY);

        cpuReasoner.addRule(largeTransactionRule);
        cpuReasoner.addRule(velocityRule);
        cpuReasoner.addRule(anomalyRule);

        for (TimedFact tf : transactions) {
            cpuReasoner.addFact(tf);
        }

        long cpuStartTime = System.nanoTime();
        cpuReasoner.reason(10);
        long cpuElapsedTime = System.nanoTime() - cpuStartTime;

        System.out.println("CPU-only time: " +
            String.format("%.2f", cpuElapsedTime / 1_000_000.0) + " ms");
        System.out.println("GPU/AUTO time: " +
            String.format("%.2f", elapsedTime / 1_000_000.0) + " ms");

        if (usingGpu && cpuElapsedTime > elapsedTime) {
            double speedup = cpuElapsedTime / (double) elapsedTime;
            System.out.println("Speedup: " + String.format("%.1fx faster with GPU", speedup));
        }

        // Demonstrate Phase 5 caching
        System.out.println();
        System.out.println("Phase 5 Caching Demonstration:");
        System.out.println("-".repeat(40));

        // Process same data again - should hit cache
        long cachedStartTime = System.nanoTime();
        reasoner.reason(10);
        long cachedElapsedTime = System.nanoTime() - cachedStartTime;

        System.out.println("First run:  " +
            String.format("%.2f", elapsedTime / 1_000_000.0) + " ms");
        System.out.println("Second run: " +
            String.format("%.2f", cachedElapsedTime / 1_000_000.0) + " ms (cached)");

        if (cachedElapsedTime < elapsedTime) {
            double cacheSpeedup = elapsedTime / (double) cachedElapsedTime;
            System.out.println("Cache benefit: " +
                String.format("%.1fx faster", cacheSpeedup));
        }

        // Cleanup
        reasoner.cleanup();
        cpuReasoner.cleanup();

        System.out.println();
        System.out.println("Summary:");
        System.out.println("  ✓ Processed " + String.format("%,d", transactionCount) +
            " transactions");
        System.out.println("  ✓ Detected " + String.format("%,d", flaggedCount) +
            " suspicious patterns");
        System.out.println("  ✓ Met SLA requirements: " + (meetsSLA ? "Yes" : "No"));
        System.out.println("  ✓ GPU acceleration: " + (usingGpu ? "Enabled" : "Not beneficial"));
    }

    /**
     * Generates synthetic transaction data for demonstration.
     */
    private static List<TimedFact> generateTransactions(int count) {
        List<TimedFact> transactions = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int timestep = i % 10;  // Distribute across 10 timesteps
            String userId = "user" + (i % 1000);  // 1000 unique users
            int amount = 100 + (i % 9900);  // Amounts from 100 to 10000

            // Transaction fact
            transactions.add(new TimedFact(
                Atom.parse("transaction(tx" + i + "," + userId + ")"),
                "trans" + i,
                timestep, timestep));

            // Large amount flag (top 10%)
            if (amount > 9000) {
                transactions.add(new TimedFact(
                    Atom.parse("large_amount(" + amount + ")"),
                    "large" + i,
                    timestep, timestep));
            }

            // Unusual time flag (late night - 5% of transactions)
            if (i % 20 == 0) {
                transactions.add(new TimedFact(
                    Atom.parse("unusual_time(" + timestep + ")"),
                    "unusual" + i,
                    timestep, timestep));
            }
        }

        return transactions;
    }
}
