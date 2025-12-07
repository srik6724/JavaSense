package com.example;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Atom {
    private final String predicate;   // e.g. "popular", "Friends", "owns"
    private final List<String> args;  // e.g. ["Mary"], ["John","Dog"]

    public Atom(String predicate, List<String> args) {
        this.predicate = predicate;
        this.args = List.copyOf(args);
    }

    public String getPredicate() { return predicate; }
    public List<String> getArgs() { return args; }
    public int arity() { return args.size(); }

    @Override
    public String toString() {
        return predicate + "(" + String.join(",", args) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Atom a)) return false;
        return predicate.equals(a.predicate) && args.equals(a.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate, args);
    }

    public static Atom parse(String text) {
    text = text.trim();

    int idxOpen = text.indexOf('(');
    int idxClose = text.lastIndexOf(')');

    // Case 1: no parentheses at all → treat whole thing as predicate, no args
    if (idxOpen == -1 && idxClose == -1) {
        return new Atom(text, List.of());
    }

    // Case 2: malformed atom (e.g. "popular(" or "popular)x(")
    if (idxOpen == -1 || idxClose == -1 || idxClose <= idxOpen) {
        throw new IllegalArgumentException("Invalid atom syntax: '" + text + "'");
    }

    String pred = text.substring(0, idxOpen).trim();
    String inside = text.substring(idxOpen + 1, idxClose).trim();

    if (inside.isEmpty()) {
        return new Atom(pred, List.of());
    }

    String[] parts = inside.split(",");
    return new Atom(pred,
            Arrays.stream(parts)
                  .map(String::trim)
                  .toList());
}

}

