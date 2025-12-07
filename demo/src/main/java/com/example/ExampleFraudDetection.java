package com.example;

/**
 * Example: Fraud Detection System
 *
 * <p>Demonstrates using JavaSense to detect suspicious patterns in
 * financial transactions over time.</p>
 *
 * <p>Scenario: Flag accounts with unusual transaction patterns.</p>
 */
public class ExampleFraudDetection {
    public static void main(String[] args) {
        // Load transaction network
        Graph kb = Interpretation.loadKnowledgeBase("transactions.graphml");

        // Define suspicious behaviors
        JavaSense.addFact(new Fact("largeTransaction(Account123)", "t1", 1, 1));
        JavaSense.addFact(new Fact("largeTransaction(Account123)", "t2", 2, 2));
        JavaSense.addFact(new Fact("largeTransaction(Account123)", "t3", 3, 3));

        JavaSense.addFact(new Fact("newAccount(Account456)", "n1", 0, 2));
        JavaSense.addFact(new Fact("largeTransaction(Account456)", "t4", 1, 1));

        // Rule 1: Multiple large transactions in short time = suspicious
        JavaSense.addRule(new Rule(
            "suspicious(a) <- largeTransaction(a), largeTransaction(a)",
            "frequent_large"
        ));

        // Rule 2: New accounts with large transactions are risky
        JavaSense.addRule(new Rule(
            "risky(a) <- newAccount(a), largeTransaction(a)",
            "new_account_risk"
        ));

        // Rule 3: Accounts transferring to suspicious accounts get flagged
        JavaSense.addRule(new Rule(
            "flagged(x) <-1 Transfer(x,y), suspicious(y)",
            "guilt_by_association"
        ));

        // Rule 4: Flagged accounts need review
        JavaSense.addRule(new Rule(
            "needsReview(a) <- flagged(a)",
            "review_flagged"
        ));

        JavaSense.addRule(new Rule(
            "needsReview(a) <- risky(a)",
            "review_risky"
        ));

        // Run fraud detection
        ReasoningInterpretation result = JavaSense.reason(kb, 10);

        // Display fraud alerts
        System.out.println("=== Fraud Detection Alerts ===");
        for (int t = 0; t <= 10; t++) {
            long suspicious = result.getFactsAt(t).stream()
                .filter(a -> a.getPredicate().equals("suspicious"))
                .count();

            long risky = result.getFactsAt(t).stream()
                .filter(a -> a.getPredicate().equals("risky"))
                .count();

            long needsReview = result.getFactsAt(t).stream()
                .filter(a -> a.getPredicate().equals("needsReview"))
                .count();

            if (suspicious > 0 || risky > 0 || needsReview > 0) {
                System.out.printf("t=%d: %d suspicious, %d risky, %d need review%n",
                    t, suspicious, risky, needsReview);
            }
        }

        // Detail accounts needing review
        System.out.println("\n=== Accounts Requiring Review ===");
        for (Atom fact : result.getFactsAt(10)) {
            if (fact.getPredicate().equals("needsReview")) {
                System.out.println("  " + fact.getArgs().get(0));
            }
        }
    }
}
