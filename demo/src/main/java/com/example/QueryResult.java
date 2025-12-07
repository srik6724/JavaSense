package com.example;

import java.util.Map;

/**
 * Represents a single query result with fact, time, and variable bindings.
 *
 * <p>QueryResult encapsulates a matched fact along with the timestep where it was
 * found and the variable substitutions that made the match successful.</p>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * // Query: popular(x) at time 5
 * // Match: popular(Mary)
 * QueryResult result = ...;
 * result.getFact();           // returns Atom: popular(Mary)
 * result.getTime();            // returns 5
 * result.getBindings();        // returns Map: {x -> Mary}
 * result.getBinding("x");      // returns "Mary"
 * }</pre>
 */
public class QueryResult {
    private final Atom fact;
    private final int time;
    private final Map<String, String> bindings;

    /**
     * Constructs a new QueryResult.
     *
     * @param fact the matched fact
     * @param time the timestep where the fact was found
     * @param bindings variable substitutions for this match
     */
    public QueryResult(Atom fact, int time, Map<String, String> bindings) {
        this.fact = fact;
        this.time = time;
        this.bindings = bindings;
    }

    /**
     * Gets the matched fact.
     *
     * @return the atom that matched the query pattern
     */
    public Atom getFact() {
        return fact;
    }

    /**
     * Gets the timestep where this fact was found.
     *
     * @return the timestep
     */
    public int getTime() {
        return time;
    }

    /**
     * Gets all variable bindings for this match.
     *
     * @return map of variable names to their bound values
     */
    public Map<String, String> getBindings() {
        return bindings;
    }

    /**
     * Gets the value bound to a specific variable.
     *
     * @param variable the variable name
     * @return the bound value, or null if variable is not bound
     */
    public String getBinding(String variable) {
        return bindings.get(variable);
    }

    @Override
    public String toString() {
        return String.format("t=%d: %s with bindings %s", time, fact, bindings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryResult that = (QueryResult) o;
        return time == that.time &&
               fact.equals(that.fact) &&
               bindings.equals(that.bindings);
    }

    @Override
    public int hashCode() {
        int result = fact.hashCode();
        result = 31 * result + time;
        result = 31 * result + bindings.hashCode();
        return result;
    }
}
