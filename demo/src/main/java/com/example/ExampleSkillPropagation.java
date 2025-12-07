package com.example;

/**
 * Example: Workplace Skill Propagation
 *
 * <p>Demonstrates using JavaSense to model how skills spread through
 * mentorship and collaboration in an organization.</p>
 *
 * <p>Scenario: Track skill acquisition through learning and collaboration.</p>
 */
public class ExampleSkillPropagation {
    public static void main(String[] args) {
        // Load organization collaboration network
        Graph kb = Interpretation.loadKnowledgeBase("organization.graphml");

        // Initial skill assignments
        JavaSense.addFact(new Fact("hasSkill(Alice,Java)", "s1", 0, 20));
        JavaSense.addFact(new Fact("hasSkill(Bob,Python)", "s2", 0, 20));
        JavaSense.addFact(new Fact("hasSkill(Carol,MachineLearning)", "s3", 0, 20));

        // Rule 1: Mentees learn skills from mentors (takes 2 time units)
        JavaSense.addRule(new Rule(
            "hasSkill(mentee,skill) <-2 Mentors(mentor,mentee), hasSkill(mentor,skill)",
            "mentorship_learning"
        ));

        // Rule 2: Collaborators share skills (takes 3 time units)
        JavaSense.addRule(new Rule(
            "hasSkill(x,skill) <-3 Collaborates(x,y), hasSkill(y,skill)",
            "collaborative_learning"
        ));

        // Rule 3: People with Java + Python become full-stack developers
        JavaSense.addRule(new Rule(
            "hasSkill(p,FullStack) <- hasSkill(p,Java), hasSkill(p,Python)",
            "fullstack_combo"
        ));

        // Rule 4: Full-stack developers with ML become AI engineers
        JavaSense.addRule(new Rule(
            "hasSkill(p,AIEngineering) <- hasSkill(p,FullStack), hasSkill(p,MachineLearning)",
            "ai_engineer_combo"
        ));

        // Track skill growth over time
        ReasoningInterpretation result = JavaSense.reason(kb, 15);

        // Analyze skill distribution
        System.out.println("=== Skill Propagation Timeline ===");
        for (int t = 0; t <= 15; t++) {
            System.out.println("\nQuarter " + t + ":");

            // Count people per skill
            java.util.Map<String, Long> skillCounts = new java.util.HashMap<>();

            for (Atom fact : result.getFactsAt(t)) {
                if (fact.getPredicate().equals("hasSkill")) {
                    String skill = fact.getArgs().get(1);
                    skillCounts.put(skill, skillCounts.getOrDefault(skill, 0L) + 1);
                }
            }

            // Display skills with counts
            skillCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .forEach(entry ->
                    System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " people")
                );
        }

        // Final skill summary
        System.out.println("\n=== Final Skill Roster (t=15) ===");
        for (Atom fact : result.getFactsAt(15)) {
            if (fact.getPredicate().equals("hasSkill")) {
                String person = fact.getArgs().get(0);
                String skill = fact.getArgs().get(1);
                System.out.println("  " + person + " knows " + skill);
            }
        }
    }
}
