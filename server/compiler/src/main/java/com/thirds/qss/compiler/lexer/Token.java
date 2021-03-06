package com.thirds.qss.compiler.lexer;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.Ranged;

public class Token implements Ranged {
    public final TokenType type;
    public final String contents;
    private final Range range;

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
                ", range=" + getRange() +
                '}';
    }

    @Override
    public Range getRange() {
        return range;
    }
}
