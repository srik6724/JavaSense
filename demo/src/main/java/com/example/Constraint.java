package com.example;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Represents a constraint that must hold during reasoning.
 *
 * <p>Constraints are integrity rules that validate the knowledge base.
 * They can be hard constraints (must never be violated) or soft constraints
 * (violations are warnings).</p>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * // Constraint: A person cannot be in two places at once
 * Constraint c = new Constraint(
 *     "no_bilocation",
 *     "A person cannot be at two locations simultaneously",
 *     atoms -> {
 *         // Check if same person appears in multiple at(person, location) facts
 *         // at the same time
 *         return true; // or false if violated
 *     },
 *     true // hard constraint
 * );
 * }</pre>
 */
public class Constraint {
    private final String name;
    private final String description;
    private final ConstraintChecker checker;
    private final boolean isHard; // true = must hold, false = warning only

    /**
     * Constructs a new constraint.
     *
     * @param name the constraint name
     * @param description human-readable description
     * @param checker the validation function
     * @param isHard whether this is a hard constraint (must hold)
     */
    public Constraint(String name, String description, ConstraintChecker checker, boolean isHard) {
        this.name = name;
        this.description = description;
        this.checker = checker;
        this.isHard = isHard;
    }

    /**
     * Checks if the constraint is satisfied.
     *
     * @param factsAtTime the facts at a specific timestep
     * @param time the timestep
     * @return true if constraint is satisfied
     */
    public boolean check(List<Atom> factsAtTime, int time) {
        return checker.check(factsAtTime, time);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHard() {
        return isHard;
    }

    public boolean isSoft() {
        return !isHard;
    }

    @Override
    public String toString() {
        return String.format("Constraint{name='%s', hard=%s}", name, isHard);
    }

    /**
     * Functional interface for constraint checking.
     */
    @FunctionalInterface
    public interface ConstraintChecker {
        /**
         * Checks if the constraint holds.
         *
         * @param factsAtTime facts at the current timestep
         * @param time the current timestep
         * @return true if constraint is satisfied
         */
        boolean check(List<Atom> factsAtTime, int time);
    }

    /**
     * Builder for common constraint patterns.
     */
    public static class Builder {
        /**
         * Creates a uniqueness constraint: at most one fact matching a pattern.
         *
         * <p>Example: Only one location per person at a time</p>
         *
         * @param name constraint name
         * @param predicate the predicate to check (e.g., "at")
         * @param keyArgIndex the argument that must be unique (e.g., 0 for person)
         * @return the constraint
         */
        public static Constraint uniqueness(String name, String predicate, int keyArgIndex) {
            return new Constraint(
                name,
                "At most one " + predicate + " per key",
                (facts, time) -> {
                    Map<String, Long> counts = facts.stream()
                        .filter(a -> a.getPredicate().equals(predicate))
                        .map(a -> a.getArgs().get(keyArgIndex))
                        .collect(java.util.stream.Collectors.groupingBy(
                            k -> k,
                            java.util.stream.Collectors.counting()
                        ));
                    return counts.values().stream().allMatch(count -> count <= 1);
                },
                true
            );
        }

        /**
         * Creates a mutual exclusion constraint: two predicates cannot both be true.
         *
         * @param name constraint name
         * @param predicate1 first predicate
         * @param predicate2 second predicate
         * @return the constraint
         */
        public static Constraint mutualExclusion(String name, String predicate1, String predicate2) {
            return new Constraint(
                name,
                predicate1 + " and " + predicate2 + " cannot both be true",
                (facts, time) -> {
                    boolean has1 = facts.stream().anyMatch(a -> a.getPredicate().equals(predicate1));
                    boolean has2 = facts.stream().anyMatch(a -> a.getPredicate().equals(predicate2));
                    return !(has1 && has2);
                },
                true
            );
        }

        /**
         * Creates a requirement constraint: if condition holds, requirement must hold.
         *
         * @param name constraint name
         * @param condition condition checker
         * @param requirement requirement checker
         * @return the constraint
         */
        public static Constraint requires(String name,
                                         Predicate<List<Atom>> condition,
                                         Predicate<List<Atom>> requirement) {
            return new Constraint(
                name,
                "Conditional requirement",
                (facts, time) -> {
                    if (condition.test(facts)) {
                        return requirement.test(facts);
                    }
                    return true;
                },
                true
            );
        }

        /**
         * Creates a cardinality constraint: number of matching facts must be in range.
         *
         * @param name constraint name
         * @param predicate the predicate to count
         * @param min minimum count (inclusive)
         * @param max maximum count (inclusive)
         * @return the constraint
         */
        public static Constraint cardinality(String name, String predicate, int min, int max) {
            return new Constraint(
                name,
                "Cardinality of " + predicate + " must be between " + min + " and " + max,
                (facts, time) -> {
                    long count = facts.stream()
                        .filter(a -> a.getPredicate().equals(predicate))
                        .count();
                    return count >= min && count <= max;
                },
                true
            );
        }
    }
}
