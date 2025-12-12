package com.example.gpu;

import com.example.Atom;
import com.example.Literal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GPU pattern matching.
 */
class GpuPatternMatcherTest {

    private GpuReasoningEngine gpu;
    private GpuFactStore store;
    private GpuPatternMatcher matcher;

    @BeforeEach
    void setUp() {
        gpu = new GpuReasoningEngine();

        if (!gpu.isGpuAvailable()) {
            System.out.println("Skipping GPU pattern matcher tests (no GPU available)");
            store = null;
            matcher = null;
            return;
        }

        store = new GpuFactStore(gpu);
    }

    @AfterEach
    void tearDown() {
        if (matcher != null) {
            matcher.cleanup();
        }
        if (store != null) {
            store.cleanup();
        }
        if (gpu != null) {
            gpu.cleanup();
        }
    }

    @Test
    void testSimplePatternMatch() {
        if (store == null) return;

        // Upload facts
        List<Atom> facts = Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("likes(bob,charlie)"),
            Atom.parse("popular(bob)")
        );
        store.uploadFacts(facts);

        matcher = new GpuPatternMatcher(gpu, store);

        // Match pattern: likes(X,Y)
        List<Literal> pattern = Arrays.asList(Literal.parse("likes(X,Y)"));
        List<Map<String, String>> subs = matcher.findSubstitutions(pattern);

        assertEquals(2, subs.size());

        // Check substitutions
        assertTrue(subs.stream().anyMatch(s ->
            s.get("X").equals("alice") && s.get("Y").equals("bob")
        ));
        assertTrue(subs.stream().anyMatch(s ->
            s.get("X").equals("bob") && s.get("Y").equals("charlie")
        ));
    }

    @Test
    void testPatternWithConstant() {
        if (store == null) return;

        List<Atom> facts = Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("likes(bob,charlie)"),
            Atom.parse("likes(alice,charlie)")
        );
        store.uploadFacts(facts);

        matcher = new GpuPatternMatcher(gpu, store);

        // Match pattern: likes(alice,X)
        List<Literal> pattern = Arrays.asList(Literal.parse("likes(alice,X)"));
        List<Map<String, String>> subs = matcher.findSubstitutions(pattern);

        assertEquals(2, subs.size());
        assertTrue(subs.stream().anyMatch(s -> s.get("X").equals("bob")));
        assertTrue(subs.stream().anyMatch(s -> s.get("X").equals("charlie")));
    }

    @Test
    void testNoMatches() {
        if (store == null) return;

        List<Atom> facts = Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("popular(bob)")
        );
        store.uploadFacts(facts);

        matcher = new GpuPatternMatcher(gpu, store);

        // Match pattern that doesn't exist
        List<Literal> pattern = Arrays.asList(Literal.parse("friend(X,Y)"));
        List<Map<String, String>> subs = matcher.findSubstitutions(pattern);

        assertEquals(0, subs.size());
    }

    @Test
    void testSingleVariablePattern() {
        if (store == null) return;

        List<Atom> facts = Arrays.asList(
            Atom.parse("popular(alice)"),
            Atom.parse("popular(bob)"),
            Atom.parse("popular(charlie)")
        );
        store.uploadFacts(facts);

        matcher = new GpuPatternMatcher(gpu, store);

        // Match pattern: popular(X)
        List<Literal> pattern = Arrays.asList(Literal.parse("popular(X)"));
        List<Map<String, String>> subs = matcher.findSubstitutions(pattern);

        assertEquals(3, subs.size());
        assertTrue(subs.stream().anyMatch(s -> s.get("X").equals("alice")));
        assertTrue(subs.stream().anyMatch(s -> s.get("X").equals("bob")));
        assertTrue(subs.stream().anyMatch(s -> s.get("X").equals("charlie")));
    }

    @Test
    void testNoVariablesPattern() {
        if (store == null) return;

        List<Atom> facts = Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("likes(bob,charlie)")
        );
        store.uploadFacts(facts);

        matcher = new GpuPatternMatcher(gpu, store);

        // Match ground pattern: likes(alice,bob)
        List<Literal> pattern = Arrays.asList(Literal.parse("likes(alice,bob)"));
        List<Map<String, String>> subs = matcher.findSubstitutions(pattern);

        assertEquals(1, subs.size());
        assertTrue(subs.get(0).isEmpty());  // No variables = empty substitution
    }

    @Test
    void testLargeDataset() {
        if (store == null) return;

        // Create 1000 facts
        List<Atom> facts = new java.util.ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            facts.add(Atom.parse("fact" + i + "(arg" + i + ",arg" + (i+1) + ")"));
        }
        store.uploadFacts(facts);

        matcher = new GpuPatternMatcher(gpu, store);

        // Match pattern
        List<Literal> pattern = Arrays.asList(Literal.parse("fact500(X,Y)"));
        List<Map<String, String>> subs = matcher.findSubstitutions(pattern);

        assertEquals(1, subs.size());
        assertEquals("arg500", subs.get(0).get("X"));
        assertEquals("arg501", subs.get(0).get("Y"));
    }

    @Test
    void testNegationNotSupported() {
        if (store == null) return;

        store.uploadFacts(Arrays.asList(Atom.parse("test(a)")));
        matcher = new GpuPatternMatcher(gpu, store);

        assertThrows(UnsupportedOperationException.class, () -> {
            matcher.findSubstitutions(Arrays.asList(Literal.parse("not test(X)")));
        });
    }

    @Test
    void testMultipleLiteralsNotSupported() {
        if (store == null) return;

        store.uploadFacts(Arrays.asList(Atom.parse("test(a)")));
        matcher = new GpuPatternMatcher(gpu, store);

        assertThrows(UnsupportedOperationException.class, () -> {
            matcher.findSubstitutions(Arrays.asList(
                Literal.parse("test(X)"),
                Literal.parse("other(Y)")
            ));
        });
    }

    @Test
    void testStats() {
        if (store == null) return;

        store.uploadFacts(Arrays.asList(Atom.parse("test(a)")));
        matcher = new GpuPatternMatcher(gpu, store);

        // Initially no patterns matched
        GpuPatternMatcher.MatcherStats stats = matcher.getStats();
        assertEquals(0, stats.patternsMatched);

        // Match a pattern
        matcher.findSubstitutions(Arrays.asList(Literal.parse("test(X)")));

        // Stats updated
        stats = matcher.getStats();
        assertEquals(1, stats.patternsMatched);
        assertTrue(stats.totalTimeNs > 0);
        assertTrue(stats.avgTimeNs > 0);

        System.out.println("Matcher stats: " + stats);
    }

    @Test
    void testCleanup() {
        if (store == null) return;

        store.uploadFacts(Arrays.asList(Atom.parse("test(a)")));
        matcher = new GpuPatternMatcher(gpu, store);

        // Should not throw
        assertDoesNotThrow(() -> matcher.cleanup());

        // Calling cleanup twice should be safe
        assertDoesNotThrow(() -> matcher.cleanup());
    }
}
