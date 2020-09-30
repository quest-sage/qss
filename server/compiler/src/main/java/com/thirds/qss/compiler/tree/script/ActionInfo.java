package com.thirds.qss.compiler.tree.script;

/**
 * Represents all info a struct needs to have about itself to be considered an action.
 */
public class ActionInfo {
    private final Shortcut shortcut;

    public ActionInfo(Shortcut shortcut) {
        this.shortcut = shortcut;
    }

    public Shortcut getShortcut() {
        return shortcut;
    }
}
