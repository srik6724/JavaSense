package com.example;

import java.io.Serializable;
import java.util.List;

public class TimedFact implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Atom atom;
    private final String name;
    private final List<Interval> intervals;

    // Backward-compatible ctor: single interval
    public TimedFact(Atom atom, String name, int start, int end) {
        this(atom, name, List.of(new Interval(start, end)));
    }

    // New ctor: multiple intervals
    public TimedFact(Atom atom, String name, List<Interval> intervals) {
        this.atom = atom;
        this.name = name;
        this.intervals = List.copyOf(intervals);
    }

    public Atom getAtom()            { return atom; }
    public String getName()          { return name; }
    public List<Interval> getIntervals() { return intervals; }

    public boolean isTrueAt(int t) {
        for (Interval iv : intervals) {
            if (iv.contains(t)) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "TimedFact{" + atom + ", intervals=" + intervals + "}";
    }
}
