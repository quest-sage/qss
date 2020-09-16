package com.thirds.qss.compiler;

public class Range {
    public final Position start, end;

    public Range(Position where) {
        this.start = where.copy();
        this.end = where.copy();
        this.end.character++;
    }

    public Range(Position start, Position end) {
        this.start = start.copy();
        this.end = end.copy();
    }

    @Override
    public String toString() {
        return start + "-" + end;
    }
}
