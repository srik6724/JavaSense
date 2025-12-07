package com.example;

import java.util.*;

/**
 * Contains information about how a fact was derived.
 *
 * <p>DerivationInfo records the rule used, the source facts that triggered the rule,
 * and the variable substitutions that were applied.</p>
 */
public class DerivationInfo {
    private final String ruleName;
    private final List<Provenance.AtomTimeKey> sourceFacts;
    private final Map<String, String> substitution;

    /**
     * Constructs a new DerivationInfo.
     *
     * @param ruleName the name of the rule that derived this fact
     * @param sourceFacts the facts used in the rule body
     * @param substitution the variable bindings used
     */
    public DerivationInfo(String ruleName, List<Provenance.AtomTimeKey> sourceFacts,
                         Map<String, String> substitution) {
        this.ruleName = ruleName;
        this.sourceFacts = new ArrayList<>(sourceFacts);
        this.substitution = new HashMap<>(substitution);
    }

    /**
     * Gets the name of the rule that derived this fact.
     *
     * @return the rule name
     */
    public String getRuleName() {
        return ruleName;
    }

    /**
     * Gets the source facts used in the derivation.
     *
     * @return list of (atom, time) keys for source facts
     */
    public List<Provenance.AtomTimeKey> getSourceFacts() {
        return Collections.unmodifiableList(sourceFacts);
    }

    /**
     * Gets the variable substitution used in the derivation.
     *
     * @return map of variable names to values
     */
    public Map<String, String> getSubstitution() {
        return Collections.unmodifiableMap(substitution);
    }

    @Override
    public String toString() {
        return String.format("DerivationInfo{rule=%s, sources=%s, subst=%s}",
                ruleName, sourceFacts, substitution);
    }
}
