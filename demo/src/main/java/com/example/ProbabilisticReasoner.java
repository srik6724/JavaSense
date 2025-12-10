package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Probabilistic Reasoner - Reasoning with Uncertainty
 *
 * <p>Extends standard reasoning with probability tracking. Each derived fact
 * gets a probability based on the probabilities of the facts and rules used to derive it.</p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li><b>Probabilistic facts:</b> Facts with certainty values (0.0 to 1.0)</li>
 *   <li><b>Probabilistic rules:</b> Rules with confidence values (0.0 to 1.0)</li>
 *   <li><b>Probability propagation:</b> Combine probabilities during inference</li>
 *   <li><b>Threshold filtering:</b> Only keep facts above a probability threshold</li>
 * </ul>
 *
 * <h2>Probability Combination:</h2>
 * <pre>
 * P(derived fact) = P(rule) × P(body fact 1) × P(body fact 2) × ...
 * </pre>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * ProbabilisticReasoner reasoner = new ProbabilisticReasoner();
 *
 * // Add uncertain fact
 * reasoner.addProbabilisticFact(new ProbabilisticFact(
 *     Atom.parse("disrupted(ACME)"),
 *     "alert1",
 *     0.7,  // 70% sure
 *     List.of(new Interval(0, 10))
 * ));
 *
 * // Add probabilistic rule
 * reasoner.addProbabilisticRule(new ProbabilisticRule(
 *     "atRisk(x) <-1 disrupted(y), supplies(y,x)",
 *     "risk_rule",
 *     0.9  // 90% confidence
 * ));
 *
 * // Reason
 * ProbabilisticInterpretation result = reasoner.reason(10);
 *
 * // Query with probability
 * double prob = result.getProbability(Atom.parse("atRisk(ENGINE)"), 1);
 * System.out.println("P(atRisk(ENGINE)) = " + prob);  // 0.63 = 0.7 × 0.9
 * }</pre>
 *
 * <h2>Use Cases:</h2>
 * <ul>
 *   <li>Risk assessment under uncertainty</li>
 *   <li>Medical diagnosis (symptoms → diseases with probabilities)</li>
 *   <li>Fraud detection with confidence scores</li>
 *   <li>Sensor fusion (combine noisy sensor data)</li>
 *   <li>Machine learning integration</li>
 * </ul>
 */
public class ProbabilisticReasoner {
    private static final Logger logger = LoggerFactory.getLogger(ProbabilisticReasoner.class);

    private final List<Rule> rules = new ArrayList<>();
    private final List<ProbabilisticRule> probabilisticRules = new ArrayList<>();
    private final List<TimedFact> baseFacts = new ArrayList<>();
    private final List<ProbabilisticFact> probabilisticFacts = new ArrayList<>();

    // Track probabilities for each (atom, time)
    private final Map<Provenance.AtomTimeKey, Double> probabilities = new ConcurrentHashMap<>();

    private double minProbabilityThreshold = 0.0;  // Filter facts below this threshold

    /**
     * Adds a standard (certain) rule.
     */
    public void addRule(Rule rule) {
        rules.add(rule);
    }

    /**
     * Adds a probabilistic rule.
     */
    public void addProbabilisticRule(ProbabilisticRule rule) {
        probabilisticRules.add(rule);
        rules.add(rule);  // Also add to standard rules list
    }

    /**
     * Adds a standard (certain) fact.
     */
    public void addFact(TimedFact fact) {
        baseFacts.add(fact);
    }

    /**
     * Adds a probabilistic fact.
     */
    public void addProbabilisticFact(ProbabilisticFact fact) {
        probabilisticFacts.add(fact);
        baseFacts.add(fact);  // Also add to standard facts list

        // Record initial probability
        for (Interval iv : fact.getIntervals()) {
            for (int t = iv.getStart(); t <= iv.getEnd(); t++) {
                probabilities.put(new Provenance.AtomTimeKey(fact.getAtom(), t), fact.getProbability());
            }
        }
    }

