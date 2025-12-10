package com.example;

import java.util.List;

/**
 * Test to identify the OptimizedReasoner bug.
 */
public class TestOptimizedReasonerBug {

    public static void main(String[] args) {
        System.out.println("=== Testing OptimizedReasoner Bug ===\n");

        // Test with facts that DON'T span all timesteps (should be dynamic)
        testDynamicFacts();

        // Test with facts that DO span all timesteps (will be static)
        testStaticFacts();
    }

    private static void testDynamicFacts() {
        System.out.println("Test 1: Dynamic Facts (interval [0,5] out of 10 timesteps)");

        OptimizedReasoner reasoner = new OptimizedReasoner();

        // Add facts that don't span all timesteps -> will be stored as DYNAMIC
        reasoner.addFact(new TimedFact(
                Atom.parse("disrupted(SupplierA)"),
                "f1",
                List.of(new Interval(0, 5))  // Only 0-5, not all timesteps
        ));

        reasoner.addFact(new TimedFact(
                Atom.parse("supplies(SupplierA,Engine)"),
                "f2",
                List.of(new Interval(0, 5))
        ));

        reasoner.addRule(new Rule(
                "atRisk(Engine) <-1 disrupted(SupplierA), supplies(SupplierA,Engine)",
                "rule1"
        ));

        ReasoningInterpretation result = reasoner.reason(10);

        boolean found = false;
        for (int t = 0; t <= 10; t++) {
            for (Atom atom : result.getFactsAt(t)) {
                if (atom.getPredicate().equals("atRisk")) {
                    System.out.println("  ✓ Found: " + atom + " at t=" + t);
                    found = true;
                }
            }
        }

        if (!found) {
            System.out.println("  ✗ FAILED with dynamic facts");
        }
        System.out.println();
    }

    private static void testStaticFacts() {
        System.out.println("Test 2: Static Facts (interval [0,10] = all timesteps)");

        OptimizedReasoner reasoner = new OptimizedReasoner();

        // Add facts that span ALL timesteps -> will be stored as STATIC
        reasoner.addFact(new TimedFact(
                Atom.parse("disrupted(SupplierA)"),
                "f1",
                List.of(new Interval(0, 10))  // All timesteps
        ));

        reasoner.addFact(new TimedFact(
                Atom.parse("supplies(SupplierA,Engine)"),
                "f2",
                List.of(new Interval(0, 10))
        ));

        reasoner.addRule(new Rule(
                "atRisk(Engine) <-1 disrupted(SupplierA), supplies(SupplierA,Engine)",
                "rule1"
        ));

        ReasoningInterpretation result = reasoner.reason(10);

        boolean found = false;
        for (int t = 0; t <= 10; t++) {
            for (Atom atom : result.getFactsAt(t)) {
                if (atom.getPredicate().equals("atRisk")) {
                    System.out.println("  ✓ Found: " + atom + " at t=" + t);
                    found = true;
                }
            }
        }

        if (!found) {
            System.out.println("  ✗ FAILED with static facts (THIS IS THE BUG!)");
        }
        System.out.println();
    }
}
