package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.tree.Node;

import java.util.function.Consumer;

public abstract class BinaryExpression extends Expression {
    private Expression left;
    private Expression right;

    public BinaryExpression(Expression left, Expression right) {
        super(Range.combine(left.getRange(), right.getRange()));
        this.left = left;
        this.right = right;
    }

    public Expression getLeft() {
        return left;
    }

    protected void setLeft(Expression left) {
        this.left = left;
    }

    public Expression getRight() {
        return right;
    }

    protected void setRight(Expression right) {
        this.right = right;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        consumer.accept(left);
        consumer.accept(right);
    }
}
