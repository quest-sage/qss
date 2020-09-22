package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;
import com.thirds.qss.compiler.type.VariableTracker;

import java.util.ArrayList;

public class FunctionInvocationExpression extends Expression {
    public FunctionInvocationExpression(Expression function, ArrayList<Expression> args) {
        super(args.isEmpty() ? function.getRange() : Range.combine(function.getRange(), args.get(args.size() - 1).getRange()));
    }

    @Override
    protected VariableType deduceVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree) {
        throw new UnsupportedOperationException();
    }
}
