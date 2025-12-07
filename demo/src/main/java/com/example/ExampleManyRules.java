package com.example;

import java.util.List;

/**
 * Example: Managing 100+ Rules Properly
 *
 * <p>Demonstrates best practices for managing large rule sets in JavaSense.</p>
 *
 * <p>Key lessons:</p>
 * <ul>
 *   <li>Use declarative rules, not if-statements</li>
 *   <li>Load rules from external files</li>
 *   <li>Use Query API for filtering results</li>
 *   <li>Organize rules by domain/module</li>
 * </ul>
 */
public class ExampleManyRules {

    public static void main(String[] args) {
        System.out.println("=== Managing 100+ Rules Example ===\n");

        // Scenario: Enterprise compliance system with many rules

        // APPROACH 1: Hardcoded rules (DOESN'T SCALE)
        demonstrateWrongApproach();

        // APPROACH 2: Declarative rules with file loading (SCALES WELL)
        demonstrateCorrectApproach();

        // APPROACH 3: Using Query API instead of if-statements
        demonstrateQueryApproach();
    }

    /**
     * ❌ WRONG: Hardcoding many rules with if-statements
     * This doesn't scale to 100+ rules!
     */
    private static void demonstrateWrongApproach() {
        System.out.println("--- Wrong Approach: Hardcoded If-Statements ---\n");

        Reasoner reasoner = new Reasoner();

        // Add some base facts
        reasoner.addFact(new TimedFact(Atom.parse("employee(Alice)"), "f1", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("role(Alice,Admin)"), "f2", 0, 5));

        ReasoningInterpretation result = reasoner.reason(10);

        // BAD: Using if-statements to implement "rules"
        System.out.println("Attempting to implement rules with if-statements...");

        // This is terrible - doesn't scale!
        /*
        if (hasRole(alice, "Admin")) {
            addPermission(alice, "Database");
        }
        if (hasRole(alice, "Admin") && hasResource("Server")) {
            addPermission(alice, "Server");
        }
        // ... 98 more if-statements? NO!
        */

        System.out.println("Problem: With 100 rules, this becomes unmaintainable!");
        System.out.println("Solution: Use declarative rules instead.\n");
    }

    /**
     * ✅ CORRECT: Declarative rules loaded from files
     * This scales to 1000+ rules!
     */
    private static void demonstrateCorrectApproach() {
        System.out.println("--- Correct Approach: Declarative Rules from Files ---\n");

        // Create sample rule files (in practice, load from disk)
        createSampleRuleFiles();

        Reasoner reasoner = new Reasoner();

        // Add base facts
        reasoner.addFact(new TimedFact(Atom.parse("employee(Alice)"), "f1", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("hasRole(Alice,Admin)"), "f2", 0, 5));
        reasoner.addFact(new TimedFact(Atom.parse("hasRole(Bob,Developer)"), "f3", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("newAccount(Charlie)"), "f4", 1, 1));
        reasoner.addFact(new TimedFact(Atom.parse("largeTransfer(Charlie)"), "f5", 2, 2));

        // Load rules declaratively (the RIGHT way)
        // In production: JavaSense.addRulesFromFile("access_rules.txt");
        // For demo: add rules manually
        addAccessControlRules(reasoner);
        addFraudDetectionRules(reasoner);
        addComplianceRules(reasoner);

        System.out.println("Loaded 30+ rules from multiple domains");
        System.out.println("All rules processed automatically by the engine!\n");

        // Run reasoning - all rules fire automatically
        ReasoningInterpretation result = reasoner.reason(10);

        System.out.println("Results at t=5:");
        result.getFactsAt(5).forEach(fact -> {
            System.out.println("  " + fact);
        });

        System.out.println("\nNote: With 100+ rules, this approach works the same!");
        System.out.println("Add more rules = just add more lines to the rule file.\n");
    }

    /**
     * ✅ BEST: Using Query API instead of if-statements for filtering
     */
    private static void demonstrateQueryApproach() {
        System.out.println("--- Best Practice: Query API for Filtering ---\n");

        Reasoner reasoner = new Reasoner();

        // Setup
        reasoner.addFact(new TimedFact(Atom.parse("employee(Alice)"), "f1", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("employee(Bob)"), "f2", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("hasRole(Alice,Admin)"), "f3", 0, 5));
        reasoner.addFact(new TimedFact(Atom.parse("hasRole(Bob,Developer)"), "f4", 0, 10));

        reasoner.addRule(new Rule("canAccess(u,r) <- 1 hasRole(u,Admin)", "admin_access"));
        reasoner.addRule(new Rule("canEdit(u,r) <- 1 hasRole(u,Developer)", "dev_edit"));

        ReasoningInterpretation result = reasoner.reason(10);

        // WRONG: Using if-statements for filtering
        System.out.println("❌ Manual filtering with if-statements:");
        for (Atom fact : result.getFactsAt(1)) {
            if (fact.getPredicate().equals("canAccess")) {
                if (fact.getArgs().get(0).equals("Alice")) {
                    System.out.println("  " + fact);
                }
            }
        }

        // RIGHT: Using Query API
        System.out.println("\n✅ Query API (better):");
        Query q = Query.parse("canAccess(Alice, x)").atTime(1);
        List<QueryResult> results = q.execute(result);
        results.forEach(r -> System.out.println("  " + r.getFact()));

        // Advanced queries
        System.out.println("\n✅ Complex query:");
        Query q2 = Query.parse("canAccess(x, y)")
            .inTimeRange(0, 5)
            .withFilter(r -> r.getBinding("x").startsWith("A"));

        List<QueryResult> filtered = q2.execute(result);
        System.out.println("Found " + filtered.size() + " matching facts");

        System.out.println("\nWith 100+ rules producing 1000s of facts,");
        System.out.println("Query API >>> if-statements!\n");
    }

