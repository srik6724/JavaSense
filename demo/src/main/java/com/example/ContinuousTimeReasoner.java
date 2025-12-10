package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Continuous Time Reasoner - Reasoning over real timestamps
 *
 * <p>Unlike {@link OptimizedReasoner} which uses discrete timesteps, ContinuousTimeReasoner
 * works with actual {@link Instant} timestamps for precise temporal reasoning.</p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li><b>Real timestamps:</b> Use Instant instead of integer timesteps</li>
 *   <li><b>Time intervals:</b> Facts hold during continuous intervals</li>
 *   <li><b>Temporal queries:</b> Query at any instant or during any interval</li>
 *   <li><b>Duration-based rules:</b> Rules with real-time delays (e.g., "within 1 hour")</li>
 * </ul>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * ContinuousTimeReasoner reasoner = new ContinuousTimeReasoner();
 *
 * // Add fact with timestamp
 * reasoner.addFact(ContinuousTimeFact.at(
 *     Atom.parse("temperature(sensor1,75)"),
 *     "reading_001",
 *     Instant.parse("2025-01-01T09:00:00Z")
 * ));
 *
 * // Add duration-based rule
 * reasoner.addRule(new ContinuousTimeRule(
 *     "alert(sensor) <-1h temperature(sensor,temp), highTemp(temp)",
 *     "high_temp_alert",
 *     Duration.ofHours(1)  // Look back 1 hour
 * ));
 *
 * // Query at specific time
 * Set<Atom> facts = reasoner.queryAt(Instant.parse("2025-01-01T10:00:00Z"));
 * }</pre>
 *
 * <h2>Use Cases:</h2>
 * <ul>
 *   <li>IoT sensor data with precise timestamps</li>
 *   <li>Financial markets (millisecond precision trading)</li>
 *   <li>Event stream processing</li>
 *   <li>Process monitoring with real-world time</li>
 *   <li>Medical records with exact times</li>
 * </ul>
 */
public class ContinuousTimeReasoner {
    private static final Logger logger = LoggerFactory.getLogger(ContinuousTimeReasoner.class);

    private final List<ContinuousTimeFact> facts = new ArrayList<>();
    private final List<ContinuousTimeRule> rules = new ArrayList<>();

    // Index: predicate -> facts with that predicate
    private final Map<String, List<ContinuousTimeFact>> factIndex = new ConcurrentHashMap<>();

    // Cache for derived facts
    private final Map<TimeInterval, Set<Atom>> derivedFactsCache = new ConcurrentHashMap<>();

    private Instant minTime = null;
    private Instant maxTime = null;

    /**
     * Adds a continuous time fact.
     */
    public void addFact(ContinuousTimeFact fact) {
        facts.add(fact);

        // Update index
        String predicate = fact.getAtom().getPredicate();
        factIndex.computeIfAbsent(predicate, k -> new ArrayList<>()).add(fact);

        // Update time bounds
        for (TimeInterval interval : fact.getIntervals()) {
            updateTimeBounds(interval.getStart());
            updateTimeBounds(interval.getEnd());
        }

        // Clear cache
        derivedFactsCache.clear();

        logger.debug("Added fact: {}", fact);
    }

    /**
     * Adds a continuous time rule.
     */
    public void addRule(ContinuousTimeRule rule) {
        rules.add(rule);
        derivedFactsCache.clear();
        logger.debug("Added rule: {}", rule);
    }

    /**
     * Queries facts that hold at a specific instant.
     *
     * @param instant the time to query
     * @return set of atoms that hold at that instant
     */
    public Set<Atom> queryAt(Instant instant) {
        logger.debug("Querying facts at {}", instant);

        Set<Atom> result = new HashSet<>();

        // Add base facts
        for (ContinuousTimeFact fact : facts) {
            if (fact.holdsAt(instant)) {
                result.add(fact.getAtom());
            }
        }

        // Add derived facts
        result.addAll(deriveFactsAt(instant));

        logger.debug("Found {} facts at {}", result.size(), instant);
        return result;
    }

