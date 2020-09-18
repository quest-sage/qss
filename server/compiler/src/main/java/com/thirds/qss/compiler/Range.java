package com.thirds.qss.compiler;

import java.util.Objects;

public final class Range {
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

    /**
     * Returns a range encompassing both ranges. This may be larger than the union of both ranges, if there is space
     * in between the two ranges.
     */
    public static Range combine(Range a, Range b) {
        Position newStart, newEnd;

        if (a.start.compareTo(b.start) < 0)
            newStart = a.start;
        else
            newStart = b.start;

        if (a.end.compareTo(b.end) > 0)
            newEnd = a.end;
        else
            newEnd = b.end;

        return new Range(newStart, newEnd);
    }

    @Override
    public String toString() {
        return start + "-" + end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Range range = (Range) o;
        return start.equals(range.start) &&
                end.equals(range.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    public boolean contains(Position position) {
        return start.compareTo(position) <= 0 && end.compareTo(position) >= 0;
    }
}
