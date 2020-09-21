package com.thirds.qss.compiler.tree.statement;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.tree.Type;
import com.thirds.qss.compiler.tree.expr.Expression;

/**
 * <code>let name = rvalue;</code>
 */
public class LetAssignStatement extends Statement {
    private final Token name;
    private final Expression rvalue;

    public LetAssignStatement(Range range, Token name, Expression rvalue) {
        super(range);
        this.name = name;
        this.rvalue = rvalue;
    }

    public Token getName() {
        return name;
    }

    public Expression getRvalue() {
        return rvalue;
    }
}
