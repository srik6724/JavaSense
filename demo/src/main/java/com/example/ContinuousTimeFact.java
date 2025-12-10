package com.example;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A fact with continuous time intervals (real timestamps).
 *
 * <p>Unlike {@link TimedFact} which uses discrete timesteps, ContinuousTimeFact
 * uses actual {@link Instant} timestamps for precise temporal reasoning over
 * real-world time series data.</p>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * // Temperature reading: sensor1 reads 75°F from 9:00 AM to 5:00 PM
 * ContinuousTimeFact fact = new ContinuousTimeFact(
 *     Atom.parse("temperature(sensor1,75)"),
 *     "reading_001",
 *     List.of(TimeInterval.between(
 *         Instant.parse("2025-01-01T09:00:00Z"),
 *         Instant.parse("2025-01-01T17:00:00Z")
 *     ))
 * );
 * }</pre>
 *
 * <h2>Use Cases:</h2>
 * <ul>
 *   <li>IoT sensor data (temperature, pressure, etc.)</li>
 *   <li>Financial markets (continuous trading)</li>
 *   <li>Event logs with precise timestamps</li>
 *   <li>Process monitoring</li>
 * </ul>
 */
public class ContinuousTimeFact {
    private final Atom atom;
    private final String id;
    private final List<TimeInterval> intervals;

    /**
     * Constructs a new ContinuousTimeFact.
     *
     * @param atom the fact
     * @param id unique identifier
     * @param intervals temporal intervals when this fact holds
     */
    public ContinuousTimeFact(Atom atom, String id, List<TimeInterval> intervals) {
        if (atom == null) throw new NullPointerException("Atom cannot be null");
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("ID cannot be null or empty");
        if (intervals == null || intervals.isEmpty()) {
            throw new IllegalArgumentException("Intervals cannot be null or empty");
        }

        this.atom = atom;
        this.id = id;
        this.intervals = new ArrayList<>(intervals);
    }

    /**
     * Creates a fact that holds at a single instant.
     */
    public static ContinuousTimeFact at(Atom atom, String id, Instant instant) {
        return new ContinuousTimeFact(atom, id, List.of(TimeInterval.at(instant)));
    }

    /**
     * Creates a fact that holds during a time interval.
     */
    public static ContinuousTimeFact during(Atom atom, String id, Instant start, Instant end) {
        return new ContinuousTimeFact(atom, id, List.of(TimeInterval.between(start, end)));
    }

    public Atom getAtom() {
        return atom;
    }

    public String getId() {
        return id;
    }

    public List<TimeInterval> getIntervals() {
        return Collections.unmodifiableList(intervals);
    }

    /**
     * Checks if this fact holds at a specific instant.
     */
    public boolean holdsAt(Instant instant) {
        return intervals.stream().anyMatch(iv -> iv.contains(instant));
    }

    /**
     * Checks if this fact holds during any part of a time interval.
     */
    public boolean holdsDuring(TimeInterval interval) {
        return intervals.stream().anyMatch(iv -> iv.overlaps(interval));
    }

    @Override
    public String toString() {
        return String.format("ContinuousTimeFact{%s, id='%s', intervals=%s}",
                atom, id, intervals);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContinuousTimeFact that = (ContinuousTimeFact) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
