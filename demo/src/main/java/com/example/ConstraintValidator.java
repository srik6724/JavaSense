package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Validates constraints during and after reasoning.
 *
 * <p>The ConstraintValidator checks both hard constraints (which must hold)
 * and soft constraints (which generate warnings when violated).</p>
 */
public class ConstraintValidator {
    private static final Logger logger = LoggerFactory.getLogger(ConstraintValidator.class);

    private final List<Constraint> constraints = new ArrayList<>();
    private final List<ConstraintViolation> violations = new ArrayList<>();

    /**
     * Adds a constraint to be checked.
     *
     * @param constraint the constraint to add
     */
    public void addConstraint(Constraint constraint) {
        constraints.add(constraint);
    }

    /**
     * Validates all constraints against a reasoning interpretation.
     *
     * @param interpretation the reasoning result
     * @return validation result
     */
    public ValidationResult validate(ReasoningInterpretation interpretation) {
        violations.clear();

        for (int t = 0; t <= interpretation.getMaxTime(); t++) {
            Set<Atom> factsSet = interpretation.getFactsAt(t);
            List<Atom> factsList = new ArrayList<>(factsSet);

            for (Constraint constraint : constraints) {
                if (!constraint.check(factsList, t)) {
                    ConstraintViolation violation = new ConstraintViolation(
                        constraint,
                        t,
                        factsList
                    );
                    violations.add(violation);

                    if (constraint.isHard()) {
                        logger.error("Hard constraint violated: {} at t={}", constraint.getName(), t);
                    } else {
                        logger.warn("Soft constraint violated: {} at t={}", constraint.getName(), t);
                    }
                }
            }
        }

        return new ValidationResult(violations);
    }

    /**
     * Validates constraints at a specific timestep (for incremental checking).
     *
     * @param factsAtTime facts at the timestep
     * @param time the timestep
     * @return list of violations at this timestep
     */
    public List<ConstraintViolation> validateAtTime(List<Atom> factsAtTime, int time) {
        List<ConstraintViolation> timeViolations = new ArrayList<>();

        for (Constraint constraint : constraints) {
            if (!constraint.check(factsAtTime, time)) {
                ConstraintViolation violation = new ConstraintViolation(
                    constraint,
                    time,
                    factsAtTime
                );
                timeViolations.add(violation);
            }
        }

        return timeViolations;
    }

    /**
     * Gets all registered constraints.
     *
     * @return list of constraints
     */
    public List<Constraint> getConstraints() {
        return Collections.unmodifiableList(constraints);
    }

    /**
     * Clears all violations.
     */
    public void clearViolations() {
        violations.clear();
    }

    /**
     * Represents a constraint violation.
     */
    public static class ConstraintViolation {
        private final Constraint constraint;
        private final int time;
        private final List<Atom> facts;

        public ConstraintViolation(Constraint constraint, int time, List<Atom> facts) {
            this.constraint = constraint;
            this.time = time;
            this.facts = new ArrayList<>(facts);
        }

        public Constraint getConstraint() {
            return constraint;
        }

        public int getTime() {
            return time;
        }

        public List<Atom> getFacts() {
            return Collections.unmodifiableList(facts);
        }

        @Override
        public String toString() {
            return String.format("Violation of '%s' (%s) at t=%d: %s",
                constraint.getName(),
                constraint.isHard() ? "HARD" : "soft",
                time,
                constraint.getDescription());
        }
    }

    /**
     * Represents the result of constraint validation.
     */
    public static class ValidationResult {
        private final List<ConstraintViolation> violations;

        public ValidationResult(List<ConstraintViolation> violations) {
            this.violations = new ArrayList<>(violations);
        }

        public boolean isValid() {
            return violations.stream().noneMatch(v -> v.getConstraint().isHard());
        }

        public boolean hasViolations() {
            return !violations.isEmpty();
        }

        public List<ConstraintViolation> getViolations() {
            return Collections.unmodifiableList(violations);
        }

        public List<ConstraintViolation> getHardViolations() {
            return violations.stream()
                .filter(v -> v.getConstraint().isHard())
                .toList();
        }

        public List<ConstraintViolation> getSoftViolations() {
            return violations.stream()
                .filter(v -> v.getConstraint().isSoft())
                .toList();
        }

        public void display() {
            if (violations.isEmpty()) {
                System.out.println("All constraints satisfied.");
                return;
            }

            System.out.println("Constraint violations:");
            for (ConstraintViolation v : violations) {
                System.out.println("  " + v);
            }
        }

        @Override
        public String toString() {
            return String.format("ValidationResult{valid=%s, violations=%d (hard=%d, soft=%d)}",
                isValid(),
                violations.size(),
                getHardViolations().size(),
                getSoftViolations().size());
        }
    }
}
