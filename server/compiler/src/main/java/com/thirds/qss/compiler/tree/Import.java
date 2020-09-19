package com.thirds.qss.compiler.tree;

import java.util.function.Consumer;

public class Import extends Node {
    public final NameLiteral packageName;

    public Import(NameLiteral packageName) {
        super(packageName.getRange());
        this.packageName = packageName;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        consumer.accept(packageName);
    }
}
