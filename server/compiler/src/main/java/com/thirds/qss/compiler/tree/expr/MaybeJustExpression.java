package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;
import com.thirds.qss.compiler.type.VariableTracker;

/**
 * Creates a new maybe value containing the given value.
 */
public class MaybeJustExpression extends UnaryExpression {
    public MaybeJustExpression(Token token, Expression argument) {
        super(Range.combine(token.getRange(), argument.getRange()), argument);
    }

    @Override
    protected VariableType deduceVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree) {
        getArgument().deduceAndAssignVariableType(expressionTypeDeducer, scopeTree);
        return getArgument().getVariableType().map(contentsType -> (VariableType) new VariableType.Maybe(contentsType)).orElse(VariableType.Primitive.TYPE_UNKNOWN);
    }
}
