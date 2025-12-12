package com.example.gpu;

import com.example.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive benchmarks for GPU acceleration across different problem sizes.
 *
 * <p>Phase 6: Final testing and benchmarking to validate GPU performance
 * across small, medium, large, and very large datasets.</p>
 */
class GpuComprehensiveBenchmark {

    private GpuReasoningEngine gpu;

    @BeforeEach
    void setUp() {
        gpu = new GpuReasoningEngine();
    }

    @AfterEach
    void tearDown() {
        if (gpu != null) {
            gpu.cleanup();
        }
    }

    @Test
    void benchmarkSmallDataset() {
        if (!gpu.isGpuAvailable()) {
            System.out.println("Skipping GPU benchmarks (no GPU available)");
            return;
        }

        runBenchmark("Small Dataset", 100, 10, 10);
    }

    @Test
    void benchmarkMediumDataset() {
        if (!gpu.isGpuAvailable()) return;
        runBenchmark("Medium Dataset", 10_000, 50, 50);
    }

    @Test
    void benchmarkLargeDataset() {
        if (!gpu.isGpuAvailable()) return;
        runBenchmark("Large Dataset", 100_000, 100, 100);
    }

    @Test
    void benchmarkVeryLargeDataset() {
        if (!gpu.isGpuAvailable()) return;
        runBenchmark("Very Large Dataset", 500_000, 200, 200);
    }

    @Test
    void benchmarkScalability() {
        if (!gpu.isGpuAvailable()) return;

        System.out.println("\n" + "=".repeat(80));
        System.out.println("GPU Scalability Benchmark");
        System.out.println("=".repeat(80));

        int[] factCounts = {100, 500, 1_000, 5_000, 10_000, 50_000, 100_000};

        System.out.printf("\n%-15s %-15s %-15s %-15s %-15s%n",
            "Facts", "CPU Time", "GPU Time", "Speedup", "Throughput");
        System.out.println("-".repeat(75));

        for (int factCount : factCounts) {
            BenchmarkResult result = runSingleBenchmark(factCount, 10, 10);

            double speedup = result.cpuTime / (double) result.gpuTime;
            double throughput = factCount / (result.gpuTime / 1_000_000_000.0);

            System.out.printf("%-15s %-15s %-15s %-15s %-15s%n",
                String.format("%,d", factCount),
                String.format("%.2f ms", result.cpuTime / 1_000_000.0),
                String.format("%.2f ms", result.gpuTime / 1_000_000.0),
                String.format("%.1fx", speedup),
                String.format("%,.0f/s", throughput));
        }

        System.out.println("=".repeat(80));
    }

    @Test
    void benchmarkTemporalScalability() {
        if (!gpu.isGpuAvailable()) return;

        System.out.println("\n" + "=".repeat(80));
        System.out.println("Temporal Reasoning Scalability");
        System.out.println("=".repeat(80));

        int[] timestepCounts = {10, 50, 100, 200, 500};
        int factCount = 10_000;

        System.out.printf("\n%-15s %-15s %-15s %-15s%n",
            "Timesteps", "CPU Time", "GPU Time", "Speedup");
        System.out.println("-".repeat(60));

        for (int timesteps : timestepCounts) {
            BenchmarkResult result = runSingleBenchmark(factCount, 10, timesteps);

            double speedup = result.cpuTime / (double) result.gpuTime;

            System.out.printf("%-15s %-15s %-15s %-15s%n",
                String.format("%,d", timesteps),
                String.format("%.2f ms", result.cpuTime / 1_000_000.0),
                String.format("%.2f ms", result.gpuTime / 1_000_000.0),
                String.format("%.1fx", speedup));
        }

        System.out.println("=".repeat(80));
    }

    @Test
    void benchmarkRuleComplexity() {
        if (!gpu.isGpuAvailable()) return;

        System.out.println("\n" + "=".repeat(80));
        System.out.println("Rule Complexity Benchmark");
        System.out.println("=".repeat(80));

        int[] ruleCounts = {1, 5, 10, 25, 50, 100};
        int factCount = 10_000;

        System.out.printf("\n%-15s %-15s %-15s %-15s%n",
            "Rules", "CPU Time", "GPU Time", "Speedup");
        System.out.println("-".repeat(60));

        for (int ruleCount : ruleCounts) {
            BenchmarkResult result = runSingleBenchmark(factCount, ruleCount, 10);

            double speedup = result.cpuTime / (double) result.gpuTime;

            System.out.printf("%-15s %-15s %-15s %-15s%n",
                String.format("%,d", ruleCount),
                String.format("%.2f ms", result.cpuTime / 1_000_000.0),
                String.format("%.2f ms", result.gpuTime / 1_000_000.0),
                String.format("%.1fx", speedup));
        }

        System.out.println("=".repeat(80));
    }

    @Test
    void benchmarkMemoryEfficiency() {
        if (!gpu.isGpuAvailable()) return;

        System.out.println("\n" + "=".repeat(80));
        System.out.println("GPU Memory Efficiency");
        System.out.println("=".repeat(80));

        int[] factCounts = {1_000, 10_000, 50_000, 100_000};

        System.out.printf("\n%-15s %-20s %-20s %-15s%n",
            "Facts", "GPU Memory", "Bytes/Fact", "Compression");
        System.out.println("-".repeat(70));

        for (int factCount : factCounts) {
            GpuFactStore store = new GpuFactStore(gpu);
            try {
                List<Atom> facts = generateFacts(factCount);

                // Calculate original size
                long originalSize = facts.stream()
                    .mapToLong(a -> a.toString().length() * 2)  // Rough estimate
                    .sum();

                store.uploadFacts(facts);
                long gpuMemory = store.estimateGpuMemory();
                double bytesPerFact = gpuMemory / (double) factCount;
                double compression = originalSize / (double) gpuMemory;

                System.out.printf("%-15s %-20s %-20s %-15s%n",
                    String.format("%,d", factCount),
                    String.format("%,.0f KB", gpuMemory / 1024.0),
                    String.format("%.1f", bytesPerFact),
                    String.format("%.1fx", compression));

            } finally {
                store.cleanup();
            }
        }

        System.out.println("=".repeat(80));
    }

