package com.thirds.qss.compiler.tree.script;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.tree.Type;

import java.util.function.Consumer;

/**
 * Represents either a function or a function hook.
 */
public abstract class FuncOrHook extends Node {
    protected final ParamList paramList;
    /**
     * May be null if we do not return a value.
     */
    protected final Type returnType;
    protected final FuncBlock funcBlock;

    public FuncOrHook(Range range, ParamList paramList, Type returnType, FuncBlock funcBlock) {
        super(range);
        this.paramList = paramList;
        this.returnType = returnType;
        this.funcBlock = funcBlock;
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
        if (returnType != null)
            consumer.accept(returnType);
        if (funcBlock != null)  // only true if we're in a trait func
            consumer.accept(funcBlock);
    }
}
