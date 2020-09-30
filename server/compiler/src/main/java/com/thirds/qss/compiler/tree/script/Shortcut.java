package com.thirds.qss.compiler.tree.script;

import com.thirds.qss.ShortcutKey;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.tree.Node;

public class Shortcut extends Node {
    private final ShortcutKey shortcutKey;

    public Shortcut(Range range, ShortcutKey shortcutKey) {
        super(range);
        this.shortcutKey = shortcutKey;
    }

    public ShortcutKey getShortcutKey() {
        return shortcutKey;
    }
}
