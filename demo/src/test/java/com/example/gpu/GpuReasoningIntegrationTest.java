package com.example.gpu;

import com.example.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for GPU-accelerated reasoning.
 *
 * <p>These tests verify that GPU pattern matching produces identical results
 * to CPU pattern matching and integrates correctly with OptimizedReasoner.</p>
 */
class GpuReasoningIntegrationTest {

    private OptimizedReasoner cpuReasoner;
    private OptimizedReasoner gpuReasoner;
    private GpuReasoningEngine gpu;

    @BeforeEach
    void setUp() {
        gpu = new GpuReasoningEngine();

        cpuReasoner = new OptimizedReasoner();
        cpuReasoner.setGpuMode(GpuMode.CPU_ONLY);

        gpuReasoner = new OptimizedReasoner();
        if (gpu.isGpuAvailable()) {
            gpuReasoner.setGpuMode(GpuMode.GPU_ONLY);
        } else {
            gpuReasoner.setGpuMode(GpuMode.CPU_ONLY);
        }
    }

    @AfterEach
    void tearDown() {
        if (gpu != null) {
            gpu.cleanup();
        }
    }

    /**
     * Helper to collect all facts across all timesteps.
     */
    private Set<Atom> getAllFacts(ReasoningInterpretation interp) {
        Set<Atom> allFacts = new HashSet<>();
        for (int t = 0; t <= interp.getMaxTime(); t++) {
            allFacts.addAll(interp.getFactsAt(t));
        }
        return allFacts;
    }

    @Test
    void testSimpleRuleGpuVsCpu() {
        if (!gpu.isGpuAvailable()) {
            System.out.println("Skipping GPU integration test (no GPU available)");
            return;
        }

        // Rule: likes(X,Y) <- 1 likes(X,Y)
        Rule rule = new Rule("friends(X,Y) <- 1 likes(X,Y)", "rule1");

        // Facts
        cpuReasoner.addRule(rule);
        cpuReasoner.addFact(new TimedFact(Atom.parse("likes(alice,bob)"), "f1", 0, 10));
        cpuReasoner.addFact(new TimedFact(Atom.parse("likes(bob,charlie)"), "f2", 0, 10));
        cpuReasoner.addFact(new TimedFact(Atom.parse("likes(charlie,alice)"), "f3", 0, 10));

        ReasoningInterpretation cpuResults = cpuReasoner.reason(5);

        // Run on GPU
        gpuReasoner.addRule(rule);
        gpuReasoner.addFact(new TimedFact(Atom.parse("likes(alice,bob)"), "f1", 0, 10));
        gpuReasoner.addFact(new TimedFact(Atom.parse("likes(bob,charlie)"), "f2", 0, 10));
        gpuReasoner.addFact(new TimedFact(Atom.parse("likes(charlie,alice)"), "f3", 0, 10));

        ReasoningInterpretation gpuResults = gpuReasoner.reason(5);

        // Results should be identical
        assertEquals(getAllFacts(cpuResults).size(), getAllFacts(gpuResults).size(),
            "GPU and CPU should produce same number of results");
    }

    @Test
    void testTransitiveClosureGpuVsCpu() {
        if (!gpu.isGpuAvailable()) return;

        // Transitive closure: path(X,Z) <- 1 path(X,Y), edge(Y,Z)
        Rule transitiveRule = new Rule("path(X,Z) <- 1 path(X,Y), edge(Y,Z)", "trans");
        Rule baseRule = new Rule("path(X,Y) <- 1 edge(X,Y)", "base");

        // Create a chain: a -> b -> c -> d
        cpuReasoner.addRule(baseRule);
        cpuReasoner.addRule(transitiveRule);
        cpuReasoner.addFact(new TimedFact(Atom.parse("edge(a,b)"), "e1", 0, 10));
        cpuReasoner.addFact(new TimedFact(Atom.parse("edge(b,c)"), "e2", 0, 10));
        cpuReasoner.addFact(new TimedFact(Atom.parse("edge(c,d)"), "e3", 0, 10));

        ReasoningInterpretation cpuResults = cpuReasoner.reason(5);

        // Run on GPU
        gpuReasoner.addRule(baseRule);
        gpuReasoner.addRule(transitiveRule);
        gpuReasoner.addFact(new TimedFact(Atom.parse("edge(a,b)"), "e1", 0, 10));
        gpuReasoner.addFact(new TimedFact(Atom.parse("edge(b,c)"), "e2", 0, 10));
        gpuReasoner.addFact(new TimedFact(Atom.parse("edge(c,d)"), "e3", 0, 10));

        ReasoningInterpretation gpuResults = gpuReasoner.reason(5);

        // Results should be identical
        assertEquals(getAllFacts(cpuResults).size(), getAllFacts(gpuResults).size(),
            "GPU and CPU transitive closure should have same number of facts");

        // Should derive path(a,d)
        assertTrue(getAllFacts(gpuResults).stream().anyMatch(atom ->
            atom.toString().equals("path(a,d)")),
            "Should derive transitive path from a to d");
    }

