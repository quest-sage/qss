package com.thirds.qss.compiler.tree;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;

import java.util.function.Consumer;

public class Func extends Node {
    private final Token name;
    private final ParamList paramList;
    private final FuncBlock funcBlock;

    public Func(Range range, Token name, ParamList paramList, FuncBlock funcBlock) {
        super(range);
        this.name = name;
        this.paramList = paramList;
        this.funcBlock = funcBlock;
    }

    public Token getName() {
        return name;
    }

    public ParamList getParamList() {
        return paramList;
    }

    public FuncBlock getFuncBlock() {
        return funcBlock;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        consumer.accept(paramList);
        consumer.accept(funcBlock);
    }
}