    /**
     * Queries facts that hold during any part of a time interval.
     *
     * @param interval the interval to query
     * @return set of atoms that hold during the interval
     */
    public Set<Atom> queryDuring(TimeInterval interval) {
        logger.debug("Querying facts during {}", interval);

        // Check cache
        if (derivedFactsCache.containsKey(interval)) {
            return new HashSet<>(derivedFactsCache.get(interval));
        }

        Set<Atom> result = new HashSet<>();

        // Add base facts
        for (ContinuousTimeFact fact : facts) {
            if (fact.holdsDuring(interval)) {
                result.add(fact.getAtom());
            }
        }

        // Add derived facts (sample interval at multiple points)
        List<Instant> samplePoints = sampleInterval(interval, 10);
        for (Instant instant : samplePoints) {
            result.addAll(deriveFactsAt(instant));
        }

        // Cache result
        derivedFactsCache.put(interval, result);

        logger.debug("Found {} facts during {}", result.size(), interval);
        return result;
    }

    /**
     * Performs forward-chaining reasoning over a time range.
     *
     * @param timeRange the range to reason over
     * @return interpretation with facts at sampled time points
     */
    public ContinuousTimeInterpretation reason(TimeInterval timeRange) {
        logger.info("Reasoning over time range: {}", timeRange);

        Map<Instant, Set<Atom>> factsAtTime = new HashMap<>();

        // Sample the time range
        List<Instant> samplePoints = sampleInterval(timeRange, 100);

        for (Instant instant : samplePoints) {
            Set<Atom> factsAtInstant = queryAt(instant);
            factsAtTime.put(instant, factsAtInstant);
        }

        logger.info("Reasoning complete. {} sample points processed", samplePoints.size());
        return new ContinuousTimeInterpretation(factsAtTime, timeRange);
    }

    /**
     * Performs reasoning over the entire time range of the facts.
     *
     * @return interpretation with facts over the full time range
     */
    public ContinuousTimeInterpretation reason() {
        if (minTime == null || maxTime == null) {
            throw new IllegalStateException("No facts loaded. Add facts before reasoning.");
        }

        TimeInterval fullRange = TimeInterval.between(minTime, maxTime);
        return reason(fullRange);
    }

    /**
     * Gets the time bounds of all facts.
     *
     * @return time interval covering all facts
     */
    public TimeInterval getTimeBounds() {
        if (minTime == null || maxTime == null) {
            throw new IllegalStateException("No facts loaded");
        }
        return TimeInterval.between(minTime, maxTime);
    }

