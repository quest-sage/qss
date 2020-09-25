package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.tree.Node;

import java.util.function.Consumer;

public abstract class UnaryExpression extends Expression {
    private final Expression argument;

    public UnaryExpression(Range range, Expression argument) {
        super(range);
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
