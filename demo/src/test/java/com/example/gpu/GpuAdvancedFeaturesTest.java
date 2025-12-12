package com.example.gpu;

import com.example.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for advanced GPU features: multi-literal patterns and negation.
 *
 * <p>Phase 7: Advanced GPU pattern matching capabilities</p>
 */
class GpuAdvancedFeaturesTest {

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
    void testMultiLiteralPattern_TwoLiterals() {
        if (!gpu.isGpuAvailable()) {
            System.out.println("Skipping GPU test (no GPU available)");
            return;
        }

        // Create reasoner with GPU
        OptimizedReasoner reasoner = new OptimizedReasoner();
        reasoner.setGpuMode(GpuMode.GPU_ONLY);

        // Add rule with multi-literal pattern: child(X) <- parent(Y,X), person(Y)
        Rule rule = new Rule("child(X) <- 1 parent(Y,X), person(Y)", "child_rule");
        reasoner.addRule(rule);

        // Add facts
        reasoner.addFact(new TimedFact(Atom.parse("parent(alice,bob)"), "f1", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("parent(bob,charlie)"), "f2", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("person(alice)"), "f3", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("person(bob)"), "f4", 0, 10));

        // Reason
        ReasoningInterpretation result = reasoner.reason(5);

        // Verify results
        Set<Atom> factsAt0 = result.getFactsAt(0);

        // Should derive: child(bob) and child(charlie)
        assertTrue(factsAt0.contains(Atom.parse("child(bob)")),
            "Should derive child(bob) from parent(alice,bob) and person(alice)");
        assertTrue(factsAt0.contains(Atom.parse("child(charlie)")),
            "Should derive child(charlie) from parent(bob,charlie) and person(bob)");

        reasoner.cleanup();
    }

    @Test
    void testMultiLiteralPattern_ThreeLiterals() {
        if (!gpu.isGpuAvailable()) return;

        OptimizedReasoner reasoner = new OptimizedReasoner();
        reasoner.setGpuMode(GpuMode.GPU_ONLY);

        // Rule: grandchild(X) <- parent(Y,X), parent(Z,Y), person(Z)
        Rule rule = new Rule("grandchild(X) <- 1 parent(Y,X), parent(Z,Y), person(Z)", "gc_rule");
        reasoner.addRule(rule);

        // Facts
        reasoner.addFact(new TimedFact(Atom.parse("parent(alice,bob)"), "f1", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("parent(bob,charlie)"), "f2", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("person(alice)"), "f3", 0, 10));

        ReasoningInterpretation result = reasoner.reason(5);

        // Should derive: grandchild(charlie)
        assertTrue(result.getFactsAt(0).contains(Atom.parse("grandchild(charlie)")));

        reasoner.cleanup();
    }

    @Test
    void testNegation_SimpleCase() {
        if (!gpu.isGpuAvailable()) return;

        OptimizedReasoner reasoner = new OptimizedReasoner();
        reasoner.setGpuMode(GpuMode.GPU_ONLY);

        // Rule: safe(X) <- person(X), not blocked(X)
        Rule rule = new Rule("safe(X) <- 1 person(X), not blocked(X)", "safe_rule");
        reasoner.addRule(rule);

        // Facts
        reasoner.addFact(new TimedFact(Atom.parse("person(alice)"), "f1", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("person(bob)"), "f2", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("person(charlie)"), "f3", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("blocked(bob)"), "f4", 0, 10));

        ReasoningInterpretation result = reasoner.reason(5);

        Set<Atom> factsAt0 = result.getFactsAt(0);

        // Should derive: safe(alice) and safe(charlie), but NOT safe(bob)
        assertTrue(factsAt0.contains(Atom.parse("safe(alice)")),
            "Should derive safe(alice) - not blocked");
        assertTrue(factsAt0.contains(Atom.parse("safe(charlie)")),
            "Should derive safe(charlie) - not blocked");
        assertFalse(factsAt0.contains(Atom.parse("safe(bob)")),
            "Should NOT derive safe(bob) - is blocked");

        reasoner.cleanup();
    }

