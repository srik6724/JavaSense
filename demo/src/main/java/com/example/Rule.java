package com.example;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class Rule implements Serializable {
    private static final long serialVersionUID = 1L;

    private String raw;
    private String name;

    private String head;
    private int delay;
    private List<String> bodyAtoms;
    private List<Literal> bodyLiterals; // Support for negation
    private final List<Interval> activeIntervals;

    private int headStartOffset;
    private int headEndOffset;

    public Rule(String raw, String name) {
        this(raw, name, List.of()); // empty list = always active
    }

    // New constructor with explicit active intervals
    public Rule(String raw, String name, List<Interval> activeIntervals) {
        this.raw = raw;
        this.name = name;
        this.activeIntervals = List.copyOf(activeIntervals);

        raw = raw.trim(); 
        String[] parts = raw.split("<-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid rule syntax: " + raw);
        }

        this.head = parts[0].trim();
        int colonIdx = this.head.indexOf(":");

        int hStart = 0;
        int hEnd = 0;
        String headAtomStr; 

        if(colonIdx >= 0) {
            headAtomStr = this.head.substring(0, colonIdx).trim();
            String intervalPart = this.head.substring(colonIdx + 1).trim();

            if(intervalPart.startsWith("[")) {
                intervalPart = intervalPart.substring(1); 
            }
            if(intervalPart.endsWith("]")) {
                intervalPart = intervalPart.substring(0, intervalPart.length() - 1); 
            }

            String[] bounds = intervalPart.split(",");
            if (bounds.length == 2) {
                try {
                    hStart = Integer.parseInt(bounds[0].trim());
                    hEnd   = Integer.parseInt(bounds[1].trim());
                    if (hEnd < hStart) {
                        throw new IllegalArgumentException("Head interval end < start in rule: " + raw);
                    }
                } catch (NumberFormatException e) {
                    // Fallback: ignore malformed interval
                    System.err.println("Warning: could not parse head interval in rule: " + raw);
                    hStart = 0;
                    hEnd   = 0;
                }
            } else {
                System.err.println("Warning: head interval must have 2 bounds in rule: " + raw);
                hStart = 0;
                hEnd   = 0;
            }
        }
        else {
            headAtomStr = this.head;
            hStart = 0;
            hEnd   = 0;
        }

        
        this.head = headAtomStr;
        this.headStartOffset = hStart;
        this.headEndOffset   = hEnd;

        String right = parts[1].trim();

        String bodyPart;
        String[] delayAndBody = right.split("\\s+", 2);
        try {
            this.delay = Integer.parseInt(delayAndBody[0]);
            bodyPart = (delayAndBody.length > 1) ? delayAndBody[1].trim() : "";
        } catch (NumberFormatException e) {
            this.delay = 1;         // default delay
            bodyPart = right;
        }

        String[] rawAtoms = bodyPart.split("\\)\\s*,\\s*");
        this.bodyAtoms = Arrays.stream(rawAtoms)
                .map(String::trim)
                .map(s -> s.endsWith(")") ? s : s + ")")
                .toList();

        // Parse literals for negation support
        this.bodyLiterals = this.bodyAtoms.stream()
                .map(Literal::parse)
                .toList();
    }

    public List<Interval> getActiveIntervals() {
        return activeIntervals;
    }

    public boolean isActiveAt(int t) {
        if (activeIntervals == null || activeIntervals.isEmpty()) {
            return true; // always active
        }
        for (Interval iv : activeIntervals) {
            if (iv.contains(t)) return true;
        }
        return false;
    }

    /*public Rule(String raw, String name) {
    this.raw = raw;
    this.name = name;

    raw = raw.trim();
    String[] parts = raw.split("<-");

    if (parts.length != 2) {
        throw new IllegalArgumentException("Invalid rule syntax: " + raw);
    }

    this.head = parts[0].trim();

    String right = parts[1].trim();  // e.g. "1 popular(y), Friends(x,y)" OR "cool_car(x), cool_pet(x)"
    String bodyPart;

    String[] delayAndBody = right.split("\\s+", 2);
    try {
        // If first token is an integer, treat it as delay
        this.delay = Integer.parseInt(delayAndBody[0]);
        bodyPart = (delayAndBody.length > 1) ? delayAndBody[1].trim() : "";
    } catch (NumberFormatException e) {
        // No explicit delay → default to 1 (PyReason behavior)
        this.delay = 1;
        bodyPart = right;  // entire RHS is body
    }

    // Now split bodyPart into atoms safely (don’t break on commas inside parentheses)
    // Example bodyPart: "popular(y), Friends(x,y), owns(y,z), owns(x,z)"
    String[] rawAtoms = bodyPart.split("\\)\\s*,\\s*");
    this.bodyAtoms = Arrays.stream(rawAtoms)
            .map(String::trim)
            .map(s -> s.endsWith(")") ? s : s + ")")  // ensure each ends with ')'
            .toList();
}*/



    public String getRaw()        { return raw; }
    public String getName()       { return name; }
    public String getHead()       { return head; }
    public int getDelay()         { return delay; }
    public List<String> getBodyAtoms() { return bodyAtoms; }
    public List<Literal> getBodyLiterals() { return bodyLiterals; }

    public int getHeadStartOffset() { return headStartOffset; }
    public int getHeadEndOffset()   { return headEndOffset; }

    @Override
    public String toString() {
        return "Rule{" +
                "name='" + name + '\'' +
                ", raw='" + raw + '\'' +
                ", delay=" + delay +
                ", headInterval=[" + headStartOffset + "," + headEndOffset + "]" +
                ", activeIntervals=" + activeIntervals +
                '}';
    }
}

