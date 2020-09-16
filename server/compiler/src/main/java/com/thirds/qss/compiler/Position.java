package com.thirds.qss.compiler;

public class Position {
    /**
     * Zero-indexed line and column numbers.
     */
    public int line, character;

    public Position(int line, int character) {
        this.line = line;
        this.character = character;
    }

    @Override
    public String toString() {
        return (line+1) + ":" + (character+1);
    }

    public Position copy() {
        return new Position(line, character);
    }
}
