package com.thirds.qss.compiler.tree.script;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.tree.NameLiteral;
import com.thirds.qss.compiler.tree.Node;

import java.util.function.Consumer;

/**
 * Represents a <code>before func</code> or <code>after func</code> hook.
 */
public class FuncHook extends Node {
    /**
     * When should the hook execute? Either KW_BEFORE or KW_AFTER.
     */
    private final Token time;
    private final NameLiteral name;
    private final ParamList paramList;
    private final FuncBlock funcBlock;

    public FuncHook(Range range, Token time, NameLiteral name, ParamList paramList, FuncBlock funcBlock) {
        super(range);
        this.time = time;
        this.name = name;
        this.paramList = paramList;
        this.funcBlock = funcBlock;
    }

    public Token getTime() {
        return time;
    }

    public NameLiteral getName() {
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
        consumer.accept(name);
        consumer.accept(paramList);
        consumer.accept(funcBlock);
    }
}
