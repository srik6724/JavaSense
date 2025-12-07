package com.example;

import java.util.List;

/**
 * Demonstrates advanced JavaSense features:
 * - Query language
 * - Provenance/explanation tracking
 * - Negation (NAF)
 * - Constraints
 * - Conflict detection
 * - Incremental reasoning
 */
public class ExampleAdvancedFeatures {

    public static void main(String[] args) {
        System.out.println("=== JavaSense Advanced Features Demo ===\n");

        // Example 1: Query Language
        demonstrateQueryLanguage();

        // Example 2: Provenance Tracking
        demonstrateProvenance();

        // Example 3: Negation as Failure
        demonstrateNegation();

        // Example 4: Constraints
        demonstrateConstraints();

        // Example 5: Conflict Detection
        demonstrateConflictDetection();

        // Example 6: Incremental Reasoning
        demonstrateIncrementalReasoning();
    }

    private static void demonstrateQueryLanguage() {
        System.out.println("\n--- 1. Query Language ---");

        JavaSense.addFact(new Fact("popular(Alice)", "f1", 0, 5));
        JavaSense.addFact(new Fact("popular(Bob)", "f2", 2, 8));
        JavaSense.addFact(new Fact("popular(Charlie)", "f3", 5, 10));

        JavaSense.addRule(new Rule("trendy(x) <- 1 popular(x)", "r1"));

        ReasoningInterpretation result = JavaSense.reason(null, 10);

        // Query 1: Find all popular people at time 5
        Query q1 = Query.parse("popular(x)").atTime(5);
        List<QueryResult> results1 = q1.execute(result);
        System.out.println("Query: popular(x) at t=5");
        for (QueryResult qr : results1) {
            System.out.println("  " + qr.getBinding("x") + " is popular");
        }

        // Query 2: Find when Alice is trendy
        Query q2 = Query.parse("trendy(x)")
            .withVariable("x", "Alice")
            .inTimeRange(0, 10);
        List<QueryResult> results2 = q2.execute(result);
        System.out.println("\nQuery: When is Alice trendy?");
        for (QueryResult qr : results2) {
            System.out.println("  t=" + qr.getTime());
        }

        // Query 3: Count popular people over time
        Query q3 = Query.parse("popular(x)").inTimeRange(0, 10);
        List<QueryResult> results3 = q3.execute(result);
        System.out.println("\nUnique popular people: " +
            Query.getUniqueBindings(results3, "x"));
    }

    private static void demonstrateProvenance() {
        System.out.println("\n--- 2. Provenance Tracking ---");

        JavaSense.addFact(new Fact("knows(Alice, Bob)", "f1", 0, 10));
        JavaSense.addFact(new Fact("knows(Bob, Charlie)", "f2", 0, 10));

        JavaSense.addRule(new Rule("friend(x,y) <- 1 knows(x,y)", "r1"));
        JavaSense.addRule(new Rule("friend(x,z) <- 1 friend(x,y), knows(y,z)", "r2"));

        ReasoningInterpretation result = JavaSense.reason(null, 5);

        // Explain how a fact was derived
        Atom factToExplain = Atom.parse("friend(Alice,Charlie)");
        System.out.println("Explaining: " + factToExplain + " at t=2");
        System.out.println(result.explain(factToExplain, 2));

        // Get derivation tree
        DerivationTree tree = result.getDerivationTree(factToExplain, 2);
        System.out.println("Derivation tree:");
        System.out.println(tree.toTreeString());

        // Check if a fact is derived vs base
        Atom baseFact = Atom.parse("knows(Alice,Bob)");
        System.out.println("\nIs " + baseFact + " derived? " +
            result.isDerived(baseFact, 1));
        System.out.println("Is " + factToExplain + " derived? " +
            result.isDerived(factToExplain, 2));
    }

