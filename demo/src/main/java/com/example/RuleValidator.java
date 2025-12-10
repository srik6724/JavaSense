package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Advanced Rule Validator with Quality Checks
 *
 * <p>Performs comprehensive validation of rule sets to detect issues before reasoning.
 * Critical for large rule bases (100+ rules) and multi-team development.</p>
 *
 * <h2>Validation Checks:</h2>
 * <ul>
 *   <li><b>Conflicts:</b> Overlapping heads, circular dependencies, redundant rules</li>
 *   <li><b>Unreachable rules:</b> Rules that can never fire</li>
 *   <li><b>Missing predicates:</b> Rules referencing undefined predicates</li>
 *   <li><b>Type inconsistencies:</b> Same predicate used with different arities</li>
 *   <li><b>Performance warnings:</b> Inefficient rule patterns</li>
 *   <li><b>Best practices:</b> Code quality checks</li>
 * </ul>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * RuleValidator validator = new RuleValidator();
 *
 * validator.addRule(new Rule("safe(x) <- approved(x)"));
 * validator.addRule(new Rule("unsafe(x) <- approved(x)"));  // Conflict!
 *
 * ValidationReport report = validator.validate();
 *
 * if (report.hasErrors()) {
 *     System.err.println("Validation failed!");
 *     report.printReport();
 *     System.exit(1);
 * }
 *
 * if (report.hasWarnings()) {
 *     System.out.println("Warnings found:");
 *     report.printWarnings();
 * }
 * }</pre>
 */
public class RuleValidator {
    private static final Logger logger = LoggerFactory.getLogger(RuleValidator.class);

    private final List<Rule> rules = new ArrayList<>();
    private final Set<TimedFact> baseFacts = new HashSet<>();

    /**
     * Adds a rule for validation.
     */
    public void addRule(Rule rule) {
        rules.add(rule);
    }

    /**
     * Adds base facts (used to check for unreachable rules).
     */
    public void addBaseFact(TimedFact fact) {
        baseFacts.add(fact);
    }

    /**
     * Performs comprehensive validation.
     *
     * @return validation report with errors and warnings
     */
    public ValidationReport validate() {
        logger.info("Starting rule validation for {} rules", rules.size());

        List<ValidationIssue> errors = new ArrayList<>();
        List<ValidationIssue> warnings = new ArrayList<>();

        // 1. Detect conflicts using ConflictDetector
        ConflictDetector detector = new ConflictDetector();
        for (Rule rule : rules) {
            detector.addRule(rule);
        }
        ConflictDetector.ConflictAnalysis conflicts = detector.analyze();

        for (ConflictDetector.RuleConflict conflict : conflicts.getConflicts()) {
            if (conflict.getType() == ConflictDetector.ConflictType.CIRCULAR_DEPENDENCY) {
                errors.add(new ValidationIssue(
                    IssueType.CIRCULAR_DEPENDENCY,
                    IssueSeverity.ERROR,
                    conflict.getInvolvedRules(),
                    conflict.getDescription()
                ));
            } else if (conflict.getType() == ConflictDetector.ConflictType.REDUNDANT) {
                warnings.add(new ValidationIssue(
                    IssueType.REDUNDANT_RULE,
                    IssueSeverity.WARNING,
                    conflict.getInvolvedRules(),
                    conflict.getDescription()
                ));
            } else {
                warnings.add(new ValidationIssue(
                    IssueType.OVERLAPPING_HEADS,
                    IssueSeverity.WARNING,
                    conflict.getInvolvedRules(),
                    conflict.getDescription()
                ));
            }
        }

        // 2. Check for type inconsistencies (same predicate, different arities)
        warnings.addAll(detectTypeInconsistencies());

        // 3. Check for unreachable rules
        warnings.addAll(detectUnreachableRules());

        // 4. Check for missing predicates in base facts
        warnings.addAll(detectMissingPredicates());

        // 5. Performance warnings
        warnings.addAll(detectPerformanceIssues());

        // 6. Best practice checks
        warnings.addAll(detectBestPracticeViolations());

        logger.info("Validation complete: {} errors, {} warnings", errors.size(), warnings.size());

        return new ValidationReport(errors, warnings);
    }

    // --- Detection Methods ---

    private List<ValidationIssue> detectTypeInconsistencies() {
        List<ValidationIssue> issues = new ArrayList<>();

        // Map predicate name → set of arities
        Map<String, Set<Integer>> predicateArities = new HashMap<>();

        for (Rule rule : rules) {
            // Check head
            Atom head = Atom.parse(rule.getHead());
            predicateArities.computeIfAbsent(head.getPredicate(), k -> new HashSet<>())
                           .add(head.arity());

            // Check body
            for (Literal lit : rule.getBodyLiterals()) {
                Atom atom = lit.getAtom();
                predicateArities.computeIfAbsent(atom.getPredicate(), k -> new HashSet<>())
                               .add(atom.arity());
            }
        }

        // Find predicates with multiple arities
        for (Map.Entry<String, Set<Integer>> entry : predicateArities.entrySet()) {
            if (entry.getValue().size() > 1) {
                issues.add(new ValidationIssue(
                    IssueType.TYPE_INCONSISTENCY,
                    IssueSeverity.WARNING,
                    Collections.emptyList(),
                    "Predicate '" + entry.getKey() + "' used with different arities: " + entry.getValue()
                ));
            }
        }

        return issues;
    }

