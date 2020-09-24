package com.thirds.qss.compiler.tree.statement;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.tree.expr.Expression;
import com.thirds.qss.compiler.tree.expr.ResultExpression;

import java.util.function.Consumer;

/**
 * This kind of statement evaluates an rvalue and sets an lvalue to the supplied rvalue.
 */
public class AssignStatement extends Statement {
    private final Expression lvalue;
    private final Expression rvalue;

    /**
     * Did this assignment expression originally come from a desugared return statement?
     */
    private boolean isReturn = false;

    public AssignStatement(Expression lvalue, Expression rvalue) {
        super(Range.combine(lvalue.getRange(), rvalue.getRange()));
        this.lvalue = lvalue;
        this.rvalue = rvalue;
    }

    /**
     * Creates a desugared assignment expression.
     * <code>{ return e } -> { result = e }</code>
     */
    public static AssignStatement returnExpr(Token returnToken, Expression expr) {
        AssignStatement statement = new AssignStatement(new ResultExpression(returnToken.getRange()), expr);
        statement.isReturn = true;
        return statement;
    }

    public Expression getLvalue() {
        return lvalue;
    }

    public Expression getRvalue() {
        return rvalue;
    }

    public boolean isReturn() {
        return isReturn;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        consumer.accept(lvalue);
        consumer.accept(rvalue);
    }
}
