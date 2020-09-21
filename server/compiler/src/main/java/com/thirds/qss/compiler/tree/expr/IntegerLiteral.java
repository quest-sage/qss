package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.lexer.TokenType;

public class IntegerLiteral extends Expression {
    private final Token integer;

    public IntegerLiteral(Token integer) {
        super(integer.getRange());
        if (integer.type != TokenType.INTEGER_LITERAL)
            throw new UnsupportedOperationException();
        this.integer = integer;
    }
}
