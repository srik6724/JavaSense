package com.example;

/**
 * Example: Product Recommendation System
 *
 * <p>Demonstrates using JavaSense for e-commerce recommendations based on
 * user preferences and purchase history.</p>
 *
 * <p>Scenario: Recommend products to users based on what similar users purchased.</p>
 */
public class ExampleRecommendations {
    public static void main(String[] args) {
        // Load customer similarity graph
        Graph kb = Interpretation.loadKnowledgeBase("customers.graphml");

        // Define initial purchases
        JavaSense.addFact(new Fact("purchased(Alice,Laptop)", "p1", 0, 10));
        JavaSense.addFact(new Fact("purchased(Alice,Mouse)", "p2", 0, 10));
        JavaSense.addFact(new Fact("purchased(Bob,Laptop)", "p3", 0, 10));

        // Rule 1: Users who buy the same product are similar
        JavaSense.addRule(new Rule(
            "similar(x,y) <- purchased(x,p), purchased(y,p)",
            "similarity_rule"
        ));

        // Rule 2: Recommend products that similar users bought
        JavaSense.addRule(new Rule(
            "recommended(x,p) <-1 similar(x,y), purchased(y,p)",
            "recommendation_rule"
        ));

        // Run reasoning
        ReasoningInterpretation result = JavaSense.reason(kb, 5);

        // Display recommendations
        System.out.println("=== Product Recommendations ===");
        for (int t = 0; t <= 5; t++) {
            System.out.println("\nTimestep " + t + ":");
            for (Atom fact : result.getFactsAt(t)) {
                if (fact.getPredicate().equals("recommended")) {
                    String user = fact.getArgs().get(0);
                    String product = fact.getArgs().get(1);
                    System.out.println("  Recommend " + product + " to " + user);
                }
            }
        }
    }
}
