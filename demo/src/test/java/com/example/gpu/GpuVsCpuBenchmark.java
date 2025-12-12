package com.example.gpu;

import com.example.Atom;
import com.example.Literal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * Benchmarks comparing GPU vs CPU pattern matching performance.
 */
class GpuVsCpuBenchmark {

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

        List<Atom> facts = generateFacts(100);
        runBenchmark("Small (100 facts)", facts);
    }

    @Test
    void benchmarkMediumDataset() {
        if (!gpu.isGpuAvailable()) return;

        List<Atom> facts = generateFacts(1000);
        runBenchmark("Medium (1K facts)", facts);
    }

    @Test
    void benchmarkLargeDataset() {
        if (!gpu.isGpuAvailable()) return;

        List<Atom> facts = generateFacts(10000);
        runBenchmark("Large (10K facts)", facts);
    }

    @Test
    void benchmarkVeryLargeDataset() {
        if (!gpu.isGpuAvailable()) return;

        List<Atom> facts = generateFacts(100000);
        runBenchmark("Very Large (100K facts)", facts);
    }

    private void runBenchmark(String name, List<Atom> facts) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Benchmark: " + name);
        System.out.println("=".repeat(80));

        // Pattern to match
        List<Literal> pattern = Arrays.asList(Literal.parse("likes(X,Y)"));

        // CPU benchmark
        long cpuTime = benchmarkCpu(facts, pattern);

        // GPU benchmark
        long gpuTime = benchmarkGpu(facts, pattern);

        // Results
        double cpuMs = cpuTime / 1_000_000.0;
        double gpuMs = gpuTime / 1_000_000.0;
        double speedup = (double) cpuTime / gpuTime;

        System.out.println("\nResults:");
        System.out.println("  CPU time:  " + String.format("%.2f", cpuMs) + " ms");
        System.out.println("  GPU time:  " + String.format("%.2f", gpuMs) + " ms");
        System.out.println("  Speedup:   " + String.format("%.1f", speedup) + "x");

        if (speedup > 1) {
            System.out.println("  Winner:    GPU ✓");
        } else {
            System.out.println("  Winner:    CPU (GPU overhead too high for this size)");
        }
    }

    private long benchmarkCpu(List<Atom> facts, List<Literal> pattern) {
        // Warmup
        for (int i = 0; i < 3; i++) {
            cpuPatternMatch(facts, pattern);
        }

        // Benchmark
        long startTime = System.nanoTime();
        List<Map<String, String>> result = cpuPatternMatch(facts, pattern);
        long elapsed = System.nanoTime() - startTime;

        System.out.println("\nCPU Pattern Matching:");
        System.out.println("  Matches found: " + result.size());
        System.out.println("  Time:          " + String.format("%.2f", elapsed / 1_000_000.0) + " ms");

        return elapsed;
    }

    private long benchmarkGpu(List<Atom> facts, List<Literal> pattern) {
        GpuFactStore store = new GpuFactStore(gpu);
        try {
            // Upload facts (include in timing for fair comparison)
            long startTime = System.nanoTime();

            store.uploadFacts(facts);

            GpuPatternMatcher matcher = new GpuPatternMatcher(gpu, store);
            try {
                // Warmup
                for (int i = 0; i < 3; i++) {
                    matcher.findSubstitutions(pattern);
                }

                // Benchmark (excluding upload which is already done)
                long matchStart = System.nanoTime();
                List<Map<String, String>> result = matcher.findSubstitutions(pattern);
                long matchTime = System.nanoTime() - matchStart;

                long totalElapsed = System.nanoTime() - startTime;

                System.out.println("\nGPU Pattern Matching:");
                System.out.println("  Matches found: " + result.size());
                System.out.println("  Upload time:   " + String.format("%.2f", (totalElapsed - matchTime) / 1_000_000.0) + " ms");
                System.out.println("  Match time:    " + String.format("%.2f", matchTime / 1_000_000.0) + " ms");
                System.out.println("  Total time:    " + String.format("%.2f", totalElapsed / 1_000_000.0) + " ms");

                return totalElapsed;

            } finally {
                matcher.cleanup();
            }
        } finally {
            store.cleanup();
        }
    }

    /**
     * Simple CPU pattern matching (for comparison).
     */
    private List<Map<String, String>> cpuPatternMatch(List<Atom> facts, List<Literal> pattern) {
        List<Map<String, String>> results = new ArrayList<>();

        if (pattern.size() != 1 || pattern.get(0).isNegated()) {
            return results;
        }

        Atom patternAtom = pattern.get(0).getAtom();
        String patternPred = patternAtom.getPredicate();
        List<String> patternArgs = patternAtom.getArgs();

        for (Atom fact : facts) {
            // Check predicate
            if (!fact.getPredicate().equals(patternPred)) {
                continue;
            }

            // Check argument count
            if (fact.getArgs().size() != patternArgs.size()) {
                continue;
            }

            // Try to unify
            Map<String, String> substitution = new HashMap<>();
            boolean matches = true;

            for (int i = 0; i < patternArgs.size(); i++) {
                String patternArg = patternArgs.get(i);
                String factArg = fact.getArgs().get(i);

                if (isVariable(patternArg)) {
                    // Variable: bind or check consistency
                    if (substitution.containsKey(patternArg)) {
                        if (!substitution.get(patternArg).equals(factArg)) {
                            matches = false;
                            break;
                        }
                    } else {
                        substitution.put(patternArg, factArg);
                    }
                } else {
                    // Constant: must match
                    if (!patternArg.equals(factArg)) {
                        matches = false;
                        break;
                    }
                }
            }

            if (matches) {
                results.add(substitution);
            }
        }

        return results;
    }

    private boolean isVariable(String str) {
        return str != null && !str.isEmpty() && Character.isUpperCase(str.charAt(0));
    }

    /**
     * Generates test facts.
     */
    private List<Atom> generateFacts(int count) {
        List<Atom> facts = new ArrayList<>();

        // Generate likes(X, Y) facts
        for (int i = 0; i < count / 2; i++) {
            facts.add(Atom.parse("likes(person" + i + ",person" + (i + 1) + ")"));
        }

        // Generate other facts (won't match)
        for (int i = 0; i < count / 2; i++) {
            facts.add(Atom.parse("other(thing" + i + ")"));
        }

        return facts;
    }
}
