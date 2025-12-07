package com.example;

import java.util.List;

public class MainSupplier {
    public static void main(String[] args) {
        // 1) Load supplier network graph
        Graph kb = Interpretation.loadKnowledgeBase("supplier_network.graphml");

        // 2) Rules
        JavaSense.addRule(new Rule(
            "served_by(x,z) <-1 supplies(x,y), ships(y,w), serves(w,z)",
            "served_by_rule"
        ));

        JavaSense.addRule(new Rule(
            "at_risk(z) <-1 served_by(x,z), disrupted(x)",
            "at_risk_rule"
        ));

        // 3) Facts
        JavaSense.addFact(
            new Fact("disrupted(S2)", "disrupted_fact", 0, 10)
        );
        JavaSense.addFact(
            new Fact("critical_customer(C1)", "critical_customer_fact", 0, 10)
        );

        // 4) Reason
        int timesteps = 3;
        ReasoningInterpretation interp = JavaSense.reason(kb, timesteps);

        // 5) Display results
        displayServedBy(interp);
        displayAtRisk(interp);
    }

    private static void displayServedBy(ReasoningInterpretation interp) {
        int T = interp.getMaxTime();
        for (int t = 0; t <= T; t++) {
            System.out.println("\nTIMESTEP - " + t + " (served_by)");
            System.out.println("supplier   customer");
            int row = 0;
            for (Atom a : interp.getFactsAt(t)) {
                if (!a.getPredicate().equals("served_by")) continue;
                String supplier = a.getArgs().get(0);
                String customer = a.getArgs().get(1);
                System.out.printf("%d    %-6s   %s%n", row++, supplier, customer);
            }
        }
    }

    private static void displayAtRisk(ReasoningInterpretation interp) {
        int T = interp.getMaxTime();
        for (int t = 0; t <= T; t++) {
            System.out.println("\nTIMESTEP - " + t + " (at_risk)");
            System.out.println("customer   at_risk");
            int row = 0;
            for (Atom a : interp.getFactsAt(t)) {
                if (!a.getPredicate().equals("at_risk")) continue;
                String customer = a.getArgs().get(0);
                System.out.printf("%d    %-6s   [1.0,1.0]%n", row++, customer);
            }
        }
    }
}

