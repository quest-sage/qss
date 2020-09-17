package com.thirds.qss.compiler.tree;

import com.thirds.qss.compiler.Range;

public class Identifier extends Node {
    private final String name;

    public Identifier(Range range, String name) {
        super(range);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