    @Test
    void testNegation_MultipleNegations() {
        if (!gpu.isGpuAvailable()) return;

        OptimizedReasoner reasoner = new OptimizedReasoner();
        reasoner.setGpuMode(GpuMode.GPU_ONLY);

        // Rule: verified(X) <- user(X), not blocked(X), not suspended(X)
        Rule rule = new Rule("verified(X) <- 1 user(X), not blocked(X), not suspended(X)",
            "verified_rule");
        reasoner.addRule(rule);

        // Facts
        reasoner.addFact(new TimedFact(Atom.parse("user(alice)"), "f1", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("user(bob)"), "f2", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("user(charlie)"), "f3", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("blocked(bob)"), "f4", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("suspended(charlie)"), "f5", 0, 10));

        ReasoningInterpretation result = reasoner.reason(5);

        Set<Atom> factsAt0 = result.getFactsAt(0);

        // Only alice should be verified
        assertTrue(factsAt0.contains(Atom.parse("verified(alice)")));
        assertFalse(factsAt0.contains(Atom.parse("verified(bob)")));
        assertFalse(factsAt0.contains(Atom.parse("verified(charlie)")));

        reasoner.cleanup();
    }

    @Test
    void testMixedPattern_MultiLiteralWithNegation() {
        if (!gpu.isGpuAvailable()) return;

        OptimizedReasoner reasoner = new OptimizedReasoner();
        reasoner.setGpuMode(GpuMode.GPU_ONLY);

        // Rule: eligible(X) <- person(X), adult(X), not convicted(X)
        Rule rule = new Rule("eligible(X) <- 1 person(X), adult(X), not convicted(X)",
            "eligible_rule");
        reasoner.addRule(rule);

        // Facts
        reasoner.addFact(new TimedFact(Atom.parse("person(alice)"), "f1", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("person(bob)"), "f2", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("person(charlie)"), "f3", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("adult(alice)"), "f4", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("adult(bob)"), "f5", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("convicted(alice)"), "f6", 0, 10));

        ReasoningInterpretation result = reasoner.reason(5);

        Set<Atom> factsAt0 = result.getFactsAt(0);

        // Only bob should be eligible (adult and not convicted)
        assertTrue(factsAt0.contains(Atom.parse("eligible(bob)")));
        assertFalse(factsAt0.contains(Atom.parse("eligible(alice)"))); // convicted
        assertFalse(factsAt0.contains(Atom.parse("eligible(charlie)"))); // not adult

        reasoner.cleanup();
    }

    @Test
    void testGpuVsCpu_MultiLiteralPattern() {
        if (!gpu.isGpuAvailable()) return;

        // GPU version
        OptimizedReasoner gpuReasoner = new OptimizedReasoner();
        gpuReasoner.setGpuMode(GpuMode.GPU_ONLY);

        Rule rule = new Rule("result(X,Z) <- 1 edge(X,Y), edge(Y,Z)", "path2");
        gpuReasoner.addRule(rule);

        // Add edge facts
        for (int i = 0; i < 100; i++) {
            gpuReasoner.addFact(new TimedFact(
                Atom.parse("edge(n" + i + ",n" + (i + 1) + ")"), "e" + i, 0, 10));
        }

        long gpuStart = System.nanoTime();
        ReasoningInterpretation gpuResult = gpuReasoner.reason(5);
        long gpuTime = System.nanoTime() - gpuStart;

        // CPU version
        OptimizedReasoner cpuReasoner = new OptimizedReasoner();
        cpuReasoner.setGpuMode(GpuMode.CPU_ONLY);

        cpuReasoner.addRule(rule);
        for (int i = 0; i < 100; i++) {
            cpuReasoner.addFact(new TimedFact(
                Atom.parse("edge(n" + i + ",n" + (i + 1) + ")"), "e" + i, 0, 10));
        }

        long cpuStart = System.nanoTime();
        ReasoningInterpretation cpuResult = cpuReasoner.reason(5);
        long cpuTime = System.nanoTime() - cpuStart;

        // Verify same results
        assertEquals(cpuResult.getFactsAt(0).size(), gpuResult.getFactsAt(0).size(),
            "GPU and CPU should produce same number of results");

        System.out.println("Multi-literal pattern performance:");
        System.out.println("  CPU time: " + String.format("%.2f", cpuTime / 1_000_000.0) + " ms");
        System.out.println("  GPU time: " + String.format("%.2f", gpuTime / 1_000_000.0) + " ms");

        if (gpuTime < cpuTime) {
            double speedup = cpuTime / (double) gpuTime;
            System.out.println("  GPU speedup: " + String.format("%.1fx", speedup));
        }

        gpuReasoner.cleanup();
        cpuReasoner.cleanup();
    }

