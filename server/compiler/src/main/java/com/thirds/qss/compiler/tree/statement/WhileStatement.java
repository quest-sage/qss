package com.thirds.qss.compiler.tree.statement;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.tree.expr.Expression;

public class WhileStatement extends Statement {
    private final Expression condition;
    private final CompoundStatement block;

    public WhileStatement(Range totalRange, Expression condition, CompoundStatement block) {
        super(totalRange);
        this.condition = condition;
        this.block = block;
    }

    public Expression getCondition() {
        return condition;
    }

    public CompoundStatement getBlock() {
        return block;
    }
}
