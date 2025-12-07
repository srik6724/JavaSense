package com.example;
public class Fact {
    private String text; 
    private String name;
    private int startTime;
    private int endTime;

    public Fact(String text, String name, int startTime, int endTime) {
        this.text = text;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getEndTime() {
        return endTime;
    }
}

