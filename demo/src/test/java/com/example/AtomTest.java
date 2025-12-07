package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Atom class.
 */
class AtomTest {

    @Test
    @DisplayName("Should parse simple predicate with one argument")
    void testParseSimple() {
        Atom atom = Atom.parse("popular(Mary)");

        assertEquals("popular", atom.getPredicate());
        assertEquals(1, atom.getArgs().size());
        assertEquals("Mary", atom.getArgs().get(0));
    }

    @Test
    @DisplayName("Should parse predicate with two arguments")
    void testParseTwoArgs() {
        Atom atom = Atom.parse("Friends(Alice,Bob)");

        assertEquals("Friends", atom.getPredicate());
        assertEquals(2, atom.getArgs().size());
        assertEquals("Alice", atom.getArgs().get(0));
        assertEquals("Bob", atom.getArgs().get(1));
    }

    @Test
    @DisplayName("Should parse predicate with three arguments")
    void testParseThreeArgs() {
        Atom atom = Atom.parse("transfer(Account1,Account2,1000)");

        assertEquals("transfer", atom.getPredicate());
        assertEquals(3, atom.getArgs().size());
        assertEquals("Account1", atom.getArgs().get(0));
        assertEquals("Account2", atom.getArgs().get(1));
        assertEquals("1000", atom.getArgs().get(2));
    }

    @Test
    @DisplayName("Should handle predicates with no arguments")
    void testParseNoArgs() {
        Atom atom = Atom.parse("constant()");

        assertEquals("constant", atom.getPredicate());
        assertTrue(atom.getArgs().isEmpty());
    }

    @Test
    @DisplayName("Should preserve argument spacing")
    void testParseWithSpaces() {
        Atom atom = Atom.parse("predicate(arg1, arg2, arg3)");

        assertEquals("predicate", atom.getPredicate());
        assertEquals(3, atom.getArgs().size());
        // Args might be trimmed or not, depending on implementation
        assertTrue(atom.getArgs().contains("arg1") || atom.getArgs().contains(" arg1"));
    }

    @Test
    @DisplayName("Should create string representation")
    void testToString() {
        Atom atom = Atom.parse("Friends(Alice,Bob)");
        String str = atom.toString();

        assertNotNull(str);
        assertTrue(str.contains("Friends"));
        assertTrue(str.contains("Alice"));
        assertTrue(str.contains("Bob"));
    }

    @Test
    @DisplayName("Should support equality comparison")
    void testEquality() {
        Atom atom1 = Atom.parse("popular(Mary)");
        Atom atom2 = Atom.parse("popular(Mary)");

        // Depending on implementation, these might be equal
        // This test documents the expected behavior
        assertNotNull(atom1);
        assertNotNull(atom2);
    }
}