    // Helper methods to simulate loading rules from files

    private static void createSampleRuleFiles() {
        System.out.println("In production, create these files:");
        System.out.println("  rules/access_control.txt");
        System.out.println("  rules/fraud_detection.txt");
        System.out.println("  rules/compliance.txt");
        System.out.println();
    }

    private static void addAccessControlRules(Reasoner reasoner) {
        // 10 access control rules
        reasoner.addRule(new Rule("canAccess(u,Database) <- 1 hasRole(u,Admin)", "r1"));
        reasoner.addRule(new Rule("canAccess(u,Server) <- 1 hasRole(u,Admin)", "r2"));
        reasoner.addRule(new Rule("canAccess(u,Code) <- 1 hasRole(u,Developer)", "r3"));
        reasoner.addRule(new Rule("canAccess(u,Logs) <- 1 hasRole(u,Developer)", "r4"));
        reasoner.addRule(new Rule("canEdit(u,Code) <- 1 hasRole(u,Developer)", "r5"));
        reasoner.addRule(new Rule("canDelete(u,r) <- 1 hasRole(u,Admin), canAccess(u,r)", "r6"));
        reasoner.addRule(new Rule("canView(u,r) <- 1 canAccess(u,r)", "r7"));
        reasoner.addRule(new Rule("canView(u,Logs) <- 1 employee(u)", "r8"));
        reasoner.addRule(new Rule("needsApproval(u,r) <- 1 canDelete(u,r)", "r9"));
        reasoner.addRule(new Rule("audited(u,r,t) <- 1 canAccess(u,r)", "r10"));
        // ... add 90 more easily!
    }

    private static void addFraudDetectionRules(Reasoner reasoner) {
        // 10 fraud detection rules
        reasoner.addRule(new Rule("suspicious(x) <- 1 largeWithdrawal(x), largeWithdrawal(x)", "fr1"));
        reasoner.addRule(new Rule("risky(x) <- 1 newAccount(x), largeTransfer(x)", "fr2"));
        reasoner.addRule(new Rule("flagged(x) <- 1 suspicious(x)", "fr3"));
        reasoner.addRule(new Rule("flagged(x) <- 1 risky(x)", "fr4"));
        reasoner.addRule(new Rule("investigate(x) <- 1 flagged(x)", "fr5"));
        reasoner.addRule(new Rule("alert(x) <- 1 investigate(x)", "fr6"));
        reasoner.addRule(new Rule("highRisk(x) <- 1 suspicious(x), risky(x)", "fr7"));
        reasoner.addRule(new Rule("escalate(x) <- 1 highRisk(x)", "fr8"));
        reasoner.addRule(new Rule("freeze(x) <- 1 escalate(x)", "fr9"));
        reasoner.addRule(new Rule("notify(compliance,x) <- 1 freeze(x)", "fr10"));
        // ... add 90 more easily!
    }

    private static void addComplianceRules(Reasoner reasoner) {
        // 10 compliance rules
        reasoner.addRule(new Rule("compliant(x) <- 1 employee(x), not suspended(x)", "c1"));
        reasoner.addRule(new Rule("violation(x,rule) <- 1 employee(x), not compliant(x)", "c2"));
        reasoner.addRule(new Rule("requiresTraining(x) <- 1 newEmployee(x)", "c3"));
        reasoner.addRule(new Rule("certified(x) <- 1 employee(x), completedTraining(x)", "c4"));
        reasoner.addRule(new Rule("eligible(x,promotion) <- 1 certified(x)", "c5"));
        reasoner.addRule(new Rule("mustReview(mgr,x) <- 1 manages(mgr,x)", "c6"));
        reasoner.addRule(new Rule("approved(x,action) <- 1 reviewed(mgr,x)", "c7"));
        reasoner.addRule(new Rule("documented(x,action) <- 1 approved(x,action)", "c8"));
        reasoner.addRule(new Rule("retained(doc) <- 1 documented(x,action)", "c9"));
        reasoner.addRule(new Rule("archived(doc) <- 1 retained(doc)", "c10"));
        // ... add 90 more easily!
    }
}
