package com.example;

import java.util.*;

public class Reasoner {

    private final List<Rule> rules = new ArrayList<>();
    private final List<TimedFact> baseFacts = new ArrayList<>();
    private final Provenance provenance = new Provenance();

    public void addRule(Rule r)  { rules.add(r); }
    public void addFact(TimedFact f) { baseFacts.add(f); }

    public ReasoningInterpretation reason(int timesteps) {
        // factsAtTime[t] = all atoms true at time t
        List<Set<Atom>> factsAtTime = new ArrayList<>();
        for (int t = 0; t <= timesteps; t++) {
            factsAtTime.add(new HashSet<>());
        }

        // 1) Load base facts
        /*for (TimedFact f : baseFacts) {
            for (int t = f.getStart(); t <= f.getEnd() && t <= timesteps; t++) {
                factsAtTime.get(t).add(f.getAtom());
            }
        }*/
       for (TimedFact f : baseFacts) {
    for (Interval iv : f.getIntervals()) {
        int start = Math.max(0, iv.getStart());
        int end   = Math.min(timesteps, iv.getEnd());
        for (int t = start; t <= end; t++) {
            factsAtTime.get(t).add(f.getAtom());
        }
    }
}

        // 2) Forward chaining across time
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int t = 0; t <= timesteps; t++) {
                for (Rule r : rules) {
                    if (!r.isActiveAt(t)) continue;
                    int baseTime = t + r.getDelay();
                    if (baseTime > timesteps) continue;

                    List<Map<String,String>> subsList =
                            findAllSubstitutionsWithNegation(r.getBodyLiterals(), factsAtTime.get(t));

                    for (Map<String,String> theta : subsList) {
                        Atom headPattern = Atom.parse(r.getHead());
                        Atom headGrounded = applySubstitution(headPattern, theta);
                        int startOffset = r.getHeadStartOffset();
                        int endOffset   = r.getHeadEndOffset();

                        for (int dt = startOffset; dt <= endOffset; dt++) {
                            int tt = baseTime + dt;             // actual time where head is true
                        if (tt < 0 || tt > timesteps) continue;
                            if (factsAtTime.get(tt).add(headGrounded)) {
                            changed = true;
                            System.out.println("t=" + tt
                                    + " inferred by " + r.getName()
                                    + ": " + headGrounded);

                            // Record provenance
                            List<Provenance.AtomTimeKey> sources = new ArrayList<>();
                            for (String bodyAtomStr : r.getBodyAtoms()) {
                                Atom bodyPattern = Atom.parse(bodyAtomStr);
                                Atom bodyGrounded = applySubstitution(bodyPattern, theta);
                                sources.add(new Provenance.AtomTimeKey(bodyGrounded, t));
                            }
                            DerivationInfo derivInfo = new DerivationInfo(r.getName(), sources, theta);
                            provenance.record(headGrounded, tt, derivInfo);
                        }
                    }
                    }
                }
            }
        }

        return new ReasoningInterpretation(factsAtTime, provenance);
    }

    // --- unification helpers (variables = lowercase) ---

    /**
     * Finds all substitutions that satisfy the body literals (with negation support).
     */
    private List<Map<String,String>> findAllSubstitutionsWithNegation(List<Literal> bodyLiterals, Set<Atom> factsAtTime) {
        List<Map<String,String>> results = new ArrayList<>();
        backtrackWithNegation(bodyLiterals, 0, factsAtTime, new HashMap<>(), results);
        return results;
    }

    private List<Map<String,String>> findAllSubstitutions(List<String> bodyStrs, Set<Atom> factsAtTime) {
        List<Atom> body = bodyStrs.stream().map(Atom::parse).toList();
        List<Map<String,String>> results = new ArrayList<>();
        backtrack(body, 0, factsAtTime, new HashMap<>(), results);
        return results;
    }

    /**
     * Backtracking with support for negated literals (Negation as Failure).
     */
    private void backtrackWithNegation(List<Literal> body,
                                       int idx,
                                       Set<Atom> factsAtTime,
                                       Map<String,String> current,
                                       List<Map<String,String>> results) {
        if (idx == body.size()) {
            results.add(new HashMap<>(current));
            return;
        }

        Literal literal = body.get(idx);
        Atom pattern = applySubstitution(literal.getAtom(), current);

        if (literal.isPositive()) {
            // Positive literal: must unify with at least one fact
            for (Atom fact : factsAtTime) {
                Map<String,String> newSubst = unify(pattern, fact, current);
                if (newSubst != null) {
                    backtrackWithNegation(body, idx + 1, factsAtTime, newSubst, results);
                }
            }
        } else {
            // Negative literal (Negation as Failure): must NOT unify with any fact
            boolean unifiesWithAny = false;
            for (Atom fact : factsAtTime) {
                if (unify(pattern, fact, current) != null) {
                    unifiesWithAny = true;
                    break;
                }
            }

            // If it doesn't unify with any fact, the negation succeeds
            if (!unifiesWithAny) {
                backtrackWithNegation(body, idx + 1, factsAtTime, current, results);
            }
            // Otherwise, negation fails, don't continue this branch
        }
    }

    private void backtrack(List<Atom> body,
                           int idx,
                           Set<Atom> factsAtTime,
                           Map<String,String> current,
                           List<Map<String,String>> results) {
        if (idx == body.size()) {
            results.add(new HashMap<>(current));
            return;
        }

        Atom pattern = applySubstitution(body.get(idx), current);

        for (Atom fact : factsAtTime) {
            Map<String,String> newSubst = unify(pattern, fact, current);
            if (newSubst != null) {
                backtrack(body, idx + 1, factsAtTime, newSubst, results);
            }
        }
    }

    private Map<String,String> unify(Atom pattern, Atom fact, Map<String,String> subst) {
        if (!pattern.getPredicate().equals(fact.getPredicate())) return null;
        if (pattern.arity() != fact.arity()) return null;

        Map<String,String> result = new HashMap<>(subst);
        for (int i = 0; i < pattern.arity(); i++) {
            String pArg = pattern.getArgs().get(i);
            String fArg = fact.getArgs().get(i);

            if (isVariable(pArg)) {
                String existing = result.get(pArg);
                if (existing == null) {
                    result.put(pArg, fArg);
                } else if (!existing.equals(fArg)) {
                    return null; // conflicting substitution
                }
            } else {
                if (!pArg.equals(fArg)) return null; // constant mismatch
            }
        }
        return result;
    }

    private boolean isVariable(String s) {
        return s.length() > 0 && Character.isLowerCase(s.charAt(0)); // x,y,z,...
    }

    private Atom applySubstitution(Atom atom, Map<String,String> subst) {
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