    @Test
    void testTemporalRuleGpuVsCpu() {
        if (!gpu.isGpuAvailable()) return;

        // Temporal rule: alert(X) : [0,0] <- 1 event(X)
        Rule rule = new Rule("alert(X) : [0,0] <- 1 event(X)", "temporal");

        cpuReasoner.addRule(rule);
        cpuReasoner.addFact(new TimedFact(Atom.parse("event(fire)"), "ev1", 0, 2));
        cpuReasoner.addFact(new TimedFact(Atom.parse("event(alarm)"), "ev2", 3, 5));

        ReasoningInterpretation cpuResults = cpuReasoner.reason(10);

        // Run on GPU
        gpuReasoner.addRule(rule);
        gpuReasoner.addFact(new TimedFact(Atom.parse("event(fire)"), "ev1", 0, 2));
        gpuReasoner.addFact(new TimedFact(Atom.parse("event(alarm)"), "ev2", 3, 5));

        ReasoningInterpretation gpuResults = gpuReasoner.reason(10);

        assertEquals(getAllFacts(cpuResults).size(), getAllFacts(gpuResults).size(),
            "GPU and CPU temporal reasoning should be identical");
    }

    @Test
    void testLargeDatasetGpuVsCpu() {
        if (!gpu.isGpuAvailable()) return;

        // Generate large dataset
        Rule rule = new Rule("derived(X,Y) <- 1 base(X,Y)", "large");

        // CPU version
        for (int i = 0; i < 1000; i++) {
            cpuReasoner.addFact(new TimedFact(
                Atom.parse("base(n" + i + ",m" + i + ")"),
                "bf" + i,
                0, 5
            ));
        }
        cpuReasoner.addRule(rule);

        long cpuStart = System.nanoTime();
        ReasoningInterpretation cpuResults = cpuReasoner.reason(5);
        long cpuTime = System.nanoTime() - cpuStart;

        // GPU version
        for (int i = 0; i < 1000; i++) {
            gpuReasoner.addFact(new TimedFact(
                Atom.parse("base(n" + i + ",m" + i + ")"),
                "bf" + i,
                0, 5
            ));
        }
        gpuReasoner.addRule(rule);

        long gpuStart = System.nanoTime();
        ReasoningInterpretation gpuResults = gpuReasoner.reason(5);
        long gpuTime = System.nanoTime() - gpuStart;

        // Results should be identical
        assertEquals(getAllFacts(cpuResults).size(), getAllFacts(gpuResults).size());

        // Performance info
        System.out.println("\nLarge Dataset Performance (1000 facts):");
        System.out.println("  CPU time: " + String.format("%.2f", cpuTime / 1_000_000.0) + " ms");
        System.out.println("  GPU time: " + String.format("%.2f", gpuTime / 1_000_000.0) + " ms");
        if (gpuTime > 0) {
            System.out.println("  Speedup:  " + String.format("%.1f", (double) cpuTime / gpuTime) + "x");
        }
    }

