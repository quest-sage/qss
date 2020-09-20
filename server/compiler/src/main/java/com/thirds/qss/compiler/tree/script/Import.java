package com.thirds.qss.compiler.tree.script;

import com.thirds.qss.compiler.tree.NameLiteral;
import com.thirds.qss.compiler.tree.Node;

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
