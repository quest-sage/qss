package com.thirds.qss.compiler.tree.script;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.tree.Type;

import java.util.function.Consumer;

public class Func extends Node {
    private final Token name;
    private final ParamList paramList;
    /**
     * May be null if we do not return a value.
     */
    private final Type returnType;
    private final FuncBlock funcBlock;

    public Func(Range range, Token name, ParamList paramList, Type returnType, FuncBlock funcBlock) {
        super(range);
        this.name = name;
        this.paramList = paramList;
        this.returnType = returnType;
        this.funcBlock = funcBlock;
    }

    public Token getName() {
        return name;
    }

    public ParamList getParamList() {
        return paramList;
    }

    public Type getReturnType() {
        return returnType;
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