    private static void demonstrateNegation() {
        System.out.println("\n--- 3. Negation as Failure ---");

        JavaSense.addFact(new Fact("bird(tweety)", "f1", 0, 10));
        JavaSense.addFact(new Fact("penguin(opus)", "f2", 0, 10));
        JavaSense.addFact(new Fact("bird(opus)", "f3", 0, 10));

        // Birds can fly unless they are penguins
        JavaSense.addRule(new Rule("canFly(x) <- 1 bird(x), not penguin(x)", "r1"));

        ReasoningInterpretation result = JavaSense.reason(null, 5);

        System.out.println("Facts at t=1:");
        for (Atom a : result.getFactsAt(1)) {
            if (a.getPredicate().equals("canFly")) {
                System.out.println("  " + a);
            }
        }

        System.out.println("\nExplanation:");
        System.out.println("- tweety can fly (bird but not penguin)");
        System.out.println("- opus cannot fly (bird AND penguin, negation fails)");
    }

    private static void demonstrateConstraints() {
        System.out.println("\n--- 4. Constraint Validation ---");

        JavaSense.addFact(new Fact("at(Alice, RoomA)", "f1", 0, 5));
        JavaSense.addFact(new Fact("at(Alice, RoomB)", "f2", 3, 7));

        ReasoningInterpretation result = JavaSense.reason(null, 10);

        // Create and check constraints
        ConstraintValidator validator = new ConstraintValidator();

        // Constraint: A person can only be in one location at a time
        Constraint uniqueLocation = Constraint.Builder.uniqueness(
            "unique_location",
            "at",
            0  // person is the first argument
        );
        validator.addConstraint(uniqueLocation);

        // Constraint: Must have at least one location fact
        Constraint atLeastOne = Constraint.Builder.cardinality(
            "min_locations",
            "at",
            1,
            Integer.MAX_VALUE
        );
        validator.addConstraint(atLeastOne);

        ConstraintValidator.ValidationResult validationResult = validator.validate(result);
        validationResult.display();

        System.out.println("\nValidation summary: " + validationResult);
    }

    private static void demonstrateConflictDetection() {
        System.out.println("\n--- 5. Rule Conflict Detection ---");

        ConflictDetector detector = new ConflictDetector();

        // Add potentially conflicting rules
        Rule r1 = new Rule("popular(x) <- 1 hasFriends(x)", "r1");
        Rule r2 = new Rule("popular(y) <- 1 hasFriends(y)", "r2"); // Overlapping head
        Rule r3 = new Rule("hasFriends(x) <- 1 popular(x)", "r3"); // Circular dependency

        detector.addRule(r1);
        detector.addRule(r2);
        detector.addRule(r3);

        ConflictDetector.ConflictAnalysis analysis = detector.analyze();
        analysis.display();

        System.out.println("\nConflict summary: " + analysis);
        System.out.println("Overlapping heads: " +
            analysis.getConflictsByType(ConflictDetector.ConflictType.OVERLAPPING_HEADS).size());
        System.out.println("Circular dependencies: " +
            analysis.getConflictsByType(ConflictDetector.ConflictType.CIRCULAR_DEPENDENCY).size());
    }

    private static void demonstrateIncrementalReasoning() {
        System.out.println("\n--- 6. Incremental Reasoning ---");

        IncrementalReasoner incReasoner = new IncrementalReasoner();

        incReasoner.addRule(new Rule("friend(x,y) <- 1 knows(x,y)", "r1"));
        incReasoner.addRule(new Rule("popular(x) <- 1 friend(y,x)", "r2"));

        // Initial facts
        incReasoner.addFact(new TimedFact(
            Atom.parse("knows(Alice,Bob)"),
            "f1",
            0, 10
        ));

        System.out.println("Initial reasoning:");
        ReasoningInterpretation result1 = incReasoner.reason(10);
        System.out.println("Facts at t=1:");
        for (Atom a : result1.getFactsAt(1)) {
            System.out.println("  " + a);
        }

        // Add new fact incrementally
        System.out.println("\nAdding new fact: knows(Charlie,Alice)");
        incReasoner.addFact(new TimedFact(
            Atom.parse("knows(Charlie,Alice)"),
            "f2",
            0, 10
        ));

        System.out.println("Incremental reasoning:");
        ReasoningInterpretation result2 = incReasoner.incrementalReason();
        System.out.println("Facts at t=1 (updated):");
        for (Atom a : result2.getFactsAt(1)) {
            System.out.println("  " + a);
        }

        System.out.println("\nNote: Alice is now popular because Charlie knows Alice!");
    }
}
