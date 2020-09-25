package com.thirds.qss.compiler.tree.statement;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.tree.expr.Expression;

public class IfStatement extends Statement {
    private final Expression condition;
    private final Statement trueBlock;
    private final Statement falseBlock;

    /**
     * @param falseBlock May be null.
     */
    public IfStatement(Expression condition, Statement trueBlock, Statement falseBlock) {
        super(falseBlock == null ?
                Range.combine(condition.getRange(), trueBlock.getRange()) :
                Range.combine(condition.getRange(), falseBlock.getRange()));
        this.condition = condition;
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
    }

    public Expression getCondition() {
        return condition;
    }

    public Statement getTrueBlock() {
        return trueBlock;
    }

    public Statement getFalseBlock() {
        return falseBlock;
    }
}
