package com.example;

import com.example.gpu.GpuFactStore;
import com.example.gpu.GpuPatternMatcher;
import com.example.gpu.GpuReasoningEngine;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Example demonstrating GPU-accelerated pattern matching (Phase 3).
 */
public class ExampleGpuPatternMatching {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("JavaSense v1.4 - GPU Acceleration (Phase 3: Pattern Matching)");
        System.out.println("=".repeat(80));
        System.out.println();

        GpuReasoningEngine gpu = new GpuReasoningEngine();

        if (!gpu.isGpuAvailable()) {
            System.out.println("No GPU detected - this demo requires GPU acceleration");
            System.out.println("GPU Info: " + gpu.getGpuInfo());
            return;
        }

        System.out.println("GPU detected: " + gpu.getGpuInfo());
        System.out.println();

        try {
            // Example 1: Basic pattern matching
            example1BasicPatternMatching(gpu);
            System.out.println();

            // Example 2: Pattern with constants
            example2PatternWithConstants(gpu);
            System.out.println();

            // Example 3: Performance comparison
            example3Performance(gpu);
            System.out.println();

        } finally {
            gpu.cleanup();
        }

        System.out.println("=".repeat(80));
    }

    private static void example1BasicPatternMatching(GpuReasoningEngine gpu) {
        System.out.println("Example 1: Basic Pattern Matching");
        System.out.println("-".repeat(40));

        // Create facts
        List<Atom> facts = Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("likes(bob,charlie)"),
            Atom.parse("likes(charlie,alice)"),
            Atom.parse("popular(bob)")
        );

        System.out.println("Facts:");
        for (int i = 0; i < facts.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, facts.get(i));
        }
        System.out.println();

        // Upload to GPU
        GpuFactStore store = new GpuFactStore(gpu);
        try {
            store.uploadFacts(facts);
            System.out.println("Uploaded to GPU: " + store.getFactCount() + " facts");
            System.out.println();

            // Pattern match
            GpuPatternMatcher matcher = new GpuPatternMatcher(gpu, store);
            try {
                System.out.println("Pattern: likes(X,Y)");

                long startTime = System.nanoTime();
                List<Map<String, String>> subs = matcher.findSubstitutions(
                    Arrays.asList(Literal.parse("likes(X,Y)"))
                );
                long elapsed = System.nanoTime() - startTime;

                System.out.println("Matches: " + subs.size());
                System.out.println("Time: " + String.format("%.2f", elapsed / 1_000_000.0) + " ms");
                System.out.println();

                System.out.println("Substitutions:");
                for (Map<String, String> sub : subs) {
                    System.out.println("  X=" + sub.get("X") + ", Y=" + sub.get("Y"));
                }

            } finally {
                matcher.cleanup();
            }
        } finally {
            store.cleanup();
        }
    }

    private static void example2PatternWithConstants(GpuReasoningEngine gpu) {
        System.out.println("Example 2: Pattern with Constants");
        System.out.println("-".repeat(40));

        List<Atom> facts = Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("likes(alice,charlie)"),
            Atom.parse("likes(bob,alice)"),
            Atom.parse("likes(bob,charlie)")
        );

        GpuFactStore store = new GpuFactStore(gpu);
        try {
            store.uploadFacts(facts);

            GpuPatternMatcher matcher = new GpuPatternMatcher(gpu, store);
            try {
                // Pattern: likes(alice,X) - find who alice likes
                System.out.println("Pattern: likes(alice,X)");
                List<Map<String, String>> subs = matcher.findSubstitutions(
                    Arrays.asList(Literal.parse("likes(alice,X)"))
                );

                System.out.println("Alice likes:");
                for (Map<String, String> sub : subs) {
                    System.out.println("  - " + sub.get("X"));
                }
                System.out.println();

                // Pattern: likes(X,bob) - find who likes bob
                System.out.println("Pattern: likes(X,bob)");
                subs = matcher.findSubstitutions(
                    Arrays.asList(Literal.parse("likes(X,bob)"))
                );

                System.out.println("People who like bob:");
                for (Map<String, String> sub : subs) {
                    System.out.println("  - " + sub.get("X"));
                }

            } finally {
                matcher.cleanup();
            }
        } finally {
            store.cleanup();
        }
    }

    private static void example3Performance(GpuReasoningEngine gpu) {
        System.out.println("Example 3: Performance at Different Scales");
        System.out.println("-".repeat(40));

        int[] sizes = {100, 1000, 10000};

        System.out.printf("%-15s %-15s %-15s%n", "Facts", "Time (ms)", "Throughput");
        System.out.println("-".repeat(45));

        for (int size : sizes) {
            // Generate facts
            List<Atom> facts = new java.util.ArrayList<>();
            for (int i = 0; i < size; i++) {
                facts.add(Atom.parse("fact" + i + "(a" + i + ",b" + i + ")"));
            }

            GpuFactStore store = new GpuFactStore(gpu);
            try {
                store.uploadFacts(facts);

                GpuPatternMatcher matcher = new GpuPatternMatcher(gpu, store);
                try {
                    // Warmup
                    for (int i = 0; i < 3; i++) {
                        matcher.findSubstitutions(Arrays.asList(Literal.parse("fact0(X,Y)")));
                    }

                    // Benchmark
                    long startTime = System.nanoTime();
                    matcher.findSubstitutions(Arrays.asList(Literal.parse("fact0(X,Y)")));
                    long elapsed = System.nanoTime() - startTime;

                    double timeMs = elapsed / 1_000_000.0;
                    double throughput = size / (elapsed / 1_000_000_000.0);

                    System.out.printf("%-15s %-15s %-15s%n",
                        String.format("%,d", size),
                        String.format("%.2f", timeMs),
                        String.format("%,.0f facts/s", throughput)
                    );

                } finally {
                    matcher.cleanup();
                }
            } finally {
                store.cleanup();
            }
        }

        System.out.println();
        System.out.println("Note: GPU shines at larger datasets (10K+ facts)");
    }
}