    private List<ValidationIssue> detectUnreachableRules() {
        List<ValidationIssue> issues = new ArrayList<>();

        // Get all predicates in base facts
        Set<String> basePredicates = new HashSet<>();
        for (TimedFact fact : baseFacts) {
            basePredicates.add(fact.getAtom().getPredicate());
        }

        // Get all predicates that can be derived
        Set<String> derivablePredicates = new HashSet<>(basePredicates);
        boolean changed = true;
        int iterations = 0;

        while (changed && iterations < 100) {
            changed = false;
            iterations++;

            for (Rule rule : rules) {
                // Check if all positive body literals are derivable
                boolean allBodyDerivable = true;
                for (Literal lit : rule.getBodyLiterals()) {
                    if (lit.isPositive() && !derivablePredicates.contains(lit.getAtom().getPredicate())) {
                        allBodyDerivable = false;
                        break;
                    }
                }

                // If yes, head becomes derivable
                if (allBodyDerivable) {
                    String headPred = Atom.parse(rule.getHead()).getPredicate();
                    if (derivablePredicates.add(headPred)) {
                        changed = true;
                    }
                }
            }
        }

        // Find unreachable rules
        for (Rule rule : rules) {
            boolean reachable = true;
            for (Literal lit : rule.getBodyLiterals()) {
                if (lit.isPositive() && !derivablePredicates.contains(lit.getAtom().getPredicate())) {
                    reachable = false;
                    break;
                }
            }

            if (!reachable) {
                issues.add(new ValidationIssue(
                    IssueType.UNREACHABLE_RULE,
                    IssueSeverity.WARNING,
                    List.of(rule),
                    "Rule '" + rule.getName() + "' can never fire (missing base facts for body predicates)"
                ));
            }
        }

        return issues;
    }

    private List<ValidationIssue> detectMissingPredicates() {
        List<ValidationIssue> issues = new ArrayList<>();

        // Get all predicates used in rules
        Set<String> usedPredicates = new HashSet<>();
        for (Rule rule : rules) {
            for (Literal lit : rule.getBodyLiterals()) {
                usedPredicates.add(lit.getAtom().getPredicate());
            }
        }

        // Get all predicates in base facts or derivable
        Set<String> availablePredicates = new HashSet<>();
        for (TimedFact fact : baseFacts) {
            availablePredicates.add(fact.getAtom().getPredicate());
        }
        for (Rule rule : rules) {
            availablePredicates.add(Atom.parse(rule.getHead()).getPredicate());
        }

        // Find missing predicates (used but not available)
        for (String pred : usedPredicates) {
            if (!availablePredicates.contains(pred)) {
                issues.add(new ValidationIssue(
                    IssueType.MISSING_PREDICATE,
                    IssueSeverity.WARNING,
                    Collections.emptyList(),
                    "Predicate '" + pred + "' is used in rule bodies but never defined or derived"
                ));
            }
        }

        return issues;
    }

    private List<ValidationIssue> detectPerformanceIssues() {
        List<ValidationIssue> issues = new ArrayList<>();

        for (Rule rule : rules) {
            // Check for Cartesian product risk (multiple body literals with no shared variables)
            List<Literal> bodyLiterals = rule.getBodyLiterals();
            if (bodyLiterals.size() >= 2) {
                // Get variables in each literal
                for (int i = 0; i < bodyLiterals.size(); i++) {
                    Set<String> vars1 = getVariables(bodyLiterals.get(i).getAtom());
                    boolean hasSharedVar = false;

                    for (int j = 0; j < bodyLiterals.size(); j++) {
                        if (i != j) {
                            Set<String> vars2 = getVariables(bodyLiterals.get(j).getAtom());
                            Set<String> intersection = new HashSet<>(vars1);
                            intersection.retainAll(vars2);
                            if (!intersection.isEmpty()) {
                                hasSharedVar = true;
                                break;
                            }
                        }
                    }

                    if (!hasSharedVar && vars1.size() > 0) {
                        issues.add(new ValidationIssue(
                            IssueType.PERFORMANCE_WARNING,
                            IssueSeverity.WARNING,
                            List.of(rule),
                            "Rule '" + rule.getName() + "' may cause Cartesian product (no shared variables between literals)"
                        ));
                        break;
                    }
                }
            }

            // Check for negation with unbound variables (unsafe)
            for (Literal lit : bodyLiterals) {
                if (lit.isNegated()) {
                    Set<String> negVars = getVariables(lit.getAtom());
                    Set<String> boundVars = new HashSet<>();

                    // Collect variables from positive literals
                    for (Literal posLit : bodyLiterals) {
                        if (posLit.isPositive()) {
                            boundVars.addAll(getVariables(posLit.getAtom()));
                        }
                    }

                    // Check if negation has unbound variables
                    negVars.removeAll(boundVars);
                    if (!negVars.isEmpty()) {
                        issues.add(new ValidationIssue(
                            IssueType.UNSAFE_NEGATION,
                            IssueSeverity.ERROR,
                            List.of(rule),
                            "Rule '" + rule.getName() + "' has unsafe negation (unbound variables in negated literal: " + negVars + ")"
                        ));
                    }
                }
            }
        }

        return issues;
    }

