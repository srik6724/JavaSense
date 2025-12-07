package com.example;

import java.util.*;

/**
 * Represents a tree structure showing how a fact was derived recursively.
 *
 * <p>Each node in the tree represents a fact, and its children are the facts
 * that were used to derive it. Base facts (non-derived) are leaf nodes.</p>
 */
public class DerivationTree {
    private final Atom atom;
    private final int time;
    private final DerivationInfo derivationInfo; // null for base facts
    private final List<DerivationTree> children;

    /**
     * Constructs a new DerivationTree node.
     *
     * @param atom the fact at this node
     * @param time the timestep
     * @param derivationInfo how this fact was derived (null if base fact)
     * @param children the facts used to derive this one
     */
    public DerivationTree(Atom atom, int time, DerivationInfo derivationInfo,
                         List<DerivationTree> children) {
        this.atom = atom;
        this.time = time;
        this.derivationInfo = derivationInfo;
        this.children = new ArrayList<>(children);
    }

    /**
     * Gets the fact at this node.
     *
     * @return the atom
     */
    public Atom getAtom() {
        return atom;
    }

    /**
     * Gets the timestep for this fact.
     *
     * @return the time
     */
    public int getTime() {
        return time;
    }

    /**
     * Gets the derivation info for this fact.
     *
     * @return derivation info, or null if this is a base fact
     */
    public DerivationInfo getDerivationInfo() {
        return derivationInfo;
    }

    /**
     * Checks if this is a base fact (not derived).
     *
     * @return true if this is a base fact
     */
    public boolean isBaseFact() {
        return derivationInfo == null;
    }

    /**
     * Gets the child nodes (facts used to derive this one).
     *
     * @return list of child nodes
     */
    public List<DerivationTree> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Gets the depth of this tree (longest path to a leaf).
     *
     * @return tree depth
     */
    public int getDepth() {
        if (children.isEmpty()) {
            return 1;
        }
        int maxChildDepth = 0;
        for (DerivationTree child : children) {
            maxChildDepth = Math.max(maxChildDepth, child.getDepth());
        }
        return 1 + maxChildDepth;
    }

    /**
     * Gets all base facts (leaves) in this tree.
     *
     * @return set of base fact (atom, time) pairs
     */
    public Set<Provenance.AtomTimeKey> getBaseFacts() {
        Set<Provenance.AtomTimeKey> baseFacts = new HashSet<>();
        collectBaseFacts(baseFacts);
        return baseFacts;
    }

    private void collectBaseFacts(Set<Provenance.AtomTimeKey> baseFacts) {
        if (isBaseFact()) {
            baseFacts.add(new Provenance.AtomTimeKey(atom, time));
        } else {
            for (DerivationTree child : children) {
                child.collectBaseFacts(baseFacts);
            }
        }
    }

    /**
     * Displays the tree in a human-readable format.
     *
     * @return formatted tree string
     */
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        buildTreeString(sb, "", true);
        return sb.toString();
    }

    private void buildTreeString(StringBuilder sb, String prefix, boolean isTail) {
        sb.append(prefix);
        sb.append(isTail ? "└── " : "├── ");
        sb.append("t=").append(time).append(": ").append(atom);
        if (derivationInfo != null) {
            sb.append(" [").append(derivationInfo.getRuleName()).append("]");
        }
        sb.append("\n");

        for (int i = 0; i < children.size(); i++) {
            boolean isLast = (i == children.size() - 1);
            children.get(i).buildTreeString(sb,
                    prefix + (isTail ? "    " : "│   "),
                    isLast);
        }
    }

    @Override
    public String toString() {
        return String.format("t=%d: %s (depth=%d, children=%d)",
                time, atom, getDepth(), children.size());
    }
}
