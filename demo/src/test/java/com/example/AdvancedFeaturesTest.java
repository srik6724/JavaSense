package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for advanced JavaSense features.
 */
class AdvancedFeaturesTest {

    @BeforeEach
    void setUp() {
        // Clear any static state if needed
    }

    @Test
    void testQueryLanguage() {
        // Setup
        Reasoner reasoner = new Reasoner();
        reasoner.addFact(new TimedFact(Atom.parse("popular(Alice)"), "f1", 0, 5));
        reasoner.addFact(new TimedFact(Atom.parse("popular(Bob)"), "f2", 2, 8));
        reasoner.addRule(new Rule("trendy(x) <- 1 popular(x)", "r1"));

        ReasoningInterpretation result = reasoner.reason(10);

        // Test 1: Query at specific time
        Query q1 = Query.parse("popular(x)").atTime(3);
        List<QueryResult> results1 = q1.execute(result);
        assertEquals(2, results1.size());

        // Test 2: Query with variable binding
        Query q2 = Query.parse("trendy(x)")
            .withVariable("x", "Alice")
            .inTimeRange(0, 10);
        List<QueryResult> results2 = q2.execute(result);
        assertTrue(results2.size() > 0);

        // Test 3: Aggregate unique bindings
        Set<String> uniqueVars = Query.getUniqueBindings(results1, "x");
        assertTrue(uniqueVars.contains("Alice"));
        assertTrue(uniqueVars.contains("Bob"));
    }

    @Test
    void testProvenanceTracking() {
        Reasoner reasoner = new Reasoner();
        reasoner.addFact(new TimedFact(Atom.parse("knows(Alice,Bob)"), "f1", 0, 10));
        reasoner.addRule(new Rule("friend(x,y) <- 1 knows(x,y)", "r1"));

        ReasoningInterpretation result = reasoner.reason(5);

        // Test derivation tracking
        Atom derived = Atom.parse("friend(Alice,Bob)");
        assertTrue(result.isDerived(derived, 1));

        // Test base fact
        Atom base = Atom.parse("knows(Alice,Bob)");
        assertFalse(result.isDerived(base, 0));

        // Test explanation
        String explanation = result.explain(derived, 1);
        assertNotNull(explanation);
        assertTrue(explanation.contains("r1"));

        // Test derivation tree
        DerivationTree tree = result.getDerivationTree(derived, 1);
        assertNotNull(tree);
        assertEquals(derived, tree.getAtom());
        assertFalse(tree.isBaseFact());
    }

    @Test
    void testNegationAsFailure() {
        Reasoner reasoner = new Reasoner();
        reasoner.addFact(new TimedFact(Atom.parse("bird(tweety)"), "f1", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("bird(opus)"), "f2", 0, 10));
        reasoner.addFact(new TimedFact(Atom.parse("penguin(opus)"), "f3", 0, 10));

        // Birds can fly unless they are penguins
        reasoner.addRule(new Rule("canFly(x) <- 1 bird(x), not penguin(x)", "r1"));

        ReasoningInterpretation result = reasoner.reason(5);

        Set<Atom> factsAt1 = result.getFactsAt(1);

        // tweety can fly (bird but not penguin)
        assertTrue(factsAt1.contains(Atom.parse("canFly(tweety)")));

        // opus cannot fly (bird AND penguin)
        assertFalse(factsAt1.contains(Atom.parse("canFly(opus)")));
    }

    @Test
    void testLiteralParsing() {
        // Test positive literal
        Literal pos = Literal.parse("popular(x)");
        assertFalse(pos.isNegated());
        assertTrue(pos.isPositive());
        assertEquals("popular", pos.getAtom().getPredicate());

        // Test negative literal with "not"
        Literal neg1 = Literal.parse("not popular(x)");
        assertTrue(neg1.isNegated());
        assertFalse(neg1.isPositive());
        assertEquals("popular", neg1.getAtom().getPredicate());

        // Test negative literal with "~"
        Literal neg2 = Literal.parse("~popular(x)");
        assertTrue(neg2.isNegated());
        assertEquals("popular", neg2.getAtom().getPredicate());
    }

    @Test
    void testConstraintValidation() {
        Reasoner reasoner = new Reasoner();
        reasoner.addFact(new TimedFact(Atom.parse("at(Alice,RoomA)"), "f1", 0, 5));
        reasoner.addFact(new TimedFact(Atom.parse("at(Alice,RoomB)"), "f2", 3, 7));

        ReasoningInterpretation result = reasoner.reason(10);

        ConstraintValidator validator = new ConstraintValidator();

        // Add uniqueness constraint
        Constraint unique = Constraint.Builder.uniqueness("unique_loc", "at", 0);
        validator.addConstraint(unique);

        // Validate
        ConstraintValidator.ValidationResult validationResult = validator.validate(result);

        // Should have violations at t=3,4,5 (overlapping)
        assertFalse(validationResult.isValid());
        assertTrue(validationResult.hasViolations());
        assertTrue(validationResult.getHardViolations().size() > 0);
    }

