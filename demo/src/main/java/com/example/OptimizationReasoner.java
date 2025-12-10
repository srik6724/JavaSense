package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Optimization Reasoner - Finding Optimal Solutions
 *
 * <p>Extends standard reasoning to find solutions that maximize or minimize
 * an objective function subject to constraints.</p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li><b>Objective functions:</b> Maximize or minimize numeric values</li>
 *   <li><b>Hard constraints:</b> Must-satisfy constraints (eliminate infeasible solutions)</li>
 *   <li><b>Soft constraints:</b> Preferences with penalties (try to satisfy)</li>
 *   <li><b>Solution ranking:</b> Find top-k best solutions</li>
 *   <li><b>Multi-objective:</b> Trade-offs between competing objectives</li>
 * </ul>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * OptimizationReasoner reasoner = new OptimizationReasoner();
 *
 * // Add facts and rules
 * reasoner.addFact(new TimedFact(Atom.parse("supplier(ACME,100,0.95)"), ...));
 * reasoner.addRule(new Rule("selected(x) <-0 supplier(x,cost,quality)"));
 *
 * // Add hard constraint: quality must be > 0.9
 * reasoner.addHardConstraint("quality(x,q)", q -> q > 0.9);
 *
 * // Objective: minimize cost
 * reasoner.setObjective(OptimizationReasoner.Objective.MINIMIZE, "cost");
 *
 * // Find optimal solution
 * OptimizationResult result = reasoner.optimize(10);
 * System.out.println("Best solution: " + result.getBestSolution());
 * System.out.println("Objective value: " + result.getObjectiveValue());
 * }</pre>
 *
 * <h2>Use Cases:</h2>
 * <ul>
 *   <li>Resource allocation (minimize cost, maximize utility)</li>
 *   <li>Scheduling (minimize makespan, maximize throughput)</li>
 *   <li>Supplier selection (minimize cost, maximize quality)</li>
 *   <li>Route planning (minimize distance, time, cost)</li>
 *   <li>Configuration optimization</li>
 * </ul>
 */
public class OptimizationReasoner {
    private static final Logger logger = LoggerFactory.getLogger(OptimizationReasoner.class);

    private final OptimizedReasoner baseReasoner;
    private final List<HardConstraint> hardConstraints = new ArrayList<>();
    private final List<SoftConstraint> softConstraints = new ArrayList<>();

    private ObjectiveType objectiveType = ObjectiveType.MINIMIZE;
    private String objectivePredicate = null;
    private Function<Atom, Double> objectiveFunction = null;

    public enum ObjectiveType {
        MINIMIZE,
        MAXIMIZE
    }

    public OptimizationReasoner() {
        this.baseReasoner = new OptimizedReasoner();
    }

    /**
     * Adds a standard rule.
     */
    public void addRule(Rule rule) {
        baseReasoner.addRule(rule);
    }

    /**
     * Adds a standard fact.
     */
    public void addFact(TimedFact fact) {
        baseReasoner.addFact(fact);
    }

    /**
     * Sets the optimization objective.
     *
     * @param type MINIMIZE or MAXIMIZE
     * @param predicate the predicate to optimize (must have numeric argument)
     */
    public void setObjective(ObjectiveType type, String predicate) {
        this.objectiveType = type;
        this.objectivePredicate = predicate;
        logger.info("Set objective: {} {}", type, predicate);
    }

    /**
     * Sets a custom objective function.
     *
     * @param type MINIMIZE or MAXIMIZE
     * @param function maps atoms to numeric values
     */
    public void setObjectiveFunction(ObjectiveType type, Function<Atom, Double> function) {
        this.objectiveType = type;
        this.objectiveFunction = function;
        logger.info("Set custom objective function: {}", type);
    }

    /**
     * Adds a hard constraint (must be satisfied).
     *
     * <p>Hard constraints eliminate infeasible solutions.</p>
     *
     * @param predicatePattern predicate to check (e.g., "quality(x,q)")
     * @param checker returns true if constraint satisfied
     */
    public void addHardConstraint(String predicatePattern, Function<Atom, Boolean> checker) {
        hardConstraints.add(new HardConstraint(predicatePattern, checker));
        logger.debug("Added hard constraint: {}", predicatePattern);
    }

