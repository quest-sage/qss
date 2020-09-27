package com.thirds.qss.compiler.tree.statement;

import com.thirds.qss.compiler.Range;

/**
 * Represents a "break" statement in a loop.
 */
public class BreakStatement extends Statement {
    public BreakStatement(Range range) {
        super(range);
    }
}
