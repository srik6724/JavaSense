package com.example;

import java.util.*;

/**
 * Tracks the provenance (derivation history) of derived facts.
 *
 * <p>Provenance records how each fact was derived, including which rule was applied,
 * what facts were used in the derivation, and the variable substitutions.</p>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * Provenance prov = ...;
 * DerivationInfo info = prov.getDerivation(atom, time);
 * System.out.println("Derived by rule: " + info.getRuleName());
 * System.out.println("From facts: " + info.getSourceFacts());
 * }</pre>
 */
public class Provenance {
    // Key: (Atom, time) -> DerivationInfo
    private final Map<AtomTimeKey, DerivationInfo> derivations = new HashMap<>();

    /**
     * Records how a fact was derived.
     *
     * @param atom the derived fact
     * @param time the timestep where it was derived
     * @param info the derivation information
     */
    public void record(Atom atom, int time, DerivationInfo info) {
        derivations.put(new AtomTimeKey(atom, time), info);
    }

    /**
     * Gets the derivation information for a fact at a specific time.
     *
     * @param atom the fact to query
     * @param time the timestep
     * @return derivation info, or null if not found or is a base fact
     */
    public DerivationInfo getDerivation(Atom atom, int time) {
        return derivations.get(new AtomTimeKey(atom, time));
    }

    /**
     * Checks if a fact was derived (vs. being a base fact).
     *
     * @param atom the fact to check
     * @param time the timestep
     * @return true if the fact was derived by a rule
     */
    public boolean isDerived(Atom atom, int time) {
        return derivations.containsKey(new AtomTimeKey(atom, time));
    }

    /**
     * Gets the full derivation tree (all facts used to derive this fact recursively).
     *
     * @param atom the fact to trace
     * @param time the timestep
     * @return derivation tree
     */
    public DerivationTree getDerivationTree(Atom atom, int time) {
        DerivationInfo info = getDerivation(atom, time);
        if (info == null) {
            // Base fact
            return new DerivationTree(atom, time, null, new ArrayList<>());
        }

        List<DerivationTree> children = new ArrayList<>();
        for (AtomTimeKey source : info.getSourceFacts()) {
            children.add(getDerivationTree(source.atom, source.time));
        }

        return new DerivationTree(atom, time, info, children);
    }

    /**
     * Generates a human-readable explanation of how a fact was derived.
     *
     * @param atom the fact to explain
     * @param time the timestep
     * @return explanation string
     */
    public String explain(Atom atom, int time) {
        StringBuilder sb = new StringBuilder();
        explainRecursive(atom, time, 0, sb, new HashSet<>());
        return sb.toString();
    }

    private void explainRecursive(Atom atom, int time, int depth, StringBuilder sb,
                                  Set<AtomTimeKey> visited) {
        AtomTimeKey key = new AtomTimeKey(atom, time);

        String indent = "  ".repeat(depth);
        sb.append(indent).append("t=").append(time).append(": ").append(atom);

        if (visited.contains(key)) {
            sb.append(" (already explained)\n");
            return;
        }
        visited.add(key);

        DerivationInfo info = getDerivation(atom, time);
        if (info == null) {
            sb.append(" [base fact]\n");
            return;
        }

        sb.append(" [derived by rule: ").append(info.getRuleName()).append("]\n");
        sb.append(indent).append("  with substitution: ").append(info.getSubstitution()).append("\n");
        sb.append(indent).append("  from:\n");

        for (AtomTimeKey source : info.getSourceFacts()) {
            explainRecursive(source.atom, source.time, depth + 2, sb, visited);
        }
    }

    /**
     * Gets all facts derived by a specific rule.
     *
     * @param ruleName the rule name to filter by
     * @return list of (atom, time) pairs derived by this rule
     */
    public List<AtomTimeKey> getFactsDeriveredByRule(String ruleName) {
        List<AtomTimeKey> result = new ArrayList<>();
        for (Map.Entry<AtomTimeKey, DerivationInfo> entry : derivations.entrySet()) {
            if (entry.getValue().getRuleName().equals(ruleName)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Returns statistics about derivations.
     *
     * @return map of rule names to count of facts derived
     */
    public Map<String, Integer> getDerivationStats() {
        Map<String, Integer> stats = new HashMap<>();
        for (DerivationInfo info : derivations.values()) {
            stats.merge(info.getRuleName(), 1, Integer::sum);
        }
        return stats;
    }

    /**
     * Key for indexing facts by atom and time.
     */
    public static class AtomTimeKey {
        final Atom atom;
        final int time;

        public AtomTimeKey(Atom atom, int time) {
            this.atom = atom;
            this.time = time;
        }

        public Atom getAtom() {
            return atom;
        }

        public int getTime() {
            return time;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AtomTimeKey that = (AtomTimeKey) o;
            return time == that.time && atom.equals(that.atom);
        }

        @Override
        public int hashCode() {
            return 31 * atom.hashCode() + time;
        }

        @Override
        public String toString() {
            return "t=" + time + ": " + atom;
        }
    }
}
