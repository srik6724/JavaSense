package com.example;

/**
 * Example: Disease Outbreak Modeling
 *
 * <p>Demonstrates using JavaSense to model how diseases spread through
 * contact networks over time.</p>
 *
 * <p>Scenario: Track disease transmission with incubation period.</p>
 */
public class ExampleDiseaseSpreading {
    public static void main(String[] args) {
        // Load contact network
        Graph kb = Interpretation.loadKnowledgeBase("contact_network.graphml");

        // Patient zero is infected at timestep 0
        JavaSense.addFact(new Fact("infected(PatientZero)", "initial", 0, 20));

        // Rule 1: Disease spreads to contacts after 2-day incubation
        JavaSense.addRule(new Rule(
            "infected(x) <-2 infected(y), Contact(y,x)",
            "transmission"
        ));

        // Rule 2: Infected people become symptomatic after 3 days
        JavaSense.addRule(new Rule(
            "symptomatic(x) <-3 infected(x)",
            "symptom_onset"
        ));

        // Rule 3: Close contacts of symptomatic people should quarantine
        JavaSense.addRule(new Rule(
            "quarantine(x) <-1 symptomatic(y), Contact(x,y)",
            "quarantine_rule"
        ));

        // Run simulation for 15 days
        ReasoningInterpretation result = JavaSense.reason(kb, 15);

        // Analyze outbreak progression
        System.out.println("=== Disease Outbreak Simulation ===");
        for (int day = 0; day <= 15; day++) {
            long infected = result.getFactsAt(day).stream()
                .filter(a -> a.getPredicate().equals("infected"))
                .count();

            long symptomatic = result.getFactsAt(day).stream()
                .filter(a -> a.getPredicate().equals("symptomatic"))
                .count();

            long quarantined = result.getFactsAt(day).stream()
                .filter(a -> a.getPredicate().equals("quarantine"))
                .count();

            System.out.printf("Day %2d: %d infected, %d symptomatic, %d quarantined%n",
                day, infected, symptomatic, quarantined);
        }
    }
}
