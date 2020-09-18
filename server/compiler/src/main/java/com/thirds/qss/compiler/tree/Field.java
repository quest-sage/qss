package com.thirds.qss.compiler.tree;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;

/**
 * Represents a field of a struct.
 */
public class Field extends Node {
    public Field(Range range, Token name, Type type) {
        super(range);
    }
}
