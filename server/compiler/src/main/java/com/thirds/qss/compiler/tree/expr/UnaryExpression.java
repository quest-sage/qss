package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.compiler.tree.Node;

import java.util.function.Consumer;

public abstract class UnaryExpression extends Expression {
    private final Expression argument;

    public UnaryExpression(Expression argument) {
        super(argument.getRange());
        this.argument = argument;
    }

    public Expression getArgument() {
        return argument;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        consumer.accept(argument);
    }
}
