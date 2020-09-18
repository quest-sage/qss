package com.thirds.qss.compiler.tree;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;

import java.util.function.Consumer;

/**
 * Represents a field of a struct.
 */
public class Field extends Node {
    private final Token name;
    private final Type type;

    public Field(Range range, Token name, Type type) {
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
