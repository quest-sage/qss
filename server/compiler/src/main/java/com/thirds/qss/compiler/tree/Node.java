package com.thirds.qss.compiler.tree;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.Ranged;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The tree package represents a type-safe abstract syntax tree.
 * A Node is any element of this tree.
 */
public class Node implements Ranged {
    /**
     * What node contains this node? Null if this is a top-level node, or it has not yet been calculated.
     * By calling updateAllContainers on a root node, this variable will be updated on all children recursively.
     */
    private Node container;

    private final Range range;

    public Node(Range range) {
        this.range = range;
    }

    @Override
    public Range getRange() {
        return range;
    }

    /**
     * Call this on the root node once its contents are completely created.
     * This will update all 'container' variables in all children recursively to be correct.
     */
    protected void updateAllContainers() {
        forChildren(node -> {
            node.container = this;
            node.updateAllContainers();
        });
    }

    public Node getContainer() {
        return container;
    }

    /**
     * Recursively goes to parent containers to find the container of the given type, if it exists.
     */
    @SuppressWarnings("unchecked")
    public <T extends Node> Optional<T> getContainerOfType(Class<T> clazz) {
        if (clazz.isInstance(this)) {
            return Optional.of((T) this);
        }
        if (container == null)
            return Optional.empty();
        return container.getContainerOfType(clazz);
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
