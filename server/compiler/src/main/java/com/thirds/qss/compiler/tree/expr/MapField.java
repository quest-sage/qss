package com.thirds.qss.compiler.tree.expr;

public class MapField {
    private final Expression key, value;

    public MapField(Expression key, Expression value) {
        this.key = key;
        this.value = value;
    }

    public Expression getKey() {
        return key;
    }

    public Expression getValue() {
        return value;
    }
}
