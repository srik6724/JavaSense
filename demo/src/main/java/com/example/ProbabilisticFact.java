package com.example;

import java.util.List;

/**
 * A fact with an associated probability (certainty).
 *
 * <p>Probabilistic facts represent uncertain knowledge. The probability indicates
 * how confident we are that the fact is true.</p>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * // 70% sure the supplier is disrupted
 * ProbabilisticFact fact = new ProbabilisticFact(
 *     Atom.parse("disrupted(ACME)"),
 *     "supplier_alert",
 *     0.7,
 *     List.of(new Interval(0, 100))
 * );
 * }</pre>
 */
public class ProbabilisticFact extends TimedFact {
    private final double probability;  // 0.0 to 1.0

    /**
     * Constructs a new ProbabilisticFact.
     *
     * @param atom the fact
     * @param id unique identifier
     * @param probability certainty (0.0 = impossible, 1.0 = certain)
     * @param intervals temporal intervals when this fact holds
     */
    public ProbabilisticFact(Atom atom, String id, double probability, List<Interval> intervals) {
        super(atom, id, intervals);

        if (probability < 0.0 || probability > 1.0) {
            throw new IllegalArgumentException("Probability must be between 0.0 and 1.0, got: " + probability);
        }

        this.probability = probability;
    }

    /**
     * Gets the probability of this fact.
     *
     * @return probability value between 0.0 and 1.0
     */
    public double getProbability() {
        return probability;
    }

    /**
     * Checks if this is a certain fact (probability = 1.0).
     */
    public boolean isCertain() {
        return probability == 1.0;
    }

    /**
     * Checks if this is an uncertain fact (probability < 1.0).
     */
    public boolean isUncertain() {
        return probability < 1.0;
    }

    @Override
    public String toString() {
        return String.format("ProbabilisticFact{%s, p=%.2f, intervals=%s}",
                getAtom(), probability, getIntervals());
    }
}
