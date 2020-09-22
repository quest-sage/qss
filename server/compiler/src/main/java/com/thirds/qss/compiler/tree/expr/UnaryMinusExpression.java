package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;
import com.thirds.qss.compiler.type.VariableTracker;

public class UnaryMinusExpression extends UnaryExpression {
    public UnaryMinusExpression(Expression argument) {
        super(argument);
    }

    @Override
    protected VariableType deduceVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree) {
        throw new UnsupportedOperationException();
    }
}
