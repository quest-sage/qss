package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.compiler.tree.NameLiteral;

public class StructField {
    private final NameLiteral key;
    private final Expression value;

    public StructField(NameLiteral key, Expression value) {
        this.key = key;
        this.value = value;
    }

    public NameLiteral getKey() {
        return key;
    }

    public Expression getValue() {
        return value;
    }
}
