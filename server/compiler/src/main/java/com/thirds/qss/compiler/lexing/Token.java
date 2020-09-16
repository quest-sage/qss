package com.thirds.qss.compiler.lexing;

import com.thirds.qss.compiler.Range;

public class Token {
    public final TokenType type;
    public final String contents;
    public final Range range;

    public Token(TokenType type, String contents, Range range) {
        this.type = type;
        this.contents = contents;
        this.range = range;
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", contents='" + contents + '\'' +
                ", range=" + range +
                '}';
    }
}
