package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;
import com.thirds.qss.compiler.type.VariableTracker;

/**
 * Refers to the built-in "this" variable.
 */
public class ThisExpression extends Expression {
    public ThisExpression(Range range) {
        super(range);
    }

    @Override
    protected VariableType deduceVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree) {
        return scopeTree.getVariableType("this").orElse(VariableType.Primitive.TYPE_VOID);
    }
}