    @Test
    void benchmarkCacheEffectiveness() {
        if (!gpu.isGpuAvailable()) return;

        System.out.println("\n" + "=".repeat(80));
        System.out.println("Phase 5 Cache Effectiveness");
        System.out.println("=".repeat(80));

        GpuFactStore store = new GpuFactStore(gpu);

        try {
            List<Atom> facts = generateFacts(10_000);

            // Measure upload times
            long[] uploadTimes = new long[5];

            for (int i = 0; i < 5; i++) {
                long start = System.nanoTime();
                store.uploadFacts(facts);
                uploadTimes[i] = System.nanoTime() - start;
            }

            System.out.println("\nUpload Times:");
            for (int i = 0; i < 5; i++) {
                System.out.printf("  Upload %d: %.2f ms %s%n",
                    i + 1,
                    uploadTimes[i] / 1_000_000.0,
                    i == 0 ? "(first - full transfer)" : "(cached)");
            }

            System.out.println("\nCache Statistics:");
            System.out.println("  Cache hits: " + store.getCacheHits());
            System.out.println("  Total uploads: " + store.getStats().uploadCount);
            System.out.println("  Hit rate: " +
                String.format("%.0f%%", (store.getCacheHits() * 100.0) / store.getStats().uploadCount));

            double avgCached = 0;
            for (int i = 1; i < 5; i++) {
                avgCached += uploadTimes[i];
            }
            avgCached /= 4.0;

            double speedup = uploadTimes[0] / avgCached;
            System.out.println("  Cache speedup: " + String.format("%.0fx", speedup));

        } finally {
            store.cleanup();
        }

        System.out.println("=".repeat(80));
    }

    private void runBenchmark(String name, int factCount, int ruleCount, int timesteps) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(name);
        System.out.println("=".repeat(80));
        System.out.println("Configuration:");
        System.out.println("  Facts: " + String.format("%,d", factCount));
        System.out.println("  Rules: " + ruleCount);
        System.out.println("  Timesteps: " + timesteps);

        BenchmarkResult result = runSingleBenchmark(factCount, ruleCount, timesteps);

        System.out.println("\nResults:");
        System.out.println("  CPU Time: " + String.format("%.2f", result.cpuTime / 1_000_000.0) + " ms");
        System.out.println("  GPU Time: " + String.format("%.2f", result.gpuTime / 1_000_000.0) + " ms");

        double speedup = result.cpuTime / (double) result.gpuTime;
        System.out.println("  Speedup:  " + String.format("%.1f", speedup) + "x");

        if (speedup > 1) {
            System.out.println("  Winner:   GPU ✓");
        } else {
            System.out.println("  Winner:   CPU (GPU overhead for small dataset)");
        }

        System.out.println("\nThroughput:");
        double cpuThroughput = factCount / (result.cpuTime / 1_000_000_000.0);
        double gpuThroughput = factCount / (result.gpuTime / 1_000_000_000.0);
        System.out.println("  CPU: " + String.format("%,.0f", cpuThroughput) + " facts/sec");
        System.out.println("  GPU: " + String.format("%,.0f", gpuThroughput) + " facts/sec");

        System.out.println("=".repeat(80));
    }

    private BenchmarkResult runSingleBenchmark(int factCount, int ruleCount, int timesteps) {
        // CPU benchmark
        OptimizedReasoner cpuReasoner = new OptimizedReasoner();
        cpuReasoner.setGpuMode(GpuMode.CPU_ONLY);

        for (int i = 0; i < ruleCount; i++) {
            Rule rule = new Rule("derived" + i + "(X,Y) <- 1 base(X,Y)", "rule" + i);
            cpuReasoner.addRule(rule);
        }

        List<Atom> facts = generateFacts(factCount);
        for (int i = 0; i < facts.size(); i++) {
            cpuReasoner.addFact(new TimedFact(facts.get(i), "f" + i, 0, timesteps));
        }

        long cpuStart = System.nanoTime();
        cpuReasoner.reason(timesteps);
        long cpuTime = System.nanoTime() - cpuStart;
        cpuReasoner.cleanup();

        // GPU benchmark
        OptimizedReasoner gpuReasoner = new OptimizedReasoner();
        gpuReasoner.setGpuMode(GpuMode.GPU_ONLY);

        for (int i = 0; i < ruleCount; i++) {
            Rule rule = new Rule("derived" + i + "(X,Y) <- 1 base(X,Y)", "rule" + i);
            gpuReasoner.addRule(rule);
        }

        for (int i = 0; i < facts.size(); i++) {
            gpuReasoner.addFact(new TimedFact(facts.get(i), "f" + i, 0, timesteps));
        }

        long gpuStart = System.nanoTime();
        gpuReasoner.reason(timesteps);
        long gpuTime = System.nanoTime() - gpuStart;
        gpuReasoner.cleanup();

        return new BenchmarkResult(cpuTime, gpuTime);
    }

    private List<Atom> generateFacts(int count) {
        List<Atom> facts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            facts.add(Atom.parse("base(n" + i + ",m" + i + ")"));
        }
        return facts;
    }

    private static class BenchmarkResult {
        final long cpuTime;
        final long gpuTime;

        BenchmarkResult(long cpuTime, long gpuTime) {
            this.cpuTime = cpuTime;
            this.gpuTime = gpuTime;
        }
    }
}
