package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Detects conflicts between rules in the knowledge base.
 *
 * <p>Rule conflicts occur when:
 * <ul>
 *   <li>Two rules can derive contradictory facts (e.g., p(x) and not p(x))</li>
 *   <li>Two rules have overlapping heads but different conditions</li>
 *   <li>Rules form circular dependencies</li>
 *   <li>Rules have inconsistent temporal constraints</li>
 * </ul>
 */
public class ConflictDetector {
    private static final Logger logger = LoggerFactory.getLogger(ConflictDetector.class);

    private final List<Rule> rules = new ArrayList<>();

    /**
     * Adds a rule for conflict analysis.
     *
     * @param rule the rule to analyze
     */
    public void addRule(Rule rule) {
        rules.add(rule);
    }

    /**
     * Detects all conflicts in the rule set.
     *
     * @return conflict analysis result
     */
    public ConflictAnalysis analyze() {
        List<RuleConflict> conflicts = new ArrayList<>();

        // Check for overlapping head conflicts
        conflicts.addAll(detectOverlappingHeads());

        // Check for circular dependencies
        conflicts.addAll(detectCircularDependencies());

        // Check for redundant rules
        conflicts.addAll(detectRedundantRules());

        return new ConflictAnalysis(conflicts);
    }

    /**
     * Detects rules with overlapping heads (could derive same facts).
     */
    private List<RuleConflict> detectOverlappingHeads() {
        List<RuleConflict> conflicts = new ArrayList<>();

        for (int i = 0; i < rules.size(); i++) {
            for (int j = i + 1; j < rules.size(); j++) {
                Rule r1 = rules.get(i);
                Rule r2 = rules.get(j);

                if (headsCanUnify(r1.getHead(), r2.getHead())) {
                    conflicts.add(new RuleConflict(
                        ConflictType.OVERLAPPING_HEADS,
                        Arrays.asList(r1, r2),
                        "Rules can derive the same fact: " + r1.getHead()
                    ));
                }
            }
        }

        return conflicts;
    }

    /**
     * Detects circular dependencies between rules.
     */
    private List<RuleConflict> detectCircularDependencies() {
        List<RuleConflict> conflicts = new ArrayList<>();

        // Build dependency graph
        Map<String, Set<String>> dependencies = new HashMap<>();
        for (Rule rule : rules) {
            String headPred = extractPredicate(rule.getHead());
            Set<String> bodyPreds = new HashSet<>();

            for (Literal lit : rule.getBodyLiterals()) {
                if (lit.isPositive()) {
                    bodyPreds.add(lit.getAtom().getPredicate());
                }
            }

            dependencies.put(headPred, bodyPreds);
        }

        // Detect cycles using DFS
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String predicate : dependencies.keySet()) {
            List<String> cycle = detectCycle(predicate, dependencies, visited, recursionStack, new ArrayList<>());
            if (cycle != null && !cycle.isEmpty()) {
                List<Rule> cycleRules = findRulesInCycle(cycle);
                conflicts.add(new RuleConflict(
                    ConflictType.CIRCULAR_DEPENDENCY,
                    cycleRules,
                    "Circular dependency detected: " + String.join(" -> ", cycle)
                ));
            }
        }

