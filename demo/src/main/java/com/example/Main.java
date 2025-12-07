package com.example;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // 1) Load GraphML
        Graph kb = Interpretation.loadKnowledgeBase("knowledge_base.graphml");

        // 2) Add rule
        /*JavaSense.addRule(new Rule(
                "popular(x) <-1 popular(y), Friends(x,y), owns(y,z), owns(x,z)",
                "popular_rule"
        )); */

        // 3) Add initial fact: Mary is popular
        JavaSense.addFact(new Fact("popular(Mary)", "popular_fact", 0, 2));

        // popular(Mary) is true 0–2 and 5–8
/*JavaSense.addTimedFact(
    new TimedFact(
        Atom.parse("popular(Mary)"),
        "popular_mary",
        List.of(
            new Interval(0, 2),
            new Interval(5, 8)
        )
    )
);*/

// Rule only active in [0,2] and [5,5]
JavaSense.addRule(
    new Rule(
        "popular(x) : [0,2] <-1 popular(y), Friends(x,y)",
        "popular_rule",
        List.of(
            new Interval(0, 2),
            new Interval(5, 5)
        )
    )
);


        // 4) Reason
        int timesteps = 10;
        ReasoningInterpretation interp = JavaSense.reason(kb, timesteps);

        // 5) Pretty-print popularity like PyReason
        displayPopularity(interp, timesteps);
    }

    private static void displayPopularity(ReasoningInterpretation interp, int timesteps) {
         // or store size-1; if you don't have this, just track it manually

        for (int t = 0; t <= timesteps; t++) {
            System.out.println("\n TIMESTEP - " + t);
            System.out.println("  component    popular");

            int row = 0;
            for (Atom a : interp.getFactsAt(t)) {
                if (!a.getPredicate().equals("popular")) continue;
                String person = a.getArgs().get(0); // popular(person)
                // we treat annotation as [1.0,1.0] by default (like PyReason's [1,1])
                System.out.printf("%d      %-6s  [1.0,1.0]%n", row, person);
                row++;
            }
        }
    }
}