    @Test
    void testGpuFallbackOnMultiLiteral() {
        if (!gpu.isGpuAvailable()) return;

        // Multi-literal rule (should fall back to CPU)
        Rule rule = new Rule("result(X,Z) <- 1 a(X,Y), b(Y,Z)", "multi");

        gpuReasoner.addRule(rule);
        gpuReasoner.addFact(new TimedFact(Atom.parse("a(1,2)"), "a1", 0, 5));
        gpuReasoner.addFact(new TimedFact(Atom.parse("b(2,3)"), "b1", 0, 5));

        // Should not throw - should fall back to CPU
        assertDoesNotThrow(() -> {
            ReasoningInterpretation results = gpuReasoner.reason(5);
            assertTrue(getAllFacts(results).stream().anyMatch(atom ->
                atom.toString().equals("result(1,3)")),
                "Should derive result(1,3) via CPU fallback");
        });
    }

    @Test
    void testGpuFallbackOnNegation() {
        if (!gpu.isGpuAvailable()) return;

        // Rule with negation (should fall back to CPU)
        Rule rule = new Rule("allowed(X) <- 1 person(X), not blocked(X)", "negation");

        gpuReasoner.addRule(rule);
        gpuReasoner.addFact(new TimedFact(Atom.parse("person(alice)"), "p1", 0, 5));
        gpuReasoner.addFact(new TimedFact(Atom.parse("person(bob)"), "p2", 0, 5));
        gpuReasoner.addFact(new TimedFact(Atom.parse("blocked(bob)"), "b1", 0, 5));

        // Should not throw - should fall back to CPU
        assertDoesNotThrow(() -> {
            ReasoningInterpretation results = gpuReasoner.reason(5);
            assertTrue(getAllFacts(results).stream().anyMatch(atom ->
                atom.getPredicate().equals("allowed")),
                "Should derive allowed facts via CPU fallback");
        });
    }

    @Test
    void testAutoModeDecision() {
        OptimizedReasoner autoReasoner = new OptimizedReasoner();
        autoReasoner.setGpuMode(GpuMode.AUTO);

        Rule rule = new Rule("derived(X) <- 1 base(X)", "auto");
        autoReasoner.addRule(rule);

        // Small dataset - should use CPU
        for (int i = 0; i < 10; i++) {
            autoReasoner.addFact(new TimedFact(
                Atom.parse("base(n" + i + ")"),
                "f" + i,
                0, 5
            ));
        }

        boolean willUseGpuSmall = autoReasoner.willUseGpu(5);
        System.out.println("\nAuto mode with 10 facts: " +
            (willUseGpuSmall ? "GPU" : "CPU"));

        // Large dataset - should use GPU (if available)
        OptimizedReasoner autoReasoner2 = new OptimizedReasoner();
        autoReasoner2.setGpuMode(GpuMode.AUTO);
        autoReasoner2.addRule(rule);

        for (int i = 0; i < 10000; i++) {
            autoReasoner2.addFact(new TimedFact(
                Atom.parse("base(n" + i + ")"),
                "f" + i,
                0, 5
            ));
        }

        boolean willUseGpuLarge = autoReasoner2.willUseGpu(5);
        System.out.println("Auto mode with 10000 facts: " +
            (willUseGpuLarge ? "GPU" : "CPU"));

        if (gpu.isGpuAvailable()) {
            assertFalse(willUseGpuSmall, "Small dataset should use CPU");
            assertTrue(willUseGpuLarge, "Large dataset should use GPU");
        }
    }

    @Test
    void testGpuWithSemiNaiveEvaluation() {
        if (!gpu.isGpuAvailable()) return;

        // Test that GPU works correctly with semi-naive evaluation (default behavior)
        Rule rule = new Rule("derived(X,Y) <- 1 base(X,Y)", "seminaive");
        gpuReasoner.addRule(rule);

        // Add facts at different timesteps
        gpuReasoner.addFact(new TimedFact(
            Atom.parse("base(a,b)"), "f1", 0, 0));
        gpuReasoner.addFact(new TimedFact(
            Atom.parse("base(c,d)"), "f2", 2, 2));

        ReasoningInterpretation results = gpuReasoner.reason(5);

        // Should derive facts at correct timesteps
        assertTrue(results.getFactsAt(1).stream().anyMatch(atom ->
            atom.toString().equals("derived(a,b)")),
            "Should derive derived(a,b) at t=1");
        assertTrue(results.getFactsAt(3).stream().anyMatch(atom ->
            atom.toString().equals("derived(c,d)")),
            "Should derive derived(c,d) at t=3");
    }

