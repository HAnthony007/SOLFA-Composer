package com.demo51.demo51;

public class Tempo {
    private String name;

    public Tempo(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String setName(String name) {
        this.name = name;
        return name;
    }

    public String toString() {
        return this.name;
    }
}