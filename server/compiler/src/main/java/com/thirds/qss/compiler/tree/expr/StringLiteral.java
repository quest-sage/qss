package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.lexer.TokenType;

public class StringLiteral extends Expression {
    private final Token string;

    public StringLiteral(Token string) {
        super(string.getRange());
        if (string.type != TokenType.STRING_LITERAL)
            throw new UnsupportedOperationException();
        this.string = string;
    }
}
