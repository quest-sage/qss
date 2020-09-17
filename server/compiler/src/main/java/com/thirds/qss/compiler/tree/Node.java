package com.thirds.qss.compiler.tree;

import com.thirds.qss.compiler.Range;

/**
 * The tree package represents a type-safe abstract syntax tree.
 * A Node is any element of this tree.
 */
public class Node {
    private final Range range;

    public Node(Range range) {
        this.range = range;
    }

    public Range getRange() {
        return range;
    }

    @Override
    public String toString() {
        return range.toString();
    }
}
