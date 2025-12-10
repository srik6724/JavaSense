package com.example;

import java.io.Serializable;

/**
 * Represents a literal in a rule body, which can be either positive or negated.
 *
 * <p>A literal is an atom that may be prefixed with 'not' or '~' to indicate negation.
 * This supports Negation as Failure (NAF) semantics.</p>
 *
 * <h2>Examples:</h2>
 * <pre>{@code
 * Literal pos = Literal.parse("popular(x)");        // positive
 * Literal neg = Literal.parse("not popular(x)");    // negated
 * Literal neg2 = Literal.parse("~popular(x)");      // negated (alt syntax)
 * }</pre>
 */
public class Literal implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Atom atom;
    private final boolean negated;

    /**
     * Constructs a new Literal.
     *
     * @param atom the atom
     * @param negated whether this literal is negated
     */
    public Literal(Atom atom, boolean negated) {
        this.atom = atom;
        this.negated = negated;
    }

    /**
     * Parses a literal from a string.
     *
     * <p>Supported formats:</p>
     * <ul>
     *   <li>positive: "popular(x)"</li>
     *   <li>negated: "not popular(x)" or "~popular(x)"</li>
     * </ul>
     *
     * @param str the string to parse
     * @return parsed literal
     */
    public static Literal parse(String str) {
        str = str.trim();

        // Check for "not " prefix
        if (str.startsWith("not ")) {
            String atomStr = str.substring(4).trim();
            return new Literal(Atom.parse(atomStr), true);
        }

        // Check for "~" prefix
        if (str.startsWith("~")) {
            String atomStr = str.substring(1).trim();
            return new Literal(Atom.parse(atomStr), true);
        }

        // Positive literal
        return new Literal(Atom.parse(str), false);
    }

    /**
     * Gets the atom of this literal.
     *
     * @return the atom
     */
    public Atom getAtom() {
        return atom;
    }

    /**
     * Checks if this literal is negated.
     *
     * @return true if negated
     */
    public boolean isNegated() {
        return negated;
    }

    /**
     * Checks if this literal is positive (not negated).
     *
     * @return true if positive
     */
    public boolean isPositive() {
        return !negated;
    }

    @Override
    public String toString() {
        return negated ? ("not " + atom) : atom.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Literal literal = (Literal) o;
        return negated == literal.negated && atom.equals(literal.atom);
    }

    @Override
    public int hashCode() {
        return 31 * atom.hashCode() + (negated ? 1 : 0);
    }
}
