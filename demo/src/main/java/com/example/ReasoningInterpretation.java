package com.example;

import java.util.List;
import java.util.Set;

/**
 * Stores the results of temporal reasoning across multiple timesteps.
 *
 * <p>A ReasoningInterpretation organizes derived facts by timestep, allowing
 * queries for facts that are true at specific points in time.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ReasoningInterpretation result = JavaSense.reason(graph, 10);
 *
 * // Get all facts at timestep 5
 * Set<Atom> factsAtT5 = result.getFactsAt(5);
 *
 * // Display all facts across all timesteps
 * result.display();
 * }</pre>
 *
 * @see JavaSense#reason(Graph, int)
 * @see Atom
 */
public class ReasoningInterpretation {
    private final List<Set<Atom>> factsByTime; // index = timestep
    private final Provenance provenance;

    /**
     * Constructs a new ReasoningInterpretation.
     *
     * @param factsByTime list of fact sets, indexed by timestep
     */
    public ReasoningInterpretation(List<Set<Atom>> factsByTime) {
        this.factsByTime = factsByTime;
        this.provenance = new Provenance();
    }

    /**
     * Constructs a new ReasoningInterpretation with provenance tracking.
     *
     * @param factsByTime list of fact sets, indexed by timestep
     * @param provenance provenance information for derived facts
     */
    public ReasoningInterpretation(List<Set<Atom>> factsByTime, Provenance provenance) {
        this.factsByTime = factsByTime;
        this.provenance = provenance;
    }

    /**
     * Retrieves all facts that are true at a specific timestep.
     *
     * @param t the timestep to query (must be within valid range)
     * @return set of atoms (facts) true at timestep t
     * @throws IndexOutOfBoundsException if t is out of bounds
     */
    public Set<Atom> getFactsAt(int t) {
        return factsByTime.get(t);
    }

    /**
     * Returns the maximum timestep in this interpretation.
     *
     * @return the highest timestep index, or 0 if empty
     */
    public int getMaxTime() {
        return factsByTime.isEmpty() ? 0 : factsByTime.size() - 1;
    }

    /**
     * Gets the provenance tracker for this interpretation.
     *
     * @return the provenance object
     */
    public Provenance getProvenance() {
        return provenance;
    }

    /**
     * Explains how a specific fact was derived.
     *
     * @param atom the fact to explain
     * @param time the timestep
     * @return human-readable explanation
     */
    public String explain(Atom atom, int time) {
        return provenance.explain(atom, time);
    }

    /**
     * Gets the derivation tree for a fact.
     *
     * @param atom the fact to trace
     * @param time the timestep
     * @return the derivation tree
     */
    public DerivationTree getDerivationTree(Atom atom, int time) {
        return provenance.getDerivationTree(atom, time);
    }

    /**
     * Checks if a fact was derived (vs. being a base fact).
     *
     * @param atom the fact to check
     * @param time the timestep
     * @return true if derived by a rule
     */
    public boolean isDerived(Atom atom, int time) {
        return provenance.isDerived(atom, time);
    }

    /**
     * Displays all facts across all timesteps to standard output.
     *
     * <p>Output format:</p>
     * <pre>
     * t = 0
     *   popular(Mary)
     *   Friends(Mary,John)
     * t = 1
     *   popular(John)
     * ...
     * </pre>
     */
    public void display() {
        for (int t = 0; t < factsByTime.size(); t++) {
            System.out.println("t = " + t);
            for (Atom a : factsByTime.get(t)) {
                System.out.println("  " + a);
            }
        }
    }

    /**
     * Displays all facts with their derivation information.
     */
    public void displayWithProvenance() {
        for (int t = 0; t < factsByTime.size(); t++) {
            System.out.println("t = " + t);
            for (Atom a : factsByTime.get(t)) {
                if (provenance.isDerived(a, t)) {
                    System.out.println("  " + a + " [derived by " +
                            provenance.getDerivation(a, t).getRuleName() + "]");
                } else {
                    System.out.println("  " + a + " [base fact]");
                }
            }
        }
    }
}

