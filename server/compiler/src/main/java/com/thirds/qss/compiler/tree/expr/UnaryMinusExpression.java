package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;
import com.thirds.qss.compiler.type.VariableTracker;

public class UnaryMinusExpression extends UnaryExpression {
    public UnaryMinusExpression(Expression argument) {
        super(argument);
    }

    @Override
    protected VariableType deduceVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree) {
        getArgument().deduceAndAssignVariableType(expressionTypeDeducer, scopeTree);
        return getArgument().getVariableType().map(vt -> {
            if (vt != VariableType.Primitive.TYPE_INT && vt != VariableType.Primitive.TYPE_RATIO) {
                expressionTypeDeducer.getMessages().add(new Message(
                        getArgument().getRange(),
                        Message.MessageSeverity.ERROR,
                        "Expected an expression of type " +
                                VariableType.Primitive.TYPE_INT + " or " +
                                VariableType.Primitive.TYPE_RATIO + ", got " + vt
                ));
            }
            return vt;
        }).orElse(VariableType.Primitive.TYPE_UNKNOWN);
    }
}