    /**
     * Sets the minimum probability threshold for facts.
     * Facts with probability below this threshold will be filtered out.
     *
     * @param threshold minimum probability (0.0 to 1.0)
     */
    public void setMinProbabilityThreshold(double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
        }
        this.minProbabilityThreshold = threshold;
        logger.info("Set minimum probability threshold to {}", threshold);
    }

    /**
     * Performs probabilistic reasoning.
     *
     * @param timesteps maximum timestep
     * @return probabilistic interpretation with probabilities for each fact
     */
    public ProbabilisticInterpretation reason(int timesteps) {
        logger.info("Starting probabilistic reasoning for {} timesteps", timesteps);

        // Use OptimizedReasoner for the core reasoning
        OptimizedReasoner coreReasoner = new OptimizedReasoner();

        // Add all rules (both certain and probabilistic)
        for (Rule rule : rules) {
            coreReasoner.addRule(rule);
        }

        // Add all facts (both certain and probabilistic)
        for (TimedFact fact : baseFacts) {
            coreReasoner.addFact(fact);
        }

        // Perform reasoning
        ReasoningInterpretation coreResult = coreReasoner.reason(timesteps);

        // Now compute probabilities for all derived facts
        computeProbabilities(coreResult, timesteps);

        // Filter facts below threshold
        List<Set<Atom>> filteredFacts = filterByProbability(coreResult, timesteps);

        logger.info("Probabilistic reasoning complete. {} facts above threshold {}",
                probabilities.size(), minProbabilityThreshold);

        return new ProbabilisticInterpretation(filteredFacts, coreResult.getProvenance(), probabilities);
    }

    // --- Internal Methods ---

    private void computeProbabilities(ReasoningInterpretation interpretation, int timesteps) {
        // Initialize certain facts (not in probabilisticFacts) with probability 1.0
        for (TimedFact fact : baseFacts) {
            if (!(fact instanceof ProbabilisticFact)) {
                for (Interval iv : fact.getIntervals()) {
                    for (int t = Math.max(0, iv.getStart()); t <= Math.min(timesteps, iv.getEnd()); t++) {
                        probabilities.putIfAbsent(
                            new Provenance.AtomTimeKey(fact.getAtom(), t),
                            1.0
                        );
                    }
                }
            }
        }

        // Compute probabilities for derived facts using provenance
        Provenance prov = interpretation.getProvenance();

        // Iterate through all facts at all times
        for (int t = 0; t <= timesteps; t++) {
            Set<Atom> factsAtTime = interpretation.getFactsAt(t);
            for (Atom atom : factsAtTime) {
                Provenance.AtomTimeKey key = new Provenance.AtomTimeKey(atom, t);

                if (!probabilities.containsKey(key)) {
                    // This fact was derived - compute its probability
                    double prob = computeFactProbability(atom, t, prov);
                    probabilities.put(key, prob);
                }
            }
        }
    }

    private double computeFactProbability(Atom atom, int time, Provenance prov) {
        DerivationInfo info = prov.getDerivation(atom, time);

        if (info == null) {
            // Base fact - should already have probability
            return probabilities.getOrDefault(new Provenance.AtomTimeKey(atom, time), 1.0);
        }

        // Get rule probability
        double ruleProbability = getRuleProbability(info.getRuleName());

        // Get probabilities of source facts
        double sourceProbability = 1.0;
        for (Provenance.AtomTimeKey source : info.getSourceFacts()) {
            // Recursively compute source probability
            if (!probabilities.containsKey(source)) {
                double sourceProb = computeFactProbability(source.getAtom(), source.getTime(), prov);
                probabilities.put(source, sourceProb);
            }
            sourceProbability *= probabilities.get(source);
        }

        // Combined probability: P(rule) × P(source1) × P(source2) × ...
        return ruleProbability * sourceProbability;
    }

    private double getRuleProbability(String ruleName) {
        for (ProbabilisticRule rule : probabilisticRules) {
            if (rule.getName().equals(ruleName)) {
                return rule.getProbability();
            }
        }
        // Not a probabilistic rule - assume certain
        return 1.0;
    }

    private List<Set<Atom>> filterByProbability(ReasoningInterpretation interpretation, int timesteps) {
        List<Set<Atom>> filtered = new ArrayList<>();

        for (int t = 0; t <= timesteps; t++) {
            Set<Atom> filteredAtTime = new HashSet<>();
            Set<Atom> factsAtTime = interpretation.getFactsAt(t);

            for (Atom atom : factsAtTime) {
                Provenance.AtomTimeKey key = new Provenance.AtomTimeKey(atom, t);
                double prob = probabilities.getOrDefault(key, 1.0);

                if (prob >= minProbabilityThreshold) {
                    filteredAtTime.add(atom);
                }
            }

            filtered.add(filteredAtTime);
        }

        return filtered;
    }

    /**
     * Result of probabilistic reasoning with probabilities for each fact.
     */
    public static class ProbabilisticInterpretation extends ReasoningInterpretation {
        private final Map<Provenance.AtomTimeKey, Double> probabilities;

        public ProbabilisticInterpretation(List<Set<Atom>> factsAtTime,
                                          Provenance provenance,
                                          Map<Provenance.AtomTimeKey, Double> probabilities) {
            super(factsAtTime, provenance);
            this.probabilities = new HashMap<>(probabilities);
        }

        /**
         * Gets the probability of a fact at a specific time.
         *
         * @param atom the fact
         * @param time the timestep
         * @return probability (0.0 to 1.0), or 0.0 if fact doesn't exist
         */
        public double getProbability(Atom atom, int time) {
            return probabilities.getOrDefault(new Provenance.AtomTimeKey(atom, time), 0.0);
        }

        /**
         * Gets all probabilities.
         *
         * @return map of (atom, time) → probability
         */
        public Map<Provenance.AtomTimeKey, Double> getAllProbabilities() {
            return Collections.unmodifiableMap(probabilities);
        }

        /**
         * Gets facts above a probability threshold at a specific time.
         *
         * @param threshold minimum probability
         * @param time the timestep
         * @return set of atoms with probability >= threshold
         */
        public Set<Atom> getFactsAboveThreshold(double threshold, int time) {
            Set<Atom> result = new HashSet<>();
            Set<Atom> factsAtTime = getFactsAt(time);

            for (Atom atom : factsAtTime) {
                if (getProbability(atom, time) >= threshold) {
                    result.add(atom);
                }
            }

            return result;
        }
    }
}
