package com.thirds.qss.compiler.tree.statement;

import com.thirds.qss.compiler.Range;

/**
 * Represents an empty return statement (i.e. without any return value). Return values are handled through the
 * "result" variable.
 */
public class ReturnStatement extends Statement {
    private final boolean returnedValue;

    /**
     * @param returnedValue True if (and only if) the return statement was part of a <code>return x</code> statement,
     *                      not just an empty <code>return</code> statement.
     */
    public ReturnStatement(Range range, boolean returnedValue) {
        super(range);
        this.returnedValue = returnedValue;
    }

    public boolean didReturnValue() {
        return returnedValue;
    }
}
