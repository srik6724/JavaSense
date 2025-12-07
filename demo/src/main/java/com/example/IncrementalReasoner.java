package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Supports incremental reasoning - adding new facts and re-reasoning efficiently.
 *
 * <p>Instead of re-running the entire reasoning process from scratch,
 * incremental reasoning only processes new facts and their consequences.</p>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * IncrementalReasoner reasoner = new IncrementalReasoner();
 * reasoner.addRule(rule1);
 * reasoner.addRule(rule2);
 *
 * // Initial reasoning
 * reasoner.addFact(new TimedFact(...));
 * ReasoningInterpretation result1 = reasoner.reason(10);
 *
 * // Add new fact and incrementally update
 * reasoner.addFact(new TimedFact(...));
 * ReasoningInterpretation result2 = reasoner.incrementalReason();
 * }</pre>
 */
public class IncrementalReasoner {
    private static final Logger logger = LoggerFactory.getLogger(IncrementalReasoner.class);

    private final List<Rule> rules = new ArrayList<>();
    private final List<TimedFact> allFacts = new ArrayList<>();
    private final Set<TimedFact> newFacts = new HashSet<>();
    private final Provenance provenance = new Provenance();

    private List<Set<Atom>> currentFactsByTime = new ArrayList<>();
    private int maxTimesteps = 0;
    private boolean hasReasoned = false;

    /**
     * Adds a rule to the reasoner.
     *
     * @param rule the rule to add
     */
    public void addRule(Rule rule) {
        rules.add(rule);
    }

    /**
     * Adds a new fact to the knowledge base.
     *
     * @param fact the fact to add
     */
    public void addFact(TimedFact fact) {
        allFacts.add(fact);
        newFacts.add(fact);
    }

    /**
     * Performs initial complete reasoning.
     *
     * @param timesteps number of timesteps
     * @return reasoning result
     */
    public ReasoningInterpretation reason(int timesteps) {
        maxTimesteps = timesteps;
        hasReasoned = true;
        newFacts.clear();

        // Delegate to standard reasoner
        Reasoner standardReasoner = new Reasoner();
        for (Rule rule : rules) {
            standardReasoner.addRule(rule);
        }
        for (TimedFact fact : allFacts) {
            standardReasoner.addFact(fact);
        }

        ReasoningInterpretation result = standardReasoner.reason(timesteps);

        // Cache the results
        currentFactsByTime = new ArrayList<>();
        for (int t = 0; t <= timesteps; t++) {
            currentFactsByTime.add(new HashSet<>(result.getFactsAt(t)));
        }

        logger.info("Initial reasoning completed with {} facts", allFacts.size());
        return result;
    }

    /**
     * Performs incremental reasoning after new facts have been added.
     *
     * <p>Only processes the new facts and their consequences, reusing existing derivations.</p>
     *
     * @return updated reasoning result
     */
    public ReasoningInterpretation incrementalReason() {
        if (!hasReasoned) {
            throw new IllegalStateException("Must call reason() before incrementalReason()");
        }

        if (newFacts.isEmpty()) {
            logger.info("No new facts to process incrementally");
            return new ReasoningInterpretation(currentFactsByTime, provenance);
        }

        logger.info("Starting incremental reasoning with {} new facts", newFacts.size());

        // Add new base facts to the current state
        for (TimedFact newFact : newFacts) {
            for (Interval iv : newFact.getIntervals()) {
                int start = Math.max(0, iv.getStart());
                int end = Math.min(maxTimesteps, iv.getEnd());
                for (int t = start; t <= end; t++) {
                    currentFactsByTime.get(t).add(newFact.getAtom());
                }
            }
        }

        // Track which timesteps have changed
        Set<Integer> changedTimesteps = new HashSet<>();
        for (TimedFact newFact : newFacts) {
            for (Interval iv : newFact.getIntervals()) {
                int start = Math.max(0, iv.getStart());
                int end = Math.min(maxTimesteps, iv.getEnd());
                for (int t = start; t <= end; t++) {
                    changedTimesteps.add(t);
                }
            }
        }

        // Propagate changes forward in time
        boolean changed = true;
        int iteration = 0;
        while (changed && iteration < 100) { // Safety limit
            changed = false;
            iteration++;

            for (int t : new ArrayList<>(changedTimesteps)) {
                for (Rule r : rules) {
                    if (!r.isActiveAt(t)) continue;
                    int baseTime = t + r.getDelay();
                    if (baseTime > maxTimesteps) continue;

                    List<Map<String, String>> subsList =
                        findAllSubstitutionsWithNegation(r.getBodyLiterals(), currentFactsByTime.get(t));

                    for (Map<String, String> theta : subsList) {
                        Atom headPattern = Atom.parse(r.getHead());
                        Atom headGrounded = applySubstitution(headPattern, theta);
                        int startOffset = r.getHeadStartOffset();
                        int endOffset = r.getHeadEndOffset();

                        for (int dt = startOffset; dt <= endOffset; dt++) {
                            int tt = baseTime + dt;
                            if (tt < 0 || tt > maxTimesteps) continue;

                            if (currentFactsByTime.get(tt).add(headGrounded)) {
                                changed = true;
                                changedTimesteps.add(tt);

                                // Record provenance
                                List<Provenance.AtomTimeKey> sources = new ArrayList<>();
                                for (Literal lit : r.getBodyLiterals()) {
                                    if (lit.isPositive()) {
                                        Atom bodyGrounded = applySubstitution(lit.getAtom(), theta);
                                        sources.add(new Provenance.AtomTimeKey(bodyGrounded, t));
                                    }
                                }
                                DerivationInfo derivInfo = new DerivationInfo(r.getName(), sources, theta);
                                provenance.record(headGrounded, tt, derivInfo);
                            }
                        }
                    }
                }
            }
        }

        newFacts.clear();
        logger.info("Incremental reasoning completed in {} iterations, {} timesteps affected",
            iteration, changedTimesteps.size());

        return new ReasoningInterpretation(currentFactsByTime, provenance);
    }

