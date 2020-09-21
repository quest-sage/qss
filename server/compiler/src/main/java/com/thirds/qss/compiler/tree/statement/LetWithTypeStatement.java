package com.thirds.qss.compiler.tree.statement;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.tree.Type;

/**
 * <code>let name: type;</code>
 */
public class LetWithTypeStatement extends Statement {
    private final Token name;
    private final Type type;

    public LetWithTypeStatement(Range range, Token name, Type type) {
        super(range);
        this.name = name;
        this.type = type;
    }

    public Token getName() {
        return name;
    }

    public Type getType() {
        return type;
    }
}
