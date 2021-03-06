package com.thirds.qss.compiler.tree.script;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.tree.NameLiteral;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.tree.Type;
import com.thirds.qss.compiler.tree.expr.Identifier;

import java.util.function.Consumer;

/**
 * Represents a <code>before func</code> or <code>after func</code> hook.
 */
public class FuncHook extends FuncOrHook {
    /**
     * When should the hook execute? Either KW_BEFORE or KW_AFTER.
     */
    private final Token time;
    private final Identifier name;

    public FuncHook(Range range, VariableType.Function.Purity purity, Token time, NameLiteral name, ParamList paramList, Type returnType, FuncBlock funcBlock) {
        super(range, purity, paramList, returnType, funcBlock);
        this.time = time;
        this.name = new Identifier(name);
    }

    public Token getTime() {
        return time;
    }

    public Identifier getName() {
        return name;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        super.forChildren(consumer);
        consumer.accept(name);
    }
}