    /**
     * Retracts a fact from the knowledge base and updates reasoning.
     *
     * <p>Note: This requires full re-reasoning as retraction can have
     * cascading effects that are hard to track incrementally.</p>
     *
     * @param fact the fact to retract
     * @return updated reasoning result
     */
    public ReasoningInterpretation retractFact(TimedFact fact) {
        allFacts.remove(fact);
        newFacts.remove(fact);

        logger.info("Fact retracted, performing full re-reasoning");
        return reason(maxTimesteps);
    }

    /**
     * Gets the current reasoning state.
     *
     * @return current interpretation
     */
    public ReasoningInterpretation getCurrentState() {
        if (!hasReasoned) {
            throw new IllegalStateException("No reasoning has been performed yet");
        }
        return new ReasoningInterpretation(currentFactsByTime, provenance);
    }

    /**
     * Clears all facts and resets the reasoner.
     */
    public void reset() {
        allFacts.clear();
        newFacts.clear();
        currentFactsByTime.clear();
        hasReasoned = false;
        maxTimesteps = 0;
    }

    // --- Unification helpers (copied from Reasoner) ---

    private List<Map<String, String>> findAllSubstitutionsWithNegation(
            List<Literal> bodyLiterals, Set<Atom> factsAtTime) {
        List<Map<String, String>> results = new ArrayList<>();
        backtrackWithNegation(bodyLiterals, 0, factsAtTime, new HashMap<>(), results);
        return results;
    }

    private void backtrackWithNegation(List<Literal> body,
                                       int idx,
                                       Set<Atom> factsAtTime,
                                       Map<String, String> current,
                                       List<Map<String, String>> results) {
        if (idx == body.size()) {
            results.add(new HashMap<>(current));
            return;
        }

        Literal literal = body.get(idx);
        Atom pattern = applySubstitution(literal.getAtom(), current);

        if (literal.isPositive()) {
            for (Atom fact : factsAtTime) {
                Map<String, String> newSubst = unify(pattern, fact, current);
                if (newSubst != null) {
                    backtrackWithNegation(body, idx + 1, factsAtTime, newSubst, results);
                }
            }
        } else {
            boolean unifiesWithAny = false;
            for (Atom fact : factsAtTime) {
                if (unify(pattern, fact, current) != null) {
                    unifiesWithAny = true;
                    break;
                }
            }
            if (!unifiesWithAny) {
                backtrackWithNegation(body, idx + 1, factsAtTime, current, results);
            }
        }
    }

    private Map<String, String> unify(Atom pattern, Atom fact, Map<String, String> subst) {
        if (!pattern.getPredicate().equals(fact.getPredicate())) return null;
        if (pattern.arity() != fact.arity()) return null;

        Map<String, String> result = new HashMap<>(subst);
        for (int i = 0; i < pattern.arity(); i++) {
            String pArg = pattern.getArgs().get(i);
            String fArg = fact.getArgs().get(i);

            if (isVariable(pArg)) {
                String existing = result.get(pArg);
                if (existing == null) {
                    result.put(pArg, fArg);
                } else if (!existing.equals(fArg)) {
                    return null;
                }
            } else {
                if (!pArg.equals(fArg)) return null;
            }
        }
        return result;
    }

    private boolean isVariable(String s) {
        return s.length() > 0 && Character.isLowerCase(s.charAt(0));
    }

    private Atom applySubstitution(Atom atom, Map<String, String> subst) {
        List<String> newArgs = new ArrayList<>();
        for (String arg : atom.getArgs()) {
            if (isVariable(arg) && subst.containsKey(arg)) {
                newArgs.add(subst.get(arg));
            } else {
                newArgs.add(arg);
            }
        }
        return new Atom(atom.getPredicate(), newArgs);
    }
}