    /**
     * Gets statistics about the reasoner.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("facts", facts.size());
        stats.put("rules", rules.size());
        stats.put("predicates", factIndex.size());
        stats.put("minTime", minTime);
        stats.put("maxTime", maxTime);
        stats.put("cacheSize", derivedFactsCache.size());
        return stats;
    }

    // --- Internal Methods ---

    private Set<Atom> deriveFactsAt(Instant instant) {
        Set<Atom> derived = new HashSet<>();

        for (ContinuousTimeRule rule : rules) {
            // Get current facts at this instant
            Set<Atom> currentFacts = new HashSet<>();
            for (ContinuousTimeFact fact : facts) {
                if (fact.holdsAt(instant)) {
                    currentFacts.add(fact.getAtom());
                }
            }

            // Apply rule with duration lookback
            Duration delay = rule.getDelay();
            Instant lookbackTime = instant.minus(delay);

            // Get historical facts in lookback window
            TimeInterval lookbackInterval = TimeInterval.between(lookbackTime, instant);
            Set<Atom> historicalFacts = new HashSet<>();
            for (ContinuousTimeFact fact : facts) {
                if (fact.holdsDuring(lookbackInterval)) {
                    historicalFacts.add(fact.getAtom());
                }
            }

            // Combine current and historical facts
            Set<Atom> allRelevantFacts = new HashSet<>();
            allRelevantFacts.addAll(currentFacts);
            allRelevantFacts.addAll(historicalFacts);

            // Match rule body against facts
            List<Map<String, String>> matches = matchRuleBody(rule, allRelevantFacts);

            // Generate derived facts
            for (Map<String, String> binding : matches) {
                Atom derivedAtom = instantiateHead(rule.getHead(), binding);
                derived.add(derivedAtom);
            }
        }

        return derived;
    }

    private List<Map<String, String>> matchRuleBody(ContinuousTimeRule rule, Set<Atom> facts) {
        List<Map<String, String>> results = new ArrayList<>();

        // Simple implementation: Match each body literal against facts
        List<Literal> bodyLiterals = rule.getBody();

        if (bodyLiterals.isEmpty()) {
            // No body - always matches
            results.add(new HashMap<>());
            return results;
        }

        // Start with first literal
        for (Atom fact : facts) {
            if (matches(bodyLiterals.get(0), fact)) {
                Map<String, String> binding = new HashMap<>();
                if (unify(bodyLiterals.get(0).getAtom(), fact, binding)) {
                    results.add(binding);
                }
            }
        }

        // Match subsequent literals
        for (int i = 1; i < bodyLiterals.size(); i++) {
            Literal literal = bodyLiterals.get(i);
            List<Map<String, String>> newResults = new ArrayList<>();

            for (Map<String, String> existingBinding : results) {
                for (Atom fact : facts) {
                    if (matches(literal, fact)) {
                        Map<String, String> newBinding = new HashMap<>(existingBinding);
                        if (unify(literal.getAtom(), fact, newBinding)) {
                            newResults.add(newBinding);
                        }
                    }
                }
            }

            results = newResults;
        }

        return results;
    }

    private boolean matches(Literal literal, Atom fact) {
        return literal.getAtom().getPredicate().equals(fact.getPredicate()) &&
               literal.getAtom().getArgs().size() == fact.getArgs().size();
    }

    private boolean unify(Atom pattern, Atom fact, Map<String, String> binding) {
        if (!pattern.getPredicate().equals(fact.getPredicate())) {
            return false;
        }

        List<String> patternArgs = pattern.getArgs();
        List<String> factArgs = fact.getArgs();

        if (patternArgs.size() != factArgs.size()) {
            return false;
        }

        for (int i = 0; i < patternArgs.size(); i++) {
            String patternArg = patternArgs.get(i);
            String factArg = factArgs.get(i);

            if (isVariable(patternArg)) {
                // Check if variable already bound
                if (binding.containsKey(patternArg)) {
                    if (!binding.get(patternArg).equals(factArg)) {
                        return false;  // Inconsistent binding
                    }
                } else {
                    binding.put(patternArg, factArg);
                }
            } else {
                // Constant must match
                if (!patternArg.equals(factArg)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isVariable(String term) {
        return term.length() > 0 && Character.isLowerCase(term.charAt(0));
    }

    private Atom instantiateHead(Atom head, Map<String, String> binding) {
        List<String> newArgs = head.getArgs().stream()
            .map(arg -> isVariable(arg) ? binding.getOrDefault(arg, arg) : arg)
            .collect(Collectors.toList());

        return new Atom(head.getPredicate(), newArgs);
    }

    private void updateTimeBounds(Instant instant) {
        if (minTime == null || instant.isBefore(minTime)) {
            minTime = instant;
        }
        if (maxTime == null || instant.isAfter(maxTime)) {
            maxTime = instant;
        }
    }

    private List<Instant> sampleInterval(TimeInterval interval, int numSamples) {
        List<Instant> samples = new ArrayList<>();

        Instant start = interval.getStart();
        Instant end = interval.getEnd();

        long durationMillis = Duration.between(start, end).toMillis();
        long stepMillis = durationMillis / (numSamples - 1);

        for (int i = 0; i < numSamples; i++) {
            Instant sample = start.plusMillis(i * stepMillis);
            samples.add(sample);
        }

        return samples;
    }

    /**
     * Result of continuous time reasoning.
     */
    public static class ContinuousTimeInterpretation {
        private final Map<Instant, Set<Atom>> factsAtTime;
        private final TimeInterval timeRange;

        public ContinuousTimeInterpretation(Map<Instant, Set<Atom>> factsAtTime, TimeInterval timeRange) {
            this.factsAtTime = new HashMap<>(factsAtTime);
            this.timeRange = timeRange;
        }

        /**
         * Gets facts at a specific instant.
         */
        public Set<Atom> getFactsAt(Instant instant) {
            return factsAtTime.getOrDefault(instant, Collections.emptySet());
        }

        /**
         * Gets all sampled time points.
         */
        public Set<Instant> getTimePoints() {
            return factsAtTime.keySet();
        }

        /**
         * Gets the time range covered by this interpretation.
         */
        public TimeInterval getTimeRange() {
            return timeRange;
        }

        /**
         * Gets all facts across all time points.
         */
        public Set<Atom> getAllFacts() {
            Set<Atom> allFacts = new HashSet<>();
            for (Set<Atom> facts : factsAtTime.values()) {
                allFacts.addAll(facts);
            }
            return allFacts;
        }

        /**
         * Finds time points where a specific atom holds.
         */
        public List<Instant> whenHolds(Atom atom) {
            return factsAtTime.entrySet().stream()
                .filter(e -> e.getValue().contains(atom))
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
        }
    }
}
