package com.thirds.qss.compiler;

import java.util.Objects;

public final class Position implements Comparable<Position> {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return line == position.line &&
                character == position.character;
    }

    @Override
    public int hashCode() {
        return Objects.hash(line, character);
    }

    @Override
    public int compareTo(Position o) {
        int compare = Integer.compare(line, o.line);
        if (compare != 0)
            return compare;
        return Integer.compare(character, o.character);
    }
}
