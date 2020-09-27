package com.thirds.qss.compiler.tree.script;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.tree.Type;

import java.util.function.Consumer;

/**
 * Represents a parameter of a func/hook.
 */
public class Param extends Node {
    private final Token name;
    private final Type type;

    /**
     * @param name May be IDENTIFIER or KW_THIS.
     */
    public Param(Range range, Token name, Type type) {
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

    @Override
    public void forChildren(Consumer<Node> consumer) {
        consumer.accept(type);
    }
}
