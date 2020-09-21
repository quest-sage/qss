package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.tree.Node;

public class Expression extends Node {
    private VariableType variableType = null;

    public Expression(Range range) {
        super(range);
    }

    public VariableType getVariableType() {
        return variableType;
    }

    public void setVariableType(VariableType variableType) {
        this.variableType = variableType;
    }
}
