package com.thirds.qss.compiler.tree;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.Ranged;

import java.util.function.Consumer;

/**
 * The tree package represents a type-safe abstract syntax tree.
 * A Node is any element of this tree.
 */
public class Node implements Ranged {
    private final Range range;

    public Node(Range range) {
        this.range = range;
    }

    @Override
    public Range getRange() {
        return range;
    }

    /**
     * Executes the given function for each direct <i>non-null</i> child of this node.
     * Should be overridden by subclasses.
     */
    public void forChildren(Consumer<Node> consumer) {}

    /**
     * Executes the given function for each direct and indirect child of this node.
     * This should never be overridden.
     */
    public final void forAllChildren(Consumer<Node> consumer) {
        forChildren(n -> {
            consumer.accept(n);
            n.forAllChildren(consumer);
        });
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + range;
    }
}
