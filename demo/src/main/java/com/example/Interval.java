package com.example;

public class Interval {
    private final int start;
    private final int end;  // inclusive

    public Interval(int start, int end) {
        if (end < start) {
            throw new IllegalArgumentException("end < start: " + start + ".." + end);
        }
        this.start = start;
        this.end = end;
    }

    public int getStart() { return start; }
    public int getEnd()   { return end; }

    public boolean contains(int t) {
        return t >= start && t <= end;
    }

    @Override
    public String toString() {
        return "[" + start + "," + end + "]";
    }
}
