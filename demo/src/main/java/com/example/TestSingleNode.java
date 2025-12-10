package com.example;

import java.util.List;

/**
 * Test single-node reasoning to verify rule works.
 */
public class TestSingleNode {

    public static void main(String[] args) {
        System.out.println("=== Testing Single-Node Reasoning ===");
        System.out.println();

        OptimizedReasoner reasoner = new OptimizedReasoner();

        // Add base facts
        reasoner.addFact(new TimedFact(
                Atom.parse("disrupted(SupplierA)"),
                "disruption_A",
                List.of(new Interval(0, 10))
        ));

        reasoner.addFact(new TimedFact(
                Atom.parse("supplies(SupplierA,Engine)"),
                "supply_A_Engine",
                List.of(new Interval(0, 10))
        ));

        System.out.println("Added base facts:");
        System.out.println("  - disrupted(SupplierA)");
        System.out.println("  - supplies(SupplierA,Engine)");
        System.out.println();

        // Add rule
        Rule rule = new Rule(
                "atRisk(part) <-1 disrupted(supplier), supplies(supplier,part)",
                "supply_risk"
        );
        reasoner.addRule(rule);
        System.out.println("Added rule: " + rule.getRaw());
        System.out.println("  Delay: " + rule.getDelay());
        System.out.println("  Body: " + rule.getBodyAtoms());
        System.out.println();

        // Perform reasoning
        System.out.println("Performing reasoning for 10 timesteps...");
        ReasoningInterpretation result = reasoner.reason(10);
        System.out.println("Done!");
        System.out.println();

        // Check results at each timestep
        System.out.println("Results:");
        for (int t = 0; t <= 3; t++) {
            System.out.println("Timestep t=" + t + ":");
            for (Atom atom : result.getFactsAt(t)) {
                System.out.println("  - " + atom);
            }
            System.out.println();
        }

        // Check specifically for atRisk
        boolean foundAtRisk = false;
        for (int t = 0; t <= 10; t++) {
            for (Atom atom : result.getFactsAt(t)) {
                if (atom.getPredicate().equals("atRisk")) {
                    System.out.println("✓ Found derived fact: " + atom + " at t=" + t);
                    foundAtRisk = true;
                }
            }
        }

        if (!foundAtRisk) {
            System.out.println("✗ ERROR: atRisk(Engine) was NOT derived!");
            System.out.println();
            System.out.println("This means the OptimizedReasoner is not applying the rule correctly.");
        }
    }
}