    /**
     * Adds a soft constraint with penalty.
     *
     * <p>Soft constraints are preferences - violations add penalties to objective.</p>
     *
     * @param predicatePattern predicate to check
     * @param penalty penalty value if violated
     * @param checker returns true if constraint satisfied
     */
    public void addSoftConstraint(String predicatePattern, double penalty,
                                  Function<Atom, Boolean> checker) {
        softConstraints.add(new SoftConstraint(predicatePattern, penalty, checker));
        logger.debug("Added soft constraint: {} (penalty={})", predicatePattern, penalty);
    }

    /**
     * Performs optimization reasoning.
     *
     * @param timesteps maximum timestep
     * @return optimization result with best solution
     */
    public OptimizationResult optimize(int timesteps) {
        logger.info("Starting optimization reasoning for {} timesteps", timesteps);

        // Perform base reasoning
        ReasoningInterpretation baseResult = baseReasoner.reason(timesteps);

        // Generate candidate solutions
        List<Solution> candidates = generateCandidates(baseResult, timesteps);
        logger.info("Generated {} candidate solutions", candidates.size());

        // Filter by hard constraints
        List<Solution> feasible = filterByHardConstraints(candidates);
        logger.info("{} feasible solutions after hard constraint filtering", feasible.size());

        if (feasible.isEmpty()) {
            logger.warn("No feasible solutions found!");
            return new OptimizationResult(null, Collections.emptyList(), baseResult);
        }

        // Score solutions (including soft constraint penalties)
        for (Solution solution : feasible) {
            double score = computeObjective(solution);
            double penalty = computeSoftPenalties(solution);

            if (objectiveType == ObjectiveType.MINIMIZE) {
                solution.score = score + penalty;
            } else {
                solution.score = score - penalty;
            }
        }

        // Sort by score
        feasible.sort((s1, s2) -> {
            if (objectiveType == ObjectiveType.MINIMIZE) {
                return Double.compare(s1.score, s2.score);
            } else {
                return Double.compare(s2.score, s1.score);
            }
        });

        Solution best = feasible.get(0);
        logger.info("Optimization complete. Best score: {}", best.score);

        return new OptimizationResult(best, feasible, baseResult);
    }

    /**
     * Finds the top-k best solutions.
     *
     * @param timesteps maximum timestep
     * @param k number of solutions to return
     * @return top-k solutions
     */
    public List<Solution> findTopK(int timesteps, int k) {
        OptimizationResult result = optimize(timesteps);
        return result.getAllSolutions().stream()
            .limit(k)
            .collect(Collectors.toList());
    }

    // --- Internal Methods ---

    private List<Solution> generateCandidates(ReasoningInterpretation interpretation, int timesteps) {
        List<Solution> candidates = new ArrayList<>();

        // For each timestep, create a solution
        for (int t = 0; t <= timesteps; t++) {
            Set<Atom> factsAtTime = interpretation.getFactsAt(t);
            if (!factsAtTime.isEmpty()) {
                Solution solution = new Solution(t, new HashSet<>(factsAtTime));
                candidates.add(solution);
            }
        }

        // Also consider cumulative solutions (all facts up to time t)
        Set<Atom> cumulative = new HashSet<>();
        for (int t = 0; t <= timesteps; t++) {
            cumulative.addAll(interpretation.getFactsAt(t));
            if (!cumulative.isEmpty()) {
                Solution solution = new Solution(t, new HashSet<>(cumulative));
                candidates.add(solution);
            }
        }

        return candidates;
    }

    private List<Solution> filterByHardConstraints(List<Solution> candidates) {
        return candidates.stream()
            .filter(this::satisfiesHardConstraints)
            .collect(Collectors.toList());
    }

    private boolean satisfiesHardConstraints(Solution solution) {
        for (HardConstraint constraint : hardConstraints) {
            for (Atom atom : solution.facts) {
                if (matchesPattern(atom, constraint.pattern)) {
                    if (!constraint.checker.apply(atom)) {
                        return false;  // Hard constraint violated
                    }
                }
            }
        }
        return true;
    }

