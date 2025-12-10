package com.example;

/**
 * A rule with an associated probability (confidence).
 *
 * <p>Probabilistic rules represent uncertain inference. The probability indicates
 * how confident we are in the rule's conclusion given its conditions.</p>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * // 90% confidence: if supplier is disrupted and they supply a part, the part is at risk
 * ProbabilisticRule rule = new ProbabilisticRule(
 *     "atRisk(x) <-1 disrupted(y), supplies(y,x)",
 *     "supply_chain_risk",
 *     0.9  // 90% confidence
 * );
 * }</pre>
 *
 * <h2>Probability Combination:</h2>
 * <p>When deriving facts, probabilities are combined using multiplication:</p>
 * <pre>
 * P(conclusion) = P(rule) × P(premise1) × P(premise2) × ...
 * </pre>
 *
 * <p>Example:</p>
 * <pre>
 * Fact: disrupted(ACME) with p=0.7
 * Fact: supplies(ACME, ENGINE) with p=1.0
 * Rule: atRisk(x) <- disrupted(y), supplies(y,x) with p=0.9
 *
 * Derived: atRisk(ENGINE) with p = 0.9 × 0.7 × 1.0 = 0.63
 * </pre>
 */
public class ProbabilisticRule extends Rule {
    private final double probability;  // Rule confidence (0.0 to 1.0)

    /**
     * Constructs a new ProbabilisticRule.
     *
     * @param ruleString the rule in string format
     * @param name rule name
     * @param probability rule confidence (0.0 = never applies, 1.0 = always applies)
     */
    public ProbabilisticRule(String ruleString, String name, double probability) {
        super(ruleString, name);

        if (probability < 0.0 || probability > 1.0) {
            throw new IllegalArgumentException("Probability must be between 0.0 and 1.0, got: " + probability);
        }

        this.probability = probability;
    }

    /**
     * Gets the probability (confidence) of this rule.
     *
     * @return probability value between 0.0 and 1.0
     */
    public double getProbability() {
        return probability;
    }

    /**
     * Checks if this is a certain rule (probability = 1.0).
     */
    public boolean isCertain() {
        return probability == 1.0;
    }

    /**
     * Checks if this is an uncertain rule (probability < 1.0).
     */
    public boolean isUncertain() {
        return probability < 1.0;
    }

    @Override
    public String toString() {
        return String.format("ProbabilisticRule{%s, p=%.2f}", getRaw(), probability);
    }
}
