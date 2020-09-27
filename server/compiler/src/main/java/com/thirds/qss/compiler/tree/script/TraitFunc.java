package com.thirds.qss.compiler.tree.script;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.tree.Type;

/**
 * Represents a function in a trait definition. This is unique in the sense that it has no function body.
 */
public class TraitFunc extends FuncOrHook {
    private final Token name;

    public TraitFunc(Range range, Token name, ParamList paramList, Type returnType) {
        super(range, paramList, returnType, null);
        this.name = name;
    }

    public Token getName() {
        return name;
    }
}
