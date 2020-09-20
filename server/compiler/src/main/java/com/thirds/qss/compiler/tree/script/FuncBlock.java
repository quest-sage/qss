package com.thirds.qss.compiler.tree.script;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.tree.statement.CompoundStatement;

public class FuncBlock extends Node {
    private final CompoundStatement block;

    /**
     * @param block If null, the statement is considered "native" and has a native Java implementation but no QSS
     *              implementation.
     */
    public FuncBlock(Range range, CompoundStatement block) {
        super(range);
        this.block = block;
    }

    public boolean isNative() {
        return block == null;
    }

    public CompoundStatement getBlock() {
        return block;
    }

    @Override
    public String toString() {
        return "FuncBlock{" +
                "block=" + block +
                '}';
    }
}
