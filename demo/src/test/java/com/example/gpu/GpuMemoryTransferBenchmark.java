package com.example.gpu;

import com.example.Atom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Benchmarks for GPU memory transfer overhead.
 *
 * <p>This measures the cost of transferring facts between CPU and GPU memory.</p>
 */
class GpuMemoryTransferBenchmark {

    private GpuReasoningEngine gpu;
    private GpuFactStore store;

    @BeforeEach
    void setUp() {
        gpu = new GpuReasoningEngine();

        if (!gpu.isGpuAvailable()) {
            System.out.println("Skipping GPU benchmarks (no GPU available)");
            store = null;
            return;
        }

        store = new GpuFactStore(gpu);
    }

    @AfterEach
    void tearDown() {
        if (store != null) {
            store.cleanup();
        }
        if (gpu != null) {
            gpu.cleanup();
        }
    }

    @Test
    void benchmarkSmallTransfer() {
        if (store == null) return;

        List<Atom> facts = generateFacts(10);

        long startTime = System.nanoTime();
        store.uploadFacts(facts);
        long elapsed = System.nanoTime() - startTime;

        printResults("Small (10 facts)", facts.size(), elapsed, store.estimateGpuMemory());
    }

    @Test
    void benchmarkMediumTransfer() {
        if (store == null) return;

        List<Atom> facts = generateFacts(1000);

        long startTime = System.nanoTime();
        store.uploadFacts(facts);
        long elapsed = System.nanoTime() - startTime;

        printResults("Medium (1K facts)", facts.size(), elapsed, store.estimateGpuMemory());
    }

    @Test
    void benchmarkLargeTransfer() {
        if (store == null) return;

        List<Atom> facts = generateFacts(10000);

        long startTime = System.nanoTime();
        store.uploadFacts(facts);
        long elapsed = System.nanoTime() - startTime;

        printResults("Large (10K facts)", facts.size(), elapsed, store.estimateGpuMemory());
    }

    @Test
    void benchmarkVeryLargeTransfer() {
        if (store == null) return;

        List<Atom> facts = generateFacts(100000);

        long startTime = System.nanoTime();
        store.uploadFacts(facts);
        long elapsed = System.nanoTime() - startTime;

        printResults("Very Large (100K facts)", facts.size(), elapsed, store.estimateGpuMemory());
    }

    @Test
    void benchmarkRepeatedTransfers() {
        if (store == null) return;

        List<Atom> facts = generateFacts(1000);
        int iterations = 10;

        long totalTime = 0;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            store.uploadFacts(facts);
            long elapsed = System.nanoTime() - startTime;
            totalTime += elapsed;
        }

        double avgTime = totalTime / (double) iterations;

        System.out.println("\nRepeated Transfers (1K facts × " + iterations + " iterations):");
        System.out.println("  Average time: " + String.format("%.2f", avgTime / 1_000_000.0) + " ms");
        System.out.println("  Total time:   " + String.format("%.2f", totalTime / 1_000_000.0) + " ms");
    }

    @Test
    void benchmarkEncodingOverhead() {
        if (store == null) return;

        List<Atom> facts = generateFacts(10000);

        // Measure encoding only
        FactEncoder encoder = new FactEncoder();
        long encodeStart = System.nanoTime();
        for (Atom fact : facts) {
            encoder.encode(fact);
        }
        long encodeTime = System.nanoTime() - encodeStart;

        // Measure full upload (encoding + transfer)
        long uploadStart = System.nanoTime();
        store.uploadFacts(facts);
        long uploadTime = System.nanoTime() - uploadStart;

        double transferTime = uploadTime - encodeTime;

        System.out.println("\nEncoding vs Transfer Overhead (10K facts):");
        System.out.println("  Encoding: " + String.format("%.2f", encodeTime / 1_000_000.0) + " ms");
        System.out.println("  Transfer: " + String.format("%.2f", transferTime / 1_000_000.0) + " ms");
        System.out.println("  Total:    " + String.format("%.2f", uploadTime / 1_000_000.0) + " ms");
        System.out.println("  Encoding %: " + String.format("%.1f", 100.0 * encodeTime / uploadTime) + "%");
        System.out.println("  Transfer %: " + String.format("%.1f", 100.0 * transferTime / uploadTime) + "%");
    }

    @Test
    void benchmarkThroughput() {
        if (store == null) return;

        int[] sizes = {100, 1000, 10000, 100000};

        System.out.println("\nGPU Transfer Throughput:");
        System.out.println("-".repeat(80));
        System.out.printf("%-15s %-15s %-15s %-20s%n",
            "Facts", "Time (ms)", "Memory (KB)", "Throughput (facts/s)");
        System.out.println("-".repeat(80));

        for (int size : sizes) {
            List<Atom> facts = generateFacts(size);

            long startTime = System.nanoTime();
            store.uploadFacts(facts);
            long elapsed = System.nanoTime() - startTime;

            double timeMs = elapsed / 1_000_000.0;
            double throughput = size / (elapsed / 1_000_000_000.0);
            long memoryKb = store.estimateGpuMemory() / 1024;

            System.out.printf("%-15s %-15s %-15s %-20s%n",
                String.format("%,d", size),
                String.format("%.2f", timeMs),
                String.format("%,d", memoryKb),
                String.format("%,.0f", throughput));
        }
    }

    @Test
    void benchmarkMemoryEfficiency() {
        if (store == null) return;

        List<Atom> facts = generateFacts(10000);
        store.uploadFacts(facts);

        FactEncoder encoder = store.getEncoder();
        long encoderMemory = encoder.estimateMemoryUsage();
        long storeMemory = store.estimateGpuMemory();

        System.out.println("\nMemory Efficiency (10K facts):");
        System.out.println("  Encoder memory (CPU): " + String.format("%,d", encoderMemory) + " bytes");
        System.out.println("  Store memory (GPU):   " + String.format("%,d", storeMemory) + " bytes");
        System.out.println("  Total:                " + String.format("%,d", encoderMemory + storeMemory) + " bytes");
        System.out.println("  Per fact:             " + String.format("%.1f", (encoderMemory + storeMemory) / 10000.0) + " bytes/fact");
    }

    /**
     * Generates test facts with varying complexity.
     */
    private List<Atom> generateFacts(int count) {
        List<Atom> facts = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            // Generate facts of different arities
            if (i % 3 == 0) {
                facts.add(Atom.parse("fact" + i + "(arg" + i + ")"));
            } else if (i % 3 == 1) {
                facts.add(Atom.parse("fact" + i + "(arg" + i + ",arg" + (i+1) + ")"));
            } else {
                facts.add(Atom.parse("fact" + i + "(arg" + i + ",arg" + (i+1) + ",arg" + (i+2) + ")"));
            }
        }

        return facts;
    }

    /**
     * Prints benchmark results in a formatted way.
     */
    private void printResults(String name, int factCount, long nanoTime, long memoryBytes) {
        double timeMs = nanoTime / 1_000_000.0;
        double memoryKb = memoryBytes / 1024.0;
        double throughput = factCount / (nanoTime / 1_000_000_000.0);

        System.out.println("\n" + name + ":");
        System.out.println("  Facts:      " + String.format("%,d", factCount));
        System.out.println("  Time:       " + String.format("%.2f", timeMs) + " ms");
        System.out.println("  Memory:     " + String.format("%.1f", memoryKb) + " KB");
        System.out.println("  Throughput: " + String.format("%,.0f", throughput) + " facts/s");
    }
}
