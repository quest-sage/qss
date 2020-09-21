package com.thirds.qss.compiler.tree.statement;

import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.tree.expr.Expression;

import java.util.function.Consumer;

/**
 * This kind of statement simply evaluates an expression.
 */
public class EvaluateStatement extends Statement {
    private final Expression expr;

    public EvaluateStatement(Expression expr) {
        super(expr.getRange());
        this.expr = expr;
    }

    public Expression getExpr() {
        return expr;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        consumer.accept(expr);
    }
}
