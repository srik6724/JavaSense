package com.example;

import com.example.gpu.FactEncoder;
import com.example.gpu.GpuFactStore;
import com.example.gpu.GpuReasoningEngine;

import java.util.Arrays;
import java.util.List;

/**
 * Example demonstrating GPU data structures (Phase 2).
 *
 * <p>This example shows how facts are encoded and transferred to GPU memory.</p>
 */
public class ExampleGpuDataStructures {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("JavaSense v1.4 - GPU Acceleration (Phase 2: Data Structures)");
        System.out.println("=".repeat(80));
        System.out.println();

        // Example 1: Fact Encoding
        demonstrateEncoding();
        System.out.println();

        // Example 2: GPU Memory Transfer
        demonstrateGpuTransfer();
        System.out.println();

        // Example 3: Performance Analysis
        analyzePerformance();
        System.out.println();

        System.out.println("=".repeat(80));
    }

    private static void demonstrateEncoding() {
        System.out.println("Example 1: Fact Encoding");
        System.out.println("-".repeat(40));

        FactEncoder encoder = new FactEncoder();

        // Encode some facts
        List<Atom> facts = Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("popular(bob)"),
            Atom.parse("friend(alice,charlie)")
        );

        System.out.println("Original Facts:");
        for (int i = 0; i < facts.size(); i++) {
            Atom atom = facts.get(i);
            int[] encoded = encoder.encode(atom);

            System.out.printf("  %d. %-30s -> %s%n",
                i + 1,
                atom,
                Arrays.toString(encoded));
        }

        System.out.println();
        System.out.println("String Interning:");
        System.out.println("  Unique strings: " + encoder.getStringCount());

        encoder.getAllStrings().forEach((str, id) ->
            System.out.printf("    %-15s -> %d%n", "'" + str + "'", id)
        );

        System.out.println();
        System.out.println("Statistics:");
        System.out.println("  " + encoder.getStats());
        System.out.println("  Memory: " + encoder.estimateMemoryUsage() + " bytes");
    }

    private static void demonstrateGpuTransfer() {
        System.out.println("Example 2: GPU Memory Transfer");
        System.out.println("-".repeat(40));

        GpuReasoningEngine gpu = new GpuReasoningEngine();

        if (!gpu.isGpuAvailable()) {
            System.out.println("No GPU detected - skipping transfer demo");
            System.out.println("This is OK - Phase 2 also works without GPU");
            return;
        }

        System.out.println("GPU detected: " + gpu.getGpuInfo());
        System.out.println();

        GpuFactStore store = new GpuFactStore(gpu);

        // Upload facts
        List<Atom> facts = Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("likes(bob,charlie)"),
            Atom.parse("popular(bob)"),
            Atom.parse("friend(alice,charlie)"),
            Atom.parse("friend(bob,alice)")
        );

        System.out.println("Uploading " + facts.size() + " facts to GPU...");
        long startTime = System.nanoTime();
        store.uploadFacts(facts);
        long elapsed = System.nanoTime() - startTime;

        System.out.println("Upload complete in " + String.format("%.2f", elapsed / 1_000_000.0) + " ms");
        System.out.println();

        System.out.println("GPU Memory Layout:");
        System.out.println("  Facts stored:  " + store.getFactCount());
        System.out.println("  Integers used: " + store.getEncodedFacts().length);
        System.out.println("  GPU memory:    " + store.estimateGpuMemory() + " bytes");
        System.out.println();

        System.out.println("Encoded Facts:");
        int[] encoded = store.getEncodedFacts();
        int[] sizes = store.getFactSizes();
        System.out.println("  Data:  " + Arrays.toString(encoded));
        System.out.println("  Sizes: " + Arrays.toString(sizes));
        System.out.println();

        System.out.println("Verification (decode from GPU data):");
        for (int i = 0; i < store.getFactCount(); i++) {
            Atom fact = store.getFactAt(i);
            System.out.printf("  %d. %s%n", i + 1, fact);
        }

        store.cleanup();
        gpu.cleanup();
    }

    private static void analyzePerformance() {
        System.out.println("Example 3: Performance Analysis");
        System.out.println("-".repeat(40));

        GpuReasoningEngine gpu = new GpuReasoningEngine();

        if (!gpu.isGpuAvailable()) {
            System.out.println("No GPU detected - skipping performance analysis");
            return;
        }

        GpuFactStore store = new GpuFactStore(gpu);

        // Test different problem sizes
        int[] sizes = {100, 1000, 10000, 100000};

        System.out.printf("%-15s %-15s %-15s %-20s%n",
            "Facts", "Time (ms)", "Memory (KB)", "Throughput (facts/s)");
        System.out.println("-".repeat(65));

        for (int size : sizes) {
            // Generate facts
            List<Atom> facts = new java.util.ArrayList<>();
            for (int i = 0; i < size; i++) {
                facts.add(Atom.parse("fact" + i + "(arg" + i + ",arg" + (i+1) + ")"));
            }

            // Upload and measure
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

        System.out.println();
        System.out.println("Key Insights:");
        System.out.println("  • Transfer overhead is low for small problems");
        System.out.println("  • Throughput scales well with problem size");
        System.out.println("  • Memory usage is efficient (string interning)");
        System.out.println("  • Ready for Phase 3 (GPU pattern matching)");

        store.cleanup();
        gpu.cleanup();
    }
}