    @Test
    void testConstraintBuilders() {
        // Test cardinality constraint
        Constraint card = Constraint.Builder.cardinality("min_one", "at", 1, 10);
        assertNotNull(card);
        assertTrue(card.isHard());

        // Test mutual exclusion
        Constraint mutex = Constraint.Builder.mutualExclusion("mutex", "open", "closed");
        assertNotNull(mutex);

        // Test with valid data (one predicate)
        List<Atom> facts1 = List.of(Atom.parse("open(door)"));
        assertTrue(mutex.check(facts1, 0));

        // Test with invalid data (both predicates)
        List<Atom> facts2 = List.of(
            Atom.parse("open(door)"),
            Atom.parse("closed(window)")
        );
        assertFalse(mutex.check(facts2, 0));
    }

    @Test
    void testConflictDetection() {
        ConflictDetector detector = new ConflictDetector();

        // Add rules with overlapping heads
        Rule r1 = new Rule("popular(x) <- 1 hasFriends(x)", "r1");
        Rule r2 = new Rule("popular(y) <- 1 hasFriends(y)", "r2");

        detector.addRule(r1);
        detector.addRule(r2);

        ConflictDetector.ConflictAnalysis analysis = detector.analyze();

        assertTrue(analysis.hasConflicts());
        assertEquals(1, analysis.getConflictsByType(
            ConflictDetector.ConflictType.OVERLAPPING_HEADS).size());
    }

    @Test
    void testCircularDependencyDetection() {
        ConflictDetector detector = new ConflictDetector();

        // Create circular dependency
        Rule r1 = new Rule("a(x) <- 1 b(x)", "r1");
        Rule r2 = new Rule("b(x) <- 1 c(x)", "r2");
        Rule r3 = new Rule("c(x) <- 1 a(x)", "r3");

        detector.addRule(r1);
        detector.addRule(r2);
        detector.addRule(r3);

        ConflictDetector.ConflictAnalysis analysis = detector.analyze();

        assertTrue(analysis.hasConflicts());
        List<ConflictDetector.RuleConflict> cycles =
            analysis.getConflictsByType(ConflictDetector.ConflictType.CIRCULAR_DEPENDENCY);
        assertTrue(cycles.size() > 0);
    }

    @Test
    void testIncrementalReasoning() {
        IncrementalReasoner incReasoner = new IncrementalReasoner();

        incReasoner.addRule(new Rule("friend(x,y) <- 1 knows(x,y)", "r1"));

        // Initial fact
        incReasoner.addFact(new TimedFact(Atom.parse("knows(Alice,Bob)"), "f1", 0, 10));

        // Initial reasoning
        ReasoningInterpretation result1 = incReasoner.reason(10);
        Set<Atom> facts1 = result1.getFactsAt(1);
        assertTrue(facts1.contains(Atom.parse("friend(Alice,Bob)")));

        // Add new fact incrementally
        incReasoner.addFact(new TimedFact(Atom.parse("knows(Bob,Charlie)"), "f2", 0, 10));

        // Incremental reasoning
        ReasoningInterpretation result2 = incReasoner.incrementalReason();
        Set<Atom> facts2 = result2.getFactsAt(1);

        // Should have both old and new derived facts
        assertTrue(facts2.contains(Atom.parse("friend(Alice,Bob)")));
        assertTrue(facts2.contains(Atom.parse("friend(Bob,Charlie)")));
    }

    @Test
    void testIncrementalReasoningWithPropagation() {
        IncrementalReasoner incReasoner = new IncrementalReasoner();

        incReasoner.addRule(new Rule("friend(x,y) <- 1 knows(x,y)", "r1"));
        incReasoner.addRule(new Rule("popular(x) <- 1 friend(y,x)", "r2"));

        // Initial facts
        incReasoner.addFact(new TimedFact(Atom.parse("knows(Alice,Bob)"), "f1", 0, 10));
        incReasoner.reason(10);

        // Add new fact that should trigger cascading derivations
        incReasoner.addFact(new TimedFact(Atom.parse("knows(Charlie,Alice)"), "f2", 0, 10));
        ReasoningInterpretation result2 = incReasoner.incrementalReason();

        // Alice should become popular due to Charlie knowing her
        Set<Atom> facts = result2.getFactsAt(2);
        assertTrue(facts.contains(Atom.parse("popular(Alice)")));
    }

    @Test
    void testQueryResultEquality() {
        Atom atom = Atom.parse("test(x)");
        Map<String, String> bindings = Map.of("x", "value");

        QueryResult qr1 = new QueryResult(atom, 5, bindings);
        QueryResult qr2 = new QueryResult(atom, 5, bindings);
        QueryResult qr3 = new QueryResult(atom, 6, bindings);

        assertEquals(qr1, qr2);
        assertNotEquals(qr1, qr3);
        assertEquals(qr1.hashCode(), qr2.hashCode());
    }

    @Test
    void testDerivationTreeDepth() {
        Reasoner reasoner = new Reasoner();
        reasoner.addFact(new TimedFact(Atom.parse("a(x)"), "f1", 0, 10));
        reasoner.addRule(new Rule("b(x) <- 1 a(x)", "r1"));
        reasoner.addRule(new Rule("c(x) <- 1 b(x)", "r2"));
        reasoner.addRule(new Rule("d(x) <- 1 c(x)", "r3"));

        ReasoningInterpretation result = reasoner.reason(10);

        DerivationTree tree = result.getDerivationTree(Atom.parse("d(x)"), 3);
        assertTrue(tree.getDepth() >= 3); // Multi-level derivation
        assertFalse(tree.getChildren().isEmpty());
    }
}
