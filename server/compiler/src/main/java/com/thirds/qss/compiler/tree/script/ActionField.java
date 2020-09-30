package com.thirds.qss.compiler.tree.script;

import com.thirds.qss.compiler.tree.Node;

import java.util.function.Consumer;

/**
 * Represents a field of an action.
 * This could be any kind of useful information about the action itself.
 */
public class ActionField extends Node {
    private final Shortcut shortcut;

    public ActionField(Shortcut shortcut) {
        super(shortcut.getRange());
        this.shortcut = shortcut;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        if (shortcut != null)
            consumer.accept(shortcut);
    }

    public Shortcut getShortcut() {
        return shortcut;
    }
}
