package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.compiler.Range;

import java.util.ArrayList;

public class FunctionInvocationExpression extends Expression {
    public FunctionInvocationExpression(Expression function, ArrayList<Expression> args) {
        super(args.isEmpty() ? function.getRange() : Range.combine(function.getRange(), args.get(args.size() - 1).getRange()));
    }
}
