package com.example;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Continuous time interval with real timestamps.
 *
 * <p>Unlike {@link Interval} which uses discrete timesteps, TimeInterval uses
 * actual {@link Instant} timestamps for precise temporal reasoning.</p>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * // Temperature reading from 9:00 AM to 5:00 PM on Jan 1, 2025
 * TimeInterval interval = TimeInterval.between(
 *     Instant.parse("2025-01-01T09:00:00Z"),
 *     Instant.parse("2025-01-01T17:00:00Z")
 * );
 *
 * // Or use convenience methods
 * TimeInterval today = TimeInterval.today();
 * TimeInterval lastHour = TimeInterval.lastHours(1);
 * }</pre>
 */
public class TimeInterval {
    private final Instant start;
    private final Instant end;

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    /**
     * Constructs a new TimeInterval.
     *
     * @param start start timestamp (inclusive)
     * @param end end timestamp (inclusive)
     */
    public TimeInterval(Instant start, Instant end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start must be before or equal to end");
        }
        this.start = start;
        this.end = end;
    }

    /**
     * Creates a TimeInterval between two timestamps.
     */
    public static TimeInterval between(Instant start, Instant end) {
        return new TimeInterval(start, end);
    }

    /**
     * Creates a TimeInterval for a single instant (point in time).
     */
    public static TimeInterval at(Instant instant) {
        return new TimeInterval(instant, instant);
    }

    /**
     * Creates a TimeInterval from start for a given duration.
     */
    public static TimeInterval from(Instant start, Duration duration) {
        return new TimeInterval(start, start.plus(duration));
    }

    /**
     * Creates a TimeInterval for the last N hours from now.
     */
    public static TimeInterval lastHours(long hours) {
        Instant now = Instant.now();
        return new TimeInterval(now.minus(Duration.ofHours(hours)), now);
    }

    /**
     * Creates a TimeInterval for the last N minutes from now.
     */
    public static TimeInterval lastMinutes(long minutes) {
        Instant now = Instant.now();
        return new TimeInterval(now.minus(Duration.ofMinutes(minutes)), now);
    }

    /**
     * Creates a TimeInterval for today (midnight to now).
     */
    public static TimeInterval today() {
        Instant now = Instant.now();
        Instant midnight = now.atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant();
        return new TimeInterval(midnight, now);
    }

    /**
     * Gets the start timestamp.
     */
    public Instant getStart() {
        return start;
    }

    /**
     * Gets the end timestamp.
     */
    public Instant getEnd() {
        return end;
    }

    /**
     * Gets the duration of this interval.
     */
    public Duration getDuration() {
        return Duration.between(start, end);
    }

    /**
     * Checks if this interval contains a specific instant.
     */
    public boolean contains(Instant instant) {
        return !instant.isBefore(start) && !instant.isAfter(end);
    }

    /**
     * Checks if this interval overlaps with another interval.
     */
    public boolean overlaps(TimeInterval other) {
        return !this.end.isBefore(other.start) && !other.end.isBefore(this.start);
    }

    /**
     * Checks if this interval is before another interval.
     */
    public boolean isBefore(TimeInterval other) {
        return this.end.isBefore(other.start);
    }

    /**
     * Checks if this interval is after another interval.
     */
    public boolean isAfter(TimeInterval other) {
        return this.start.isAfter(other.end);
    }

    /**
     * Returns the intersection of this interval with another (null if no overlap).
     */
    public TimeInterval intersection(TimeInterval other) {
        if (!overlaps(other)) {
            return null;
        }

        Instant newStart = start.isAfter(other.start) ? start : other.start;
        Instant newEnd = end.isBefore(other.end) ? end : other.end;

        return new TimeInterval(newStart, newEnd);
    }

    @Override
    public String toString() {
        return String.format("[%s, %s]",
            FORMATTER.format(start),
            FORMATTER.format(end));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeInterval that = (TimeInterval) o;
        return start.equals(that.start) && end.equals(that.end);
    }

    @Override
    public int hashCode() {
        return 31 * start.hashCode() + end.hashCode();
    }
}
