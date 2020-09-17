package com.thirds.qss.compiler.tree;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexing.Token;

/**
 * Represents a data type.
 */
public class Type extends Node {
    public Type(Range range) {
        super(range);
    }

    public static class StructType extends Type {
        private final NameLiteral structName;

        public StructType(Range range, NameLiteral structName) {
            super(range);
            this.structName = structName;
        }
    }
}