    @Test
    void testGpuVsCpu_Negation() {
        if (!gpu.isGpuAvailable()) return;

        // GPU version
        OptimizedReasoner gpuReasoner = new OptimizedReasoner();
        gpuReasoner.setGpuMode(GpuMode.GPU_ONLY);

        Rule rule = new Rule("active(X) <- 1 user(X), not deleted(X)", "active_users");
        gpuReasoner.addRule(rule);

        // Add 1000 users, 10% deleted
        for (int i = 0; i < 1000; i++) {
            gpuReasoner.addFact(new TimedFact(
                Atom.parse("user(u" + i + ")"), "u" + i, 0, 10));
            if (i % 10 == 0) {
                gpuReasoner.addFact(new TimedFact(
                    Atom.parse("deleted(u" + i + ")"), "d" + i, 0, 10));
            }
        }

        long gpuStart = System.nanoTime();
        ReasoningInterpretation gpuResult = gpuReasoner.reason(5);
        long gpuTime = System.nanoTime() - gpuStart;

        // CPU version
        OptimizedReasoner cpuReasoner = new OptimizedReasoner();
        cpuReasoner.setGpuMode(GpuMode.CPU_ONLY);

        cpuReasoner.addRule(rule);
        for (int i = 0; i < 1000; i++) {
            cpuReasoner.addFact(new TimedFact(
                Atom.parse("user(u" + i + ")"), "u" + i, 0, 10));
            if (i % 10 == 0) {
                cpuReasoner.addFact(new TimedFact(
                    Atom.parse("deleted(u" + i + ")"), "d" + i, 0, 10));
            }
        }

        long cpuStart = System.nanoTime();
        ReasoningInterpretation cpuResult = cpuReasoner.reason(5);
        long cpuTime = System.nanoTime() - cpuStart;

        // Verify results
        long activeUsersGpu = gpuResult.getFactsAt(0).stream()
            .filter(a -> a.getPredicate().equals("active"))
            .count();
        long activeUsersCpu = cpuResult.getFactsAt(0).stream()
            .filter(a -> a.getPredicate().equals("active"))
            .count();

        assertEquals(activeUsersCpu, activeUsersGpu,
            "GPU and CPU should produce same results");
        assertEquals(900, activeUsersGpu,
            "Should have 900 active users (90% of 1000)");

        System.out.println("Negation pattern performance:");
        System.out.println("  CPU time: " + String.format("%.2f", cpuTime / 1_000_000.0) + " ms");
        System.out.println("  GPU time: " + String.format("%.2f", gpuTime / 1_000_000.0) + " ms");

        if (gpuTime < cpuTime) {
            double speedup = cpuTime / (double) gpuTime;
            System.out.println("  GPU speedup: " + String.format("%.1fx", speedup));
        }

        gpuReasoner.cleanup();
        cpuReasoner.cleanup();
    }

    @Test
    void testComplexPattern_MultiLiteralNegationCombined() {
        if (!gpu.isGpuAvailable()) return;

        OptimizedReasoner reasoner = new OptimizedReasoner();
        reasoner.setGpuMode(GpuMode.GPU_ONLY);

        // Complex rule: recommend(User,Item) <-
        //   user(User), item(Item), likes(Friend,Item), friend(User,Friend), not owns(User,Item)
        Rule rule = new Rule(
            "recommend(U,I) <- 1 user(U), item(I), likes(F,I), friend(U,F), not owns(U,I)",
            "recommendation");
        reasoner.addRule(rule);

        // Facts
        reasoner.addFact(new TimedFact(Atom.parse("user(alice)"), "f1", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("user(bob)"), "f2", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("item(book1)"), "f3", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("item(book2)"), "f4", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("friend(alice,bob)"), "f5", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("likes(bob,book1)"), "f6", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("likes(bob,book2)"), "f7", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("owns(alice,book1)"), "f8", 0, 10));

        ReasoningInterpretation result = reasoner.reason(5);

        Set<Atom> factsAt0 = result.getFactsAt(0);

        // Should recommend book2 to alice (friend bob likes it, alice doesn't own it)
        assertTrue(factsAt0.contains(Atom.parse("recommend(alice,book2)")));

        // Should NOT recommend book1 to alice (she already owns it)
        assertFalse(factsAt0.contains(Atom.parse("recommend(alice,book1)")));

        reasoner.cleanup();
    }
}