    private double computeObjective(Solution solution) {
        if (objectiveFunction != null) {
            // Use custom objective function
            return solution.facts.stream()
                .mapToDouble(atom -> objectiveFunction.apply(atom))
                .sum();
        }

        if (objectivePredicate != null) {
            // Use predicate-based objective
            return solution.facts.stream()
                .filter(atom -> atom.getPredicate().equals(objectivePredicate))
                .mapToDouble(this::extractNumericValue)
                .sum();
        }

        // Default: count facts
        return solution.facts.size();
    }

    private double computeSoftPenalties(Solution solution) {
        double totalPenalty = 0.0;

        for (SoftConstraint constraint : softConstraints) {
            for (Atom atom : solution.facts) {
                if (matchesPattern(atom, constraint.pattern)) {
                    if (!constraint.checker.apply(atom)) {
                        totalPenalty += constraint.penalty;
                    }
                }
            }
        }

        return totalPenalty;
    }

    private boolean matchesPattern(Atom atom, String pattern) {
        // Simple pattern matching: extract predicate from pattern
        String patternPredicate = pattern.split("\\(")[0];
        return atom.getPredicate().equals(patternPredicate);
    }

    private double extractNumericValue(Atom atom) {
        // Try to extract a numeric value from atom arguments
        for (String arg : atom.getArgs()) {
            try {
                return Double.parseDouble(arg);
            } catch (NumberFormatException e) {
                // Continue to next argument
            }
        }
        return 0.0;
    }

    // --- Inner Classes ---

    private static class HardConstraint {
        final String pattern;
        final Function<Atom, Boolean> checker;

        HardConstraint(String pattern, Function<Atom, Boolean> checker) {
            this.pattern = pattern;
            this.checker = checker;
        }
    }

    private static class SoftConstraint {
        final String pattern;
        final double penalty;
        final Function<Atom, Boolean> checker;

        SoftConstraint(String pattern, double penalty, Function<Atom, Boolean> checker) {
            this.pattern = pattern;
            this.penalty = penalty;
            this.checker = checker;
        }
    }

    /**
     * A candidate solution with facts and objective score.
     */
    public static class Solution {
        private final int timestep;
        private final Set<Atom> facts;
        private double score;

        public Solution(int timestep, Set<Atom> facts) {
            this.timestep = timestep;
            this.facts = facts;
            this.score = 0.0;
        }

        public int getTimestep() {
            return timestep;
        }

        public Set<Atom> getFacts() {
            return Collections.unmodifiableSet(facts);
        }

        public double getScore() {
            return score;
        }

        /**
         * Gets facts with a specific predicate.
         */
        public List<Atom> getFactsWithPredicate(String predicate) {
            return facts.stream()
                .filter(atom -> atom.getPredicate().equals(predicate))
                .collect(Collectors.toList());
        }

        @Override
        public String toString() {
            return String.format("Solution{time=%d, facts=%d, score=%.2f}",
                timestep, facts.size(), score);
        }
    }

    /**
     * Result of optimization with best solution and alternatives.
     */
    public static class OptimizationResult {
        private final Solution bestSolution;
        private final List<Solution> allSolutions;
        private final ReasoningInterpretation baseInterpretation;

        public OptimizationResult(Solution bestSolution,
                                 List<Solution> allSolutions,
                                 ReasoningInterpretation baseInterpretation) {
            this.bestSolution = bestSolution;
            this.allSolutions = new ArrayList<>(allSolutions);
            this.baseInterpretation = baseInterpretation;
        }

        /**
         * Gets the best (optimal) solution.
         */
        public Solution getBestSolution() {
            return bestSolution;
        }

        /**
         * Gets the objective value of the best solution.
         */
        public double getObjectiveValue() {
            return bestSolution != null ? bestSolution.score : Double.NaN;
        }

        /**
         * Gets all feasible solutions (sorted by score).
         */
        public List<Solution> getAllSolutions() {
            return Collections.unmodifiableList(allSolutions);
        }

        /**
         * Gets the top-k best solutions.
         */
        public List<Solution> getTopK(int k) {
            return allSolutions.stream().limit(k).collect(Collectors.toList());
        }

        /**
         * Gets the base reasoning interpretation.
         */
        public ReasoningInterpretation getBaseInterpretation() {
            return baseInterpretation;
        }

        /**
         * Checks if any feasible solution exists.
         */
        public boolean hasFeasibleSolution() {
            return bestSolution != null;
        }
    }
}
