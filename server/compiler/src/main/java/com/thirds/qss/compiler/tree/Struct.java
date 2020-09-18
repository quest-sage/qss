package com.thirds.qss.compiler.tree;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.lexer.TokenType;

public class Struct extends Node {
    private final Token name;

    public Struct(Range range, Token name) {
        super(range);
        this.name = name;
        if (this.name.type != TokenType.IDENTIFIER) {
            throw new UnsupportedOperationException("Struct name token " + name + " must have type " + TokenType.IDENTIFIER);
        }
    }

    public Token getName() {
        return name;
    }

    @Override
    public String toString() {
        return "struct " + name.contents + "@" + getRange();
    }
}
