package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;
import com.thirds.qss.compiler.type.VariableTracker;

import java.util.Optional;

public abstract class Expression extends Node {
    private VariableType variableType = null;

    public Expression(Range range) {
        super(range);
    }

    public Optional<VariableType> getVariableType() {
        return Optional.ofNullable(variableType);
    }

    public void setVariableType(VariableType variableType) {
        this.variableType = variableType;
        if (variableType == null)
            throw new UnsupportedOperationException("Should be UNKNOWN, not null: " + getClass().getName());
    }

    /**
     * Deduces the type of this variable using the given expression type deducer utility. If a type could be
     * unambiguously deduced, this variable's cached variable type is updated to match the computed value.
     */
    public void deduceAndAssignVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree) {
        setVariableType(deduceVariableType(expressionTypeDeducer, scopeTree));
    }

    /**
     * Deduces the type of this variable using the given expression type deducer utility.
     * This calculation may depend on the types of other variables, so the current scope is included here so
     * we can look at other variables' types.
     * @return A non-null variable type. If the computation succeeded, this expression's cached variable
     * type will be set to the return value. Else, return VariableType.Primitive.TYPE_UNKNOWN.
     */
    protected abstract VariableType deduceVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree);
}