        return conflicts;
    }

    /**
     * Detects redundant rules (rules that are subsumed by others).
     */
    private List<RuleConflict> detectRedundantRules() {
        List<RuleConflict> conflicts = new ArrayList<>();

        for (int i = 0; i < rules.size(); i++) {
            for (int j = i + 1; j < rules.size(); j++) {
                Rule r1 = rules.get(i);
                Rule r2 = rules.get(j);

                if (isRedundant(r1, r2)) {
                    conflicts.add(new RuleConflict(
                        ConflictType.REDUNDANT,
                        Arrays.asList(r1, r2),
                        "Rule " + r1.getName() + " is redundant with " + r2.getName()
                    ));
                }
            }
        }

        return conflicts;
    }

    private boolean headsCanUnify(String head1, String head2) {
        try {
            Atom a1 = Atom.parse(head1);
            Atom a2 = Atom.parse(head2);

            if (!a1.getPredicate().equals(a2.getPredicate())) {
                return false;
            }

            if (a1.arity() != a2.arity()) {
                return false;
            }

            // Check if arguments can unify (simple check)
            for (int i = 0; i < a1.arity(); i++) {
                String arg1 = a1.getArgs().get(i);
                String arg2 = a2.getArgs().get(i);

                // If both are constants and different, they can't unify
                if (!isVariable(arg1) && !isVariable(arg2) && !arg1.equals(arg2)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractPredicate(String atomStr) {
        int parenIdx = atomStr.indexOf('(');
        return parenIdx > 0 ? atomStr.substring(0, parenIdx) : atomStr;
    }

    private List<String> detectCycle(String node,
                                     Map<String, Set<String>> graph,
                                     Set<String> visited,
                                     Set<String> recursionStack,
                                     List<String> path) {
        if (recursionStack.contains(node)) {
            // Found a cycle
            List<String> cycle = new ArrayList<>();
            int idx = path.indexOf(node);
            if (idx >= 0) {
                cycle.addAll(path.subList(idx, path.size()));
            }
            cycle.add(node);
            return cycle;
        }

        if (visited.contains(node)) {
            return null;
        }

        visited.add(node);
        recursionStack.add(node);
        path.add(node);

        Set<String> neighbors = graph.getOrDefault(node, Collections.emptySet());
        for (String neighbor : neighbors) {
            List<String> cycle = detectCycle(neighbor, graph, visited, recursionStack, new ArrayList<>(path));
            if (cycle != null) {
                return cycle;
            }
        }

        recursionStack.remove(node);
        return null;
    }

    private List<Rule> findRulesInCycle(List<String> predicates) {
        List<Rule> cycleRules = new ArrayList<>();
        Set<String> predSet = new HashSet<>(predicates);

        for (Rule rule : rules) {
            String headPred = extractPredicate(rule.getHead());
            if (predSet.contains(headPred)) {
                cycleRules.add(rule);
            }
        }

        return cycleRules;
    }

    private boolean isRedundant(Rule r1, Rule r2) {
        // Simple check: same head and same body
        if (!r1.getHead().equals(r2.getHead())) {
            return false;
        }

        if (r1.getDelay() != r2.getDelay()) {
            return false;
        }

        Set<String> body1 = new HashSet<>(r1.getBodyAtoms());
        Set<String> body2 = new HashSet<>(r2.getBodyAtoms());

        return body1.equals(body2);
    }

    private boolean isVariable(String s) {
        return s.length() > 0 && Character.isLowerCase(s.charAt(0));
    }

    /**
     * Types of rule conflicts.
     */
    public enum ConflictType {
        OVERLAPPING_HEADS,
        CIRCULAR_DEPENDENCY,
        REDUNDANT,
        CONTRADICTORY
    }

    /**
     * Represents a conflict between rules.
     */
    public static class RuleConflict {
        private final ConflictType type;
        private final List<Rule> involvedRules;
        private final String description;

        public RuleConflict(ConflictType type, List<Rule> involvedRules, String description) {
            this.type = type;
            this.involvedRules = new ArrayList<>(involvedRules);
            this.description = description;
        }

        public ConflictType getType() {
            return type;
        }

        public List<Rule> getInvolvedRules() {
            return Collections.unmodifiableList(involvedRules);
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(type).append(": ").append(description).append("\n");
            sb.append("  Involved rules:\n");
            for (Rule rule : involvedRules) {
                sb.append("    - ").append(rule.getName()).append(": ").append(rule.getRaw()).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * Result of conflict analysis.
     */
    public static class ConflictAnalysis {
        private final List<RuleConflict> conflicts;

        public ConflictAnalysis(List<RuleConflict> conflicts) {
            this.conflicts = new ArrayList<>(conflicts);
        }

        public boolean hasConflicts() {
            return !conflicts.isEmpty();
        }

        public List<RuleConflict> getConflicts() {
            return Collections.unmodifiableList(conflicts);
        }

        public List<RuleConflict> getConflictsByType(ConflictType type) {
            return conflicts.stream()
                .filter(c -> c.getType() == type)
                .toList();
        }

        public void display() {
            if (conflicts.isEmpty()) {
                System.out.println("No conflicts detected.");
                return;
            }

            System.out.println("Detected " + conflicts.size() + " conflict(s):");
            for (RuleConflict conflict : conflicts) {
                System.out.println(conflict);
            }
        }

        @Override
        public String toString() {
            return String.format("ConflictAnalysis{conflicts=%d}", conflicts.size());
        }
    }
}
