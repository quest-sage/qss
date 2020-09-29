package com.thirds.qss.compiler.tree.script;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.tree.Type;

import java.util.function.Consumer;

public class Func extends FuncOrHook {
    private final Token name;

    public Func(Range range, VariableType.Function.Purity purity, Token name, ParamList paramList, Type returnType, FuncBlock funcBlock) {
        super(range, purity, paramList, returnType, funcBlock);
        this.name = name;
    }

    public Token getName() {
        return name;
    }
}
