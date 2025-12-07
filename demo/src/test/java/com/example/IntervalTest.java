package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Interval class.
 */
class IntervalTest {

    @Test
    @DisplayName("Should create interval with start and end")
    void testIntervalCreation() {
        Interval interval = new Interval(0, 5);

        assertEquals(0, interval.getStart());
        assertEquals(5, interval.getEnd());
    }

    @Test
    @DisplayName("Should check if timestep is within interval")
    void testContains() {
        Interval interval = new Interval(3, 7);

        assertTrue(interval.contains(3));  // Start inclusive
        assertTrue(interval.contains(5));  // Middle
        assertTrue(interval.contains(7));  // End inclusive
        assertFalse(interval.contains(2)); // Before
        assertFalse(interval.contains(8)); // After
    }

    @Test
    @DisplayName("Should handle single-timestep intervals")
    void testSingleTimestep() {
        Interval interval = new Interval(5, 5);

        assertTrue(interval.contains(5));
        assertFalse(interval.contains(4));
        assertFalse(interval.contains(6));
    }

    @Test
    @DisplayName("Should handle zero-start intervals")
    void testZeroStart() {
        Interval interval = new Interval(0, 3);

        assertTrue(interval.contains(0));
        assertTrue(interval.contains(1));
        assertTrue(interval.contains(3));
        assertFalse(interval.contains(4));
    }

    @Test
    @DisplayName("Should provide string representation")
    void testToString() {
        Interval interval = new Interval(2, 8);
        String str = interval.toString();

        assertNotNull(str);
        assertTrue(str.contains("2") || str.contains("8"));
    }

    @Test
    @DisplayName("Should calculate interval length")
    void testIntervalLength() {
        Interval interval = new Interval(3, 7);

        // Length should be end - start + 1 (inclusive)
        int expectedLength = 7 - 3 + 1;
        assertEquals(expectedLength, interval.getEnd() - interval.getStart() + 1);
    }
}
