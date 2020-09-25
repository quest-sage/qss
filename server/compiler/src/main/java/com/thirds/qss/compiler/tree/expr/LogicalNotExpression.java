package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;
import com.thirds.qss.compiler.type.VariableTracker;

public class LogicalNotExpression extends UnaryExpression {
    public LogicalNotExpression(Token token, Expression argument) {
        super(Range.combine(token.getRange(), argument.getRange()), argument);
    }

    @Override
    protected VariableType deduceVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree) {
        getArgument().deduceAndAssignVariableType(expressionTypeDeducer, scopeTree);
        getArgument().getVariableType().ifPresent(vt -> {
            if (vt != VariableType.Primitive.TYPE_BOOL) {
                expressionTypeDeducer.getMessages().add(new Message(
                        getArgument().getRange(),
                        Message.MessageSeverity.ERROR,
                        "Expected an expression of type " + VariableType.Primitive.TYPE_BOOL + ", got " + vt
                ));
            }
        });
        return VariableType.Primitive.TYPE_BOOL;
    }
}
