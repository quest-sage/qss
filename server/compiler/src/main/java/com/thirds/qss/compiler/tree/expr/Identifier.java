package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.compiler.tree.NameLiteral;

public class Identifier extends Expression {
    private final NameLiteral name;

    public Identifier(NameLiteral name) {
        super(name.getRange());
        this.name = name;
    }
}
