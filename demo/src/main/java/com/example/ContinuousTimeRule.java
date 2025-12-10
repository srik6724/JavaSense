package com.example;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A rule for continuous time reasoning with duration-based delays.
 *
 * <p>Unlike {@link Rule} which uses discrete timesteps, ContinuousTimeRule uses
 * actual {@link Duration} values for temporal delays.</p>
 *
 * <h2>Syntax:</h2>
 * <pre>
 * head <-delay body1, body2, ...
 * </pre>
 *
 * <h2>Examples:</h2>
 * <pre>{@code
 * // Alert if temperature high for more than 1 hour
 * ContinuousTimeRule rule = new ContinuousTimeRule(
 *     "alert(sensor) <-1h highTemp(sensor)",
 *     "sustained_high_temp",
 *     Duration.ofHours(1)
 * );
 *
 * // Fraud if large transaction within 5 minutes of account creation
 * ContinuousTimeRule rule = new ContinuousTimeRule(
 *     "fraudRisk(acct) <-5m newAccount(acct), largeTransaction(acct)",
 *     "quick_large_transaction",
 *     Duration.ofMinutes(5)
 * );
 *
 * // Instantaneous rule (no delay)
 * ContinuousTimeRule rule = new ContinuousTimeRule(
 *     "highTemp(x) <-0s temperature(x,t), t > 100",
 *     "high_temp_threshold",
 *     Duration.ZERO
 * );
 * }</pre>
 *
 * <h2>Use Cases:</h2>
 * <ul>
 *   <li>Real-time event correlation with precise time windows</li>
 *   <li>IoT sensor monitoring with duration-based rules</li>
 *   <li>Financial fraud detection (e.g., "within 1 minute")</li>
 *   <li>Process monitoring with SLA time constraints</li>
 * </ul>
 */
public class ContinuousTimeRule {
    private final Atom head;
    private final List<Literal> body;
    private final Duration delay;
    private final String name;

    /**
     * Constructs a continuous time rule from a string.
     *
     * <p>Format: "head <-duration body1, body2, ..."</p>
     * <p>Duration format: number + unit (s=seconds, m=minutes, h=hours, d=days)</p>
     * <p>Examples:</p>
     * <ul>
     *   <li>"alert(x) <-1h highTemp(x)" - 1 hour delay</li>
     *   <li>"fraud(x) <-5m newAccount(x), largeTransaction(x)" - 5 minute delay</li>
     *   <li>"immediate(x) <-0s sensor(x)" - no delay</li>
     * </ul>
     *
     * @param ruleString the rule string
     * @param name unique rule name
     * @param delay temporal delay (lookback duration)
     */
    public ContinuousTimeRule(String ruleString, String name, Duration delay) {
        if (ruleString == null || ruleString.isEmpty()) {
            throw new IllegalArgumentException("Rule string cannot be null or empty");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (delay == null || delay.isNegative()) {
            throw new IllegalArgumentException("Delay must be non-negative");
        }

        this.name = name;
        this.delay = delay;

        // Parse rule: "head <-delay body1, body2, ..."
        String[] parts = ruleString.split("<-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid rule format. Expected: 'head <-delay body'");
        }

        String headStr = parts[0].trim();
        String bodyStr = parts[1].trim();

        // Remove duration annotation from body if present
        // e.g., "1h highTemp(x)" -> "highTemp(x)"
        bodyStr = bodyStr.replaceFirst("^\\d+[smhd]\\s+", "");

        // Parse head
        this.head = Atom.parse(headStr);

        // Parse body
        this.body = new ArrayList<>();
        if (!bodyStr.isEmpty()) {
            // Split by comma but not inside parentheses
            List<String> bodyParts = smartSplit(bodyStr);
            for (String part : bodyParts) {
                part = part.trim();
                if (!part.isEmpty()) {
                    // Check for negation
                    if (part.startsWith("!")) {
                        Atom atom = Atom.parse(part.substring(1).trim());
                        this.body.add(new Literal(atom, true));
                    } else {
                        Atom atom = Atom.parse(part);
                        this.body.add(new Literal(atom, false));
                    }
                }
            }
        }
    }

    /**
     * Creates a rule with no delay (instantaneous).
     */
    public static ContinuousTimeRule instantaneous(String ruleString, String name) {
        return new ContinuousTimeRule(ruleString, name, Duration.ZERO);
    }

    /**
     * Creates a rule with a delay specified in seconds.
     */
    public static ContinuousTimeRule withSeconds(String ruleString, String name, long seconds) {
        return new ContinuousTimeRule(ruleString, name, Duration.ofSeconds(seconds));
    }

    /**
     * Creates a rule with a delay specified in minutes.
     */
    public static ContinuousTimeRule withMinutes(String ruleString, String name, long minutes) {
        return new ContinuousTimeRule(ruleString, name, Duration.ofMinutes(minutes));
    }

    /**
     * Creates a rule with a delay specified in hours.
     */
    public static ContinuousTimeRule withHours(String ruleString, String name, long hours) {
        return new ContinuousTimeRule(ruleString, name, Duration.ofHours(hours));
    }

    public Atom getHead() {
        return head;
    }

    public List<Literal> getBody() {
        return Collections.unmodifiableList(body);
    }

    public Duration getDelay() {
        return delay;
    }

    public String getName() {
        return name;
    }

    /**
     * Checks if this is an instantaneous rule (no delay).
     */
    public boolean isInstantaneous() {
        return delay.isZero();
    }

    /**
     * Gets the delay in milliseconds.
     */
    public long getDelayMillis() {
        return delay.toMillis();
    }

    /**
     * Gets the delay in seconds.
     */
    public long getDelaySeconds() {
        return delay.getSeconds();
    }

    /**
     * Helper method to split by comma but respect parentheses.
     * E.g., "temperature(sensor,temp), highTemp(x)" -> ["temperature(sensor,temp)", "highTemp(x)"]
     */
    private static List<String> smartSplit(String str) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;

        for (char c : str.toCharArray()) {
            if (c == '(') {
                depth++;
                current.append(c);
            } else if (c == ')') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                // Split point
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        // Add last part
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(head.toString());
        sb.append(" <-");

        // Add duration annotation
        if (!delay.isZero()) {
            long seconds = delay.getSeconds();
            if (seconds >= 86400 && seconds % 86400 == 0) {
                sb.append(seconds / 86400).append("d");
            } else if (seconds >= 3600 && seconds % 3600 == 0) {
                sb.append(seconds / 3600).append("h");
            } else if (seconds >= 60 && seconds % 60 == 0) {
                sb.append(seconds / 60).append("m");
            } else {
                sb.append(seconds).append("s");
            }
            sb.append(" ");
        }

        // Add body
        if (body.isEmpty()) {
            sb.append(" true");
        } else {
            for (int i = 0; i < body.size(); i++) {
                if (i > 0) sb.append(", ");
                Literal lit = body.get(i);
                if (lit.isNegated()) {
                    sb.append("!");
                }
                sb.append(lit.getAtom().toString());
            }
        }

        sb.append(" [").append(name).append("]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContinuousTimeRule that = (ContinuousTimeRule) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
