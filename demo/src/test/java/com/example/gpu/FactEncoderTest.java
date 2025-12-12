package com.example.gpu;

import com.example.Atom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FactEncoder.
 */
class FactEncoderTest {

    private FactEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new FactEncoder();
    }

    @Test
    void testEncodeSimpleAtom() {
        Atom atom = Atom.parse("likes(alice,bob)");
        int[] encoded = encoder.encode(atom);

        assertNotNull(encoded);
        assertEquals(3, encoded.length);  // predicate + 2 args

        // All IDs should be positive
        assertTrue(encoded[0] > 0);  // predicate
        assertTrue(encoded[1] > 0);  // alice
        assertTrue(encoded[2] > 0);  // bob
    }

    @Test
    void testEncodeSingleArgAtom() {
        Atom atom = Atom.parse("popular(alice)");
        int[] encoded = encoder.encode(atom);

        assertNotNull(encoded);
        assertEquals(2, encoded.length);  // predicate + 1 arg
    }

    @Test
    void testEncodeNoArgAtom() {
        Atom atom = Atom.parse("true()");
        int[] encoded = encoder.encode(atom);

        assertNotNull(encoded);
        assertEquals(1, encoded.length);  // predicate only
    }

    @Test
    void testEncodeNullAtom() {
        assertThrows(IllegalArgumentException.class, () -> {
            encoder.encode(null);
        });
    }

    @Test
    void testDecodeSimpleAtom() {
        Atom original = Atom.parse("likes(alice,bob)");
        int[] encoded = encoder.encode(original);
        Atom decoded = encoder.decode(encoded);

        assertEquals(original, decoded);
        assertEquals("likes", decoded.getPredicate());
        assertEquals(Arrays.asList("alice", "bob"), decoded.getArgs());
    }

    @Test
    void testDecodeNullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            encoder.decode(null);
        });
    }

    @Test
    void testDecodeEmptyArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            encoder.decode(new int[0]);
        });
    }

    @Test
    void testStringInterning() {
        Atom atom1 = Atom.parse("likes(alice,bob)");
        Atom atom2 = Atom.parse("likes(bob,charlie)");

        int[] enc1 = encoder.encode(atom1);
        int[] enc2 = encoder.encode(atom2);

        // "likes" and "bob" should have same IDs
        assertEquals(enc1[0], enc2[0]);  // same predicate "likes"
        assertEquals(enc1[2], enc2[1]);  // "bob" appears in both

        // Different arguments should have different IDs
        assertNotEquals(enc1[1], enc2[1]);  // alice != bob
        assertNotEquals(enc1[2], enc2[2]);  // bob != charlie
    }

    @Test
    void testEncodeDecodeRoundTrip() {
        List<Atom> atoms = Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("popular(bob)"),
            Atom.parse("friend(alice,charlie)"),
            Atom.parse("loves(bob,dancing)")
        );

        for (Atom original : atoms) {
            int[] encoded = encoder.encode(original);
            Atom decoded = encoder.decode(encoded);
            assertEquals(original, decoded, "Round trip failed for: " + original);
        }
    }

    @Test
    void testEncodeAll() {
        List<Atom> atoms = Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("popular(bob)"),
            Atom.parse("friend(alice,charlie)")
        );

        int[] encoded = encoder.encodeAll(atoms);

        assertNotNull(encoded);
        assertTrue(encoded.length > atoms.size());  // Should include size prefixes

        System.out.println("Encoded all: " + Arrays.toString(encoded));
    }

    @Test
    void testEncodeAllEmpty() {
        int[] encoded = encoder.encodeAll(Arrays.asList());
        assertNotNull(encoded);
        assertEquals(0, encoded.length);
    }

    @Test
    void testEncodeAllNull() {
        int[] encoded = encoder.encodeAll(null);
        assertNotNull(encoded);
        assertEquals(0, encoded.length);
    }

    @Test
    void testDecodeAll() {
        List<Atom> original = Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("popular(bob)"),
            Atom.parse("friend(alice,charlie)")
        );

        int[] encoded = encoder.encodeAll(original);
        List<Atom> decoded = encoder.decodeAll(encoded);

        assertEquals(original, decoded);
    }

    @Test
    void testDecodeAllEmpty() {
        List<Atom> decoded = encoder.decodeAll(new int[0]);
        assertNotNull(decoded);
        assertTrue(decoded.isEmpty());
    }

    @Test
    void testGetId() {
        Atom atom = Atom.parse("likes(alice,bob)");
        encoder.encode(atom);

        // Should find interned strings
        assertTrue(encoder.getId("likes") > 0);
        assertTrue(encoder.getId("alice") > 0);
        assertTrue(encoder.getId("bob") > 0);

        // Should return 0 for unknown strings
        assertEquals(0, encoder.getId("unknown"));
    }

    @Test
    void testGetString() {
        Atom atom = Atom.parse("likes(alice,bob)");
        int[] encoded = encoder.encode(atom);

        // Should find strings by ID
        assertEquals("likes", encoder.getString(encoded[0]));
        assertEquals("alice", encoder.getString(encoded[1]));
        assertEquals("bob", encoder.getString(encoded[2]));

        // Should return null for unknown IDs
        assertNull(encoder.getString(999999));
    }

    @Test
    void testHasString() {
        Atom atom = Atom.parse("likes(alice,bob)");
        encoder.encode(atom);

        assertTrue(encoder.hasString("likes"));
        assertTrue(encoder.hasString("alice"));
        assertTrue(encoder.hasString("bob"));
        assertFalse(encoder.hasString("unknown"));
    }

    @Test
    void testHasId() {
        Atom atom = Atom.parse("likes(alice,bob)");
        int[] encoded = encoder.encode(atom);

        assertTrue(encoder.hasId(encoded[0]));
        assertTrue(encoder.hasId(encoded[1]));
        assertTrue(encoder.hasId(encoded[2]));
        assertFalse(encoder.hasId(999999));
    }

    @Test
    void testGetStringCount() {
        assertEquals(0, encoder.getStringCount());

        encoder.encode(Atom.parse("likes(alice,bob)"));
        assertEquals(3, encoder.getStringCount());  // likes, alice, bob

        encoder.encode(Atom.parse("likes(bob,charlie)"));
        assertEquals(4, encoder.getStringCount());  // + charlie (likes and bob reused)
    }

    @Test
    void testReset() {
        encoder.encode(Atom.parse("likes(alice,bob)"));
        assertEquals(3, encoder.getStringCount());

        encoder.reset();
        assertEquals(0, encoder.getStringCount());
        assertFalse(encoder.hasString("likes"));
    }

    @Test
    void testGetStats() {
        encoder.encode(Atom.parse("likes(alice,bob)"));
        encoder.encode(Atom.parse("popular(bob)"));
        encoder.decode(encoder.encode(Atom.parse("friend(alice,charlie)")));

        FactEncoder.EncoderStats stats = encoder.getStats();

        assertEquals(5, stats.uniqueStrings);  // likes, alice, bob, popular, friend, charlie
        assertEquals(3, stats.atomsEncoded);
        assertEquals(1, stats.atomsDecoded);
        assertTrue(stats.maxIdUsed > 0);

        System.out.println("Stats: " + stats);
    }

    @Test
    void testMemoryEstimate() {
        encoder.encode(Atom.parse("likes(alice,bob)"));

        long memory = encoder.estimateMemoryUsage();
        assertTrue(memory > 0);

        System.out.println("Estimated memory: " + memory + " bytes");
    }

    @Test
    void testGetAllStrings() {
        encoder.encode(Atom.parse("likes(alice,bob)"));

        var allStrings = encoder.getAllStrings();
        assertEquals(3, allStrings.size());
        assertTrue(allStrings.containsKey("likes"));
        assertTrue(allStrings.containsKey("alice"));
        assertTrue(allStrings.containsKey("bob"));
    }

    @Test
    void testToString() {
        encoder.encode(Atom.parse("likes(alice,bob)"));

        String str = encoder.toString();
        assertNotNull(str);
        assertTrue(str.contains("strings=3"));

        System.out.println("Encoder: " + str);
    }

    @Test
    void testLargeAtom() {
        // Atom with many arguments
        Atom atom = new Atom("manyArgs", Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h"));
        int[] encoded = encoder.encode(atom);

        assertEquals(9, encoded.length);  // predicate + 8 args

        Atom decoded = encoder.decode(encoded);
        assertEquals(atom, decoded);
    }

    @Test
    void testVariableNames() {
        // Test with variable names (uppercase)
        Atom atom = Atom.parse("likes(X,Y)");
        int[] encoded = encoder.encode(atom);
        Atom decoded = encoder.decode(encoded);

        assertEquals(atom, decoded);
        assertEquals(Arrays.asList("X", "Y"), decoded.getArgs());
    }

    @Test
    void testSpecialCharacters() {
        // Test with special characters in arguments
        Atom atom = new Atom("test", Arrays.asList("alice_123", "bob-456", "charlie.789"));
        int[] encoded = encoder.encode(atom);
        Atom decoded = encoder.decode(encoded);

        assertEquals(atom, decoded);
    }

    @Test
    void testConsistentEncoding() {
        // Same atom should always encode to same values
        Atom atom = Atom.parse("likes(alice,bob)");

        int[] enc1 = encoder.encode(atom);
        int[] enc2 = encoder.encode(atom);

        assertArrayEquals(enc1, enc2);
    }

    @Test
    void testMultipleEncoders() {
        // Different encoders should be independent
        FactEncoder encoder1 = new FactEncoder();
        FactEncoder encoder2 = new FactEncoder();

        Atom atom = Atom.parse("likes(alice,bob)");

        int[] enc1 = encoder1.encode(atom);
        int[] enc2 = encoder2.encode(atom);

        // IDs might be same or different, but decoding should work
        Atom dec1 = encoder1.decode(enc1);
        Atom dec2 = encoder2.decode(enc2);

        assertEquals(atom, dec1);
        assertEquals(atom, dec2);
    }
}
