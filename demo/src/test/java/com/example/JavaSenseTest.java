package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JavaSense core functionality.
 */
class JavaSenseTest {

    @BeforeEach
    void setUp() {
        // Tests use static methods, so we need to be careful about state
        // In a real implementation, we'd add a reset() method to JavaSense
    }

    @Test
    @DisplayName("Should add and retrieve basic facts")
    void testAddFact() {
        Fact fact = new Fact("popular(Mary)", "test_fact", 0, 5);
        assertNotNull(fact);
        assertEquals("popular(Mary)", fact.getText());
        assertEquals("test_fact", fact.getName());
        assertEquals(0, fact.getStartTime());
        assertEquals(5, fact.getEndTime());
    }

    @Test
    @DisplayName("Should parse atoms correctly")
    void testAtomParsing() {
        Atom atom = Atom.parse("popular(Mary)");

        assertNotNull(atom);
        assertEquals("popular", atom.getPredicate());
        assertEquals(1, atom.getArgs().size());
        assertEquals("Mary", atom.getArgs().get(0));
    }

    @Test
    @DisplayName("Should parse atoms with multiple arguments")
    void testAtomParsingMultipleArgs() {
        Atom atom = Atom.parse("Friends(Alice,Bob)");

        assertNotNull(atom);
        assertEquals("Friends", atom.getPredicate());
        assertEquals(2, atom.getArgs().size());
        assertEquals("Alice", atom.getArgs().get(0));
        assertEquals("Bob", atom.getArgs().get(1));
    }

    @Test
    @DisplayName("Should create time intervals")
    void testInterval() {
        Interval interval = new Interval(0, 5);

        assertEquals(0, interval.getStart());
        assertEquals(5, interval.getEnd());
        assertTrue(interval.contains(3));
        assertFalse(interval.contains(6));
    }

    @Test
    @DisplayName("Should create timed facts with multiple intervals")
    void testTimedFact() {
        Atom atom = Atom.parse("popular(Mary)");
        TimedFact timedFact = new TimedFact(
            atom,
            "test",
            List.of(new Interval(0, 2), new Interval(5, 8))
        );

        assertNotNull(timedFact);
        assertEquals(2, timedFact.getIntervals().size());
        assertTrue(timedFact.isTrueAt(1));
        assertFalse(timedFact.isTrueAt(3));
        assertTrue(timedFact.isTrueAt(6));
    }

    @Test
    @DisplayName("Should create and parse rules")
    void testRuleCreation() {
        Rule rule = new Rule(
            "popular(x) <- popular(y), Friends(x,y)",
            "test_rule"
        );

        assertNotNull(rule);
        assertEquals("test_rule", rule.getName());
    }

    @Test
    @DisplayName("Should create rules with delays")
    void testRuleWithDelay() {
        Rule rule = new Rule(
            "popular(x) <-1 popular(y), Friends(x,y)",
            "delayed_rule"
        );

        assertNotNull(rule);
        assertEquals(1, rule.getDelay());
    }

    @Test
    @DisplayName("Should create rules with head intervals")
    void testRuleWithHeadInterval() {
        Rule rule = new Rule(
            "popular(x) : [0,5] <- popular(y), Friends(x,y)",
            "bounded_rule"
        );

        assertNotNull(rule);
        assertTrue(rule.getHeadStartOffset() >= 0);
        assertTrue(rule.getHeadEndOffset() >= 0);
    }

    @Test
    @DisplayName("Should create ReasoningInterpretation and query by timestep")
    void testReasoningInterpretation() {
        Atom atom1 = Atom.parse("popular(Mary)");
        Atom atom2 = Atom.parse("popular(John)");

        List<Set<Atom>> factsByTime = List.of(
            Set.of(atom1),
            Set.of(atom1, atom2)
        );

        ReasoningInterpretation interp = new ReasoningInterpretation(factsByTime);

        assertEquals(1, interp.getFactsAt(0).size());
        assertEquals(2, interp.getFactsAt(1).size());
        assertEquals(1, interp.getMaxTime());
    }

    @Test
    @DisplayName("Integration: Simple reasoning without graph")
    void testSimpleReasoning() {
        // This is a basic integration test
        // In a real scenario, we'd need a way to reset JavaSense state between tests

        // Create a simple rule and fact
        JavaSense.addFact(new Fact("base(A)", "fact1", 0, 5));
        JavaSense.addRule(new Rule("derived(x) <- base(x)", "rule1"));

        // Reason without a graph
        ReasoningInterpretation result = JavaSense.reason(null, 2);

        assertNotNull(result);
        assertTrue(result.getMaxTime() >= 0);

        // Check that we have facts at timestep 0
        Set<Atom> factsAt0 = result.getFactsAt(0);
        assertNotNull(factsAt0);
    }
}
