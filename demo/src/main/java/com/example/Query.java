package com.example;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Query language for flexible querying of facts and rules in JavaSense.
 *
 * <p>Supports pattern matching with variables, time constraints, and filters.</p>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Query for all popular facts at time 5
 * Query q = Query.parse("popular(x)")
 *     .atTime(5);
 * List<QueryResult> results = q.execute(interpretation);
 *
 * // Query with time range and filters
 * Query q = Query.parse("friends(x, y)")
 *     .inTimeRange(0, 10)
 *     .withVariable("x", "Alice");
 * }</pre>
 */
public class Query {
    private Atom pattern;
    private Integer exactTime;
    private Integer minTime;
    private Integer maxTime;
    private Map<String, String> bindings = new HashMap<>();
    private List<Predicate<QueryResult>> filters = new ArrayList<>();

    /**
     * Creates a query from a pattern string.
     *
     * @param patternStr the pattern to match (e.g., "popular(x)")
     * @return a new Query object
     */
    public static Query parse(String patternStr) {
        Query q = new Query();
        q.pattern = Atom.parse(patternStr);
        return q;
    }

    /**
     * Restricts the query to a specific timestep.
     *
     * @param time the exact timestep to query
     * @return this query for chaining
     */
    public Query atTime(int time) {
        this.exactTime = time;
        return this;
    }

    /**
     * Restricts the query to a time range (inclusive).
     *
     * @param minTime minimum timestep (inclusive)
     * @param maxTime maximum timestep (inclusive)
     * @return this query for chaining
     */
    public Query inTimeRange(int minTime, int maxTime) {
        this.minTime = minTime;
        this.maxTime = maxTime;
        return this;
    }

    /**
     * Binds a variable to a specific value.
     *
     * @param variable the variable name (e.g., "x")
     * @param value the value to bind to
     * @return this query for chaining
     */
    public Query withVariable(String variable, String value) {
        this.bindings.put(variable, value);
        return this;
    }

    /**
     * Adds a custom filter predicate.
     *
     * @param filter predicate to filter results
     * @return this query for chaining
     */
    public Query withFilter(Predicate<QueryResult> filter) {
        this.filters.add(filter);
        return this;
    }

    /**
     * Executes the query against a reasoning interpretation.
     *
     * @param interpretation the reasoning result to query
     * @return list of matching query results
     */
    public List<QueryResult> execute(ReasoningInterpretation interpretation) {
        List<QueryResult> results = new ArrayList<>();

        int startTime = (exactTime != null) ? exactTime : (minTime != null ? minTime : 0);
        int endTime = (exactTime != null) ? exactTime :
                      (maxTime != null ? maxTime : interpretation.getMaxTime());

        for (int t = startTime; t <= endTime; t++) {
            Set<Atom> factsAtTime = interpretation.getFactsAt(t);
            if (factsAtTime == null) continue;

            for (Atom fact : factsAtTime) {
                Map<String, String> substitution = unify(pattern, fact);
                if (substitution != null) {
                    // Check if pre-bound variables match
                    boolean matches = true;
                    for (Map.Entry<String, String> binding : bindings.entrySet()) {
                        String boundValue = substitution.get(binding.getKey());
                        if (boundValue != null && !boundValue.equals(binding.getValue())) {
                            matches = false;
                            break;
                        }
                    }

                    if (matches) {
                        QueryResult result = new QueryResult(fact, t, substitution);

                        // Apply custom filters
                        boolean passesFilters = true;
                        for (Predicate<QueryResult> filter : filters) {
                            if (!filter.test(result)) {
                                passesFilters = false;
                                break;
                            }
                        }

                        if (passesFilters) {
                            results.add(result);
                        }
                    }
                }
            }
        }

        return results;
    }

    /**
     * Unifies a pattern with a fact, returning variable bindings if successful.
     *
     * @param pattern the query pattern with variables
     * @param fact the ground fact
     * @return map of variable bindings, or null if unification fails
     */
    private Map<String, String> unify(Atom pattern, Atom fact) {
        if (!pattern.getPredicate().equals(fact.getPredicate())) {
            return null;
        }
        if (pattern.arity() != fact.arity()) {
            return null;
        }

        Map<String, String> substitution = new HashMap<>();
        for (int i = 0; i < pattern.arity(); i++) {
            String pArg = pattern.getArgs().get(i);
            String fArg = fact.getArgs().get(i);

            if (isVariable(pArg)) {
                String existing = substitution.get(pArg);
                if (existing == null) {
                    substitution.put(pArg, fArg);
                } else if (!existing.equals(fArg)) {
                    return null;
                }
            } else {
                if (!pArg.equals(fArg)) {
                    return null;
                }
            }
        }

        return substitution;
    }

    private boolean isVariable(String s) {
        return s.length() > 0 && Character.isLowerCase(s.charAt(0));
    }

    /**
     * Returns all unique values bound to a specific variable across all results.
     *
     * @param results the query results
     * @param variable the variable name
     * @return set of unique values
     */
    public static Set<String> getUniqueBindings(List<QueryResult> results, String variable) {
        return results.stream()
                .map(r -> r.getBindings().get(variable))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Counts the number of results grouped by a variable.
     *
     * @param results the query results
     * @param variable the variable to group by
     * @return map of values to counts
     */
    public static Map<String, Long> countByVariable(List<QueryResult> results, String variable) {
        return results.stream()
                .map(r -> r.getBindings().get(variable))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()));
    }
}
