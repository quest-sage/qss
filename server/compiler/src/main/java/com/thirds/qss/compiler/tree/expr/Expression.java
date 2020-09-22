package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.tree.Node;

import java.util.Optional;

public class Expression extends Node {
    private VariableType variableType = null;

    public Expression(Range range) {
        super(range);
    }

    public Optional<VariableType> getVariableType() {
        return Optional.ofNullable(variableType);
    }

    public void setVariableType(VariableType variableType) {
        this.variableType = variableType;
    }

    public String renderVariableType() {
        if (variableType == null)
            return "<unknown>";
        return variableType.toString();
    }
}
