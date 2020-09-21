package com.thirds.qss.compiler.tree.statement;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.tree.expr.Expression;

/**
 * This kind of statement evaluates an rvalue and sets an lvalue to the supplied rvalue.
 */
public class AssignStatement extends Statement {
    private final Expression lvalue;
    private final Expression rvalue;

    public AssignStatement(Expression lvalue, Expression rvalue) {
        super(Range.combine(lvalue.getRange(), rvalue.getRange()));
        this.lvalue = lvalue;
        this.rvalue = rvalue;
    }
}