    private List<ValidationIssue> detectBestPracticeViolations() {
        List<ValidationIssue> issues = new ArrayList<>();

        for (Rule rule : rules) {
            // Check for empty rule names
            if (rule.getName() == null || rule.getName().isEmpty()) {
                issues.add(new ValidationIssue(
                    IssueType.BEST_PRACTICE,
                    IssueSeverity.INFO,
                    List.of(rule),
                    "Rule has no name - consider adding descriptive name for debugging"
                ));
            }

            // Check for very long rules (>10 body literals)
            if (rule.getBodyLiterals().size() > 10) {
                issues.add(new ValidationIssue(
                    IssueType.BEST_PRACTICE,
                    IssueSeverity.INFO,
                    List.of(rule),
                    "Rule '" + rule.getName() + "' has " + rule.getBodyLiterals().size() + " body literals - consider splitting into multiple rules"
                ));
            }

            // Check for singleton variables (variables that appear only once)
            Map<String, Integer> varCounts = new HashMap<>();
            for (Literal lit : rule.getBodyLiterals()) {
                for (String var : getVariables(lit.getAtom())) {
                    varCounts.merge(var, 1, Integer::sum);
                }
            }
            for (String var : getVariables(Atom.parse(rule.getHead()))) {
                varCounts.merge(var, 1, Integer::sum);
            }

            List<String> singletons = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : varCounts.entrySet()) {
                if (entry.getValue() == 1) {
                    singletons.add(entry.getKey());
                }
            }

            if (!singletons.isEmpty()) {
                issues.add(new ValidationIssue(
                    IssueType.BEST_PRACTICE,
                    IssueSeverity.INFO,
                    List.of(rule),
                    "Rule '" + rule.getName() + "' has singleton variables (appear only once): " + singletons
                ));
            }
        }

        return issues;
    }

    // --- Helper Methods ---

    private Set<String> getVariables(Atom atom) {
        Set<String> vars = new HashSet<>();
        for (String arg : atom.getArgs()) {
            if (isVariable(arg)) {
                vars.add(arg);
            }
        }
        return vars;
    }

    private boolean isVariable(String s) {
        return s.length() > 0 && Character.isLowerCase(s.charAt(0));
    }

    // --- Inner Classes ---

    public enum IssueType {
        CIRCULAR_DEPENDENCY,
        REDUNDANT_RULE,
        OVERLAPPING_HEADS,
        TYPE_INCONSISTENCY,
        UNREACHABLE_RULE,
        MISSING_PREDICATE,
        PERFORMANCE_WARNING,
        UNSAFE_NEGATION,
        BEST_PRACTICE
    }

    public enum IssueSeverity {
        ERROR,    // Must be fixed
        WARNING,  // Should be fixed
        INFO      // Nice to fix
    }

    public static class ValidationIssue {
        private final IssueType type;
        private final IssueSeverity severity;
        private final List<Rule> involvedRules;
        private final String description;

        public ValidationIssue(IssueType type, IssueSeverity severity, List<Rule> involvedRules, String description) {
            this.type = type;
            this.severity = severity;
            this.involvedRules = new ArrayList<>(involvedRules);
            this.description = description;
        }

        public IssueType getType() { return type; }
        public IssueSeverity getSeverity() { return severity; }
        public List<Rule> getInvolvedRules() { return Collections.unmodifiableList(involvedRules); }
        public String getDescription() { return description; }

        @Override
        public String toString() {
            String icon = severity == IssueSeverity.ERROR ? "❌" :
                         severity == IssueSeverity.WARNING ? "⚠️" : "ℹ️";
            return icon + " [" + severity + "] " + type + ": " + description;
        }
    }

    public static class ValidationReport {
        private final List<ValidationIssue> errors;
        private final List<ValidationIssue> warnings;

        public ValidationReport(List<ValidationIssue> errors, List<ValidationIssue> warnings) {
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public List<ValidationIssue> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        public List<ValidationIssue> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }

        public void printReport() {
            if (errors.isEmpty() && warnings.isEmpty()) {
                System.out.println("✅ All rules valid - no issues found!");
                return;
            }

            if (!errors.isEmpty()) {
                System.err.println("\n" + "=".repeat(70));
                System.err.println("ERRORS (" + errors.size() + "):");
                System.err.println("=".repeat(70));
                for (ValidationIssue error : errors) {
                    System.err.println(error);
                }
            }

            if (!warnings.isEmpty()) {
                System.out.println("\n" + "=".repeat(70));
                System.out.println("WARNINGS (" + warnings.size() + "):");
                System.out.println("=".repeat(70));
                for (ValidationIssue warning : warnings) {
                    System.out.println(warning);
                }
            }
        }

        public void printWarnings() {
            for (ValidationIssue warning : warnings) {
                System.out.println(warning);
            }
        }
    }
}
