package com.example;

/**
 * Example: Dynamic Access Control System
 *
 * <p>Demonstrates using JavaSense for role-based access control with
 * temporal permissions and inheritance.</p>
 *
 * <p>Scenario: Employees inherit permissions from roles, with time-limited access.</p>
 */
public class ExampleAccessControl {
    public static void main(String[] args) {
        // Load organization structure
        Graph kb = Interpretation.loadKnowledgeBase("organization.graphml");

        // Define role permissions
        JavaSense.addFact(new Fact("permission(Admin,Database)", "p1", 0, 10));
        JavaSense.addFact(new Fact("permission(Admin,Server)", "p2", 0, 10));
        JavaSense.addFact(new Fact("permission(Developer,Code)", "p3", 0, 10));

        // Define role assignments (time-limited)
        JavaSense.addFact(new Fact("hasRole(Alice,Admin)", "r1", 0, 5));
        JavaSense.addFact(new Fact("hasRole(Bob,Developer)", "r2", 0, 10));

        // Rule 1: Users inherit permissions from their roles
        JavaSense.addRule(new Rule(
            "canAccess(u,r) <- hasRole(u,role), permission(role,r)",
            "role_permission"
        ));

        // Rule 2: Managers inherit permissions from their reports
        JavaSense.addRule(new Rule(
            "canAccess(mgr,r) <- Manages(mgr,emp), canAccess(emp,r)",
            "manager_inheritance"
        ));

        // Rule 3: Temporary guest access (only valid for 3 timesteps)
        JavaSense.addRule(new Rule(
            "canAccess(u,r) : [0,3] <- Guest(u), permission(Guest,r)",
            "guest_access",
            java.util.List.of(new Interval(0, 3))
        ));

        // Run access control evaluation
        ReasoningInterpretation result = JavaSense.reason(kb, 10);

        // Display access rights over time
        System.out.println("=== Access Control Timeline ===");
        for (int t = 0; t <= 10; t++) {
            System.out.println("\nTimestep " + t + " (Time-limited permissions):");

            // Note: This if-statement is just for DISPLAY filtering,
            // not for rule execution! The rules above are declarative
            // and processed automatically by the reasoning engine.
            // With 100+ rules, you'd use the Query API instead:
            // Query.parse("canAccess(x,y)").atTime(t).execute(result)
            for (Atom fact : result.getFactsAt(t)) {
                if (fact.getPredicate().equals("canAccess")) {
                    String user = fact.getArgs().get(0);
                    String resource = fact.getArgs().get(1);
                    System.out.println("  " + user + " can access " + resource);
                }
            }
        }
    }
}