    @Test
    void testGpuWithIndexing() {
        if (!gpu.isGpuAvailable()) return;

        // Test that GPU works correctly with indexing optimization (enabled by default)
        Rule rule = new Rule("result(X) <- 1 test(X)", "indexing");
        gpuReasoner.addRule(rule);

        for (int i = 0; i < 100; i++) {
            gpuReasoner.addFact(new TimedFact(
                Atom.parse("test(n" + i + ")"),
                "t" + i,
                0, 5
            ));
        }

        ReasoningInterpretation results = gpuReasoner.reason(5);

        long resultCount = getAllFacts(results).stream()
            .filter(atom -> atom.getPredicate().equals("result"))
            .count();

        assertEquals(100, resultCount, "Should derive 100 result facts");
    }

    @Test
    void testGpuCorrectnessWithVariableReuse() {
        if (!gpu.isGpuAvailable()) return;

        // Test pattern with repeated variables: same(X,X) <- 1 value(X)
        Rule rule = new Rule("same(X,X) <- 1 value(X)", "varreuse");

        // CPU version
        cpuReasoner.addRule(rule);
        cpuReasoner.addFact(new TimedFact(Atom.parse("value(a)"), "v1", 0, 5));
        cpuReasoner.addFact(new TimedFact(Atom.parse("value(b)"), "v2", 0, 5));

        ReasoningInterpretation cpuResults = cpuReasoner.reason(5);

        // GPU version
        gpuReasoner.addRule(rule);
        gpuReasoner.addFact(new TimedFact(Atom.parse("value(a)"), "v1", 0, 5));
        gpuReasoner.addFact(new TimedFact(Atom.parse("value(b)"), "v2", 0, 5));

        ReasoningInterpretation gpuResults = gpuReasoner.reason(5);

        assertEquals(getAllFacts(cpuResults).size(), getAllFacts(gpuResults).size(),
            "GPU should handle repeated variables correctly");
    }

    @Test
    void testGpuPerformanceBenefit() {
        if (!gpu.isGpuAvailable()) return;

        System.out.println("\n" + "=".repeat(80));
        System.out.println("GPU Performance Scaling Test");
        System.out.println("=".repeat(80));

        int[] sizes = {100, 1000, 10000};

        System.out.printf("%-15s %-15s %-15s %-15s%n",
            "Dataset Size", "CPU Time (ms)", "GPU Time (ms)", "Speedup");
        System.out.println("-".repeat(60));

        for (int size : sizes) {
            // CPU test
            OptimizedReasoner cpu = new OptimizedReasoner();
            cpu.setGpuMode(GpuMode.CPU_ONLY);
            Rule rule = new Rule("r(X,Y) <- 1 b(X,Y)", "perf");
            cpu.addRule(rule);

            for (int i = 0; i < size; i++) {
                cpu.addFact(new TimedFact(
                    Atom.parse("b(n" + i + ",m" + i + ")"),
                    "f" + i, 0, 3));
            }

            long cpuStart = System.nanoTime();
            cpu.reason(3);
            long cpuTime = System.nanoTime() - cpuStart;

            // GPU test
            OptimizedReasoner gpuTest = new OptimizedReasoner();
            gpuTest.setGpuMode(GpuMode.GPU_ONLY);
            gpuTest.addRule(rule);

            for (int i = 0; i < size; i++) {
                gpuTest.addFact(new TimedFact(
                    Atom.parse("b(n" + i + ",m" + i + ")"),
                    "f" + i, 0, 3));
            }

            long gpuStart = System.nanoTime();
            gpuTest.reason(3);
            long gpuTime = System.nanoTime() - gpuStart;

            double cpuMs = cpuTime / 1_000_000.0;
            double gpuMs = gpuTime / 1_000_000.0;
            double speedup = gpuTime > 0 ? cpuTime / (double) gpuTime : 0;

            System.out.printf("%-15s %-15s %-15s %-15s%n",
                String.format("%,d", size),
                String.format("%.2f", cpuMs),
                String.format("%.2f", gpuMs),
                String.format("%.1fx", speedup)
            );
        }

        System.out.println("=".repeat(80));
    }
}
