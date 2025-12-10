package com.example;

import java.util.List;

/**
 * Test with a simpler rule pattern.
 */
public class TestSingleNode2 {

    public static void main(String[] args) {
        System.out.println("=== Test 1: Original Rule (FAILING) ===");
        testOriginalRule();

        System.out.println("\n=== Test 2: Simplified Rule (TESTING) ===");
        testSimplifiedRule();
    }

    private static void testOriginalRule() {
        OptimizedReasoner reasoner = new OptimizedReasoner();

        reasoner.addFact(new TimedFact(
                Atom.parse("disrupted(SupplierA)"),
                "f1",
                List.of(new Interval(0, 10))
        ));

        reasoner.addFact(new TimedFact(
                Atom.parse("supplies(SupplierA,Engine)"),
                "f2",
                List.of(new Interval(0, 10))
        ));

        reasoner.addRule(new Rule(
                "atRisk(part) <-1 disrupted(supplier), supplies(supplier,part)",
                "rule1"
        ));

        ReasoningInterpretation result = reasoner.reason(10);

        boolean found = false;
        for (int t = 0; t <= 10; t++) {
            for (Atom atom : result.getFactsAt(t)) {
                if (atom.getPredicate().equals("atRisk")) {
                    System.out.println("✓ Found: " + atom + " at t=" + t);
                    found = true;
                }
            }
        }

        if (!found) {
            System.out.println("✗ FAILED: No atRisk facts derived");
        }
    }

    private static void testSimplifiedRule() {
        OptimizedReasoner reasoner = new OptimizedReasoner();

        // Try with concrete atoms (no variables)
        reasoner.addFact(new TimedFact(
                Atom.parse("disrupted(SupplierA)"),
                "f1",
                List.of(new Interval(0, 10))
        ));

        reasoner.addFact(new TimedFact(
                Atom.parse("supplies(SupplierA,Engine)"),
                "f2",
                List.of(new Interval(0, 10))
        ));

        // Try ground rule (no variables)
        reasoner.addRule(new Rule(
                "atRisk(Engine) <-1 disrupted(SupplierA), supplies(SupplierA,Engine)",
                "rule1_ground"
        ));

        ReasoningInterpretation result = reasoner.reason(10);

        boolean found = false;
        for (int t = 0; t <= 10; t++) {
            for (Atom atom : result.getFactsAt(t)) {
                if (atom.getPredicate().equals("atRisk")) {
                    System.out.println("✓ Found: " + atom + " at t=" + t);
                    found = true;
                }
            }
        }

        if (!found) {
            System.out.println("✗ FAILED: No atRisk facts derived even with ground rule");
        }
    }
}
