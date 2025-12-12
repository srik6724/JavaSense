package com.example;

import com.example.gpu.GpuMode;
import com.example.gpu.GpuReasoningEngine;

import java.util.List;

/**
 * Example demonstrating GPU detection and configuration.
 *
 * <p>This example shows how to:</p>
 * <ul>
 *   <li>Detect available GPUs</li>
 *   <li>Configure GPU acceleration mode</li>
 *   <li>Set custom GPU thresholds</li>
 *   <li>Use GPU-accelerated reasoning (when Phase 3 is complete)</li>
 * </ul>
 */
public class ExampleGpuDetection {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("JavaSense v1.4 - GPU Acceleration (Phase 1: Foundation)");
        System.out.println("=".repeat(80));
        System.out.println();

        // Example 1: Detect GPU
        detectGpu();
        System.out.println();

        // Example 2: Configure GPU mode
        configureGpuMode();
        System.out.println();

        // Example 3: Test reasoning with different problem sizes
        testProblemSizes();
        System.out.println();

        System.out.println("=".repeat(80));
    }

    private static void detectGpu() {
        System.out.println("Example 1: GPU Detection");
        System.out.println("-".repeat(40));

        GpuReasoningEngine gpu = new GpuReasoningEngine();

        if (gpu.isGpuAvailable()) {
            System.out.println("GPU detected!");
            System.out.println("  Device: " + gpu.getGpuInfo());
            System.out.println("  Status: Ready for acceleration");
        } else {
            System.out.println("No GPU detected");
            System.out.println("  Reason: " + gpu.getGpuInfo());
            System.out.println("  Status: Will use CPU fallback");
        }

        gpu.cleanup();
    }

    private static void configureGpuMode() {
        System.out.println("Example 2: GPU Configuration");
        System.out.println("-".repeat(40));

        OptimizedReasoner reasoner = new OptimizedReasoner();

        // Mode 1: CPU Only (default)
        System.out.println("Mode 1: CPU_ONLY");
        reasoner.setGpuMode(GpuMode.CPU_ONLY);
        System.out.println("  Will use GPU: " + reasoner.willUseGpu(100000));

        // Mode 2: AUTO (recommended)
        System.out.println("\nMode 2: AUTO (recommended)");
        reasoner.setGpuMode(GpuMode.AUTO);
        System.out.println("  GPU Info: " + reasoner.getGpuInfo());
        System.out.println("  Will use GPU for small problem (100 facts): " +
            reasoner.willUseGpu(10));
        System.out.println("  Will use GPU for large problem (100K facts): " +
            reasoner.willUseGpu(10000));

        // Mode 3: GPU Only (fails if no GPU)
        System.out.println("\nMode 3: GPU_ONLY");
        try {
            reasoner.setGpuMode(GpuMode.GPU_ONLY);
            System.out.println("  GPU_ONLY mode enabled");
            System.out.println("  GPU Info: " + reasoner.getGpuInfo());
        } catch (IllegalStateException e) {
            System.out.println("  GPU_ONLY mode not available: " + e.getMessage());
        }

        reasoner.cleanup();
    }

    private static void testProblemSizes() {
        System.out.println("Example 3: GPU Decision Logic");
        System.out.println("-".repeat(40));

        OptimizedReasoner reasoner = new OptimizedReasoner();
        reasoner.setGpuMode(GpuMode.AUTO);

        // Test different problem sizes
        int[][] testCases = {
            {10, 5, 10},           // Tiny
            {100, 10, 10},         // Small
            {1000, 50, 100},       // Medium
            {10000, 100, 500},     // Large
            {100000, 500, 1000}    // Very large
        };

        System.out.println("Problem Size Analysis:");
        System.out.println();
        System.out.printf("%-10s %-10s %-10s %-15s %-10s%n",
            "Facts", "Rules", "Timesteps", "Complexity", "Use GPU?");
        System.out.println("-".repeat(60));

        for (int[] testCase : testCases) {
            int facts = testCase[0];
            int rules = testCase[1];
            int timesteps = testCase[2];
            long complexity = (long) facts * rules * timesteps;

            // Create dummy data for willUseGpu check
            for (int i = 0; i < facts; i++) {
                reasoner.addFact(new TimedFact(
                    Atom.parse("fact" + i + "(dummy)"),
                    "f" + i,
                    List.of(new Interval(0, timesteps))
                ));
            }
            for (int i = 0; i < rules; i++) {
                reasoner.addRule(new Rule(
                    "derived" + i + "(X) <-0 fact" + i + "(X)",
                    "r" + i
                ));
            }

            boolean useGpu = reasoner.willUseGpu(timesteps);

            System.out.printf("%-10d %-10d %-10d %-15s %-10s%n",
                facts, rules, timesteps, String.format("%,d", complexity),
                useGpu ? "YES" : "NO");

            // Reset for next test
            reasoner = new OptimizedReasoner();
            reasoner.setGpuMode(GpuMode.AUTO);
        }

        System.out.println();
        System.out.println("Note: GPU is only used when complexity is high enough");
        System.out.println("      to overcome data transfer overhead.");

        reasoner.cleanup();
    }
}
