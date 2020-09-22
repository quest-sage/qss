package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.tree.Node;

import java.util.function.Consumer;

public abstract class BinaryExpression extends Expression {
    private final Expression left;
    private final Expression right;

    public BinaryExpression(Expression left, Expression right) {
        super(Range.combine(left.getRange(), right.getRange()));
        this.left = left;
        this.right = right;
    }

    public Expression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        consumer.accept(left);
        consumer.accept(right);
    }
}
