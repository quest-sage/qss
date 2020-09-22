package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.lexer.TokenType;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;
import com.thirds.qss.compiler.type.VariableTracker;

public class BooleanLiteral extends Expression {
    private final Token bool;

    public BooleanLiteral(Token bool) {
        super(bool.getRange());
        if (bool.type != TokenType.KW_TRUE && bool.type != TokenType.KW_FALSE)
            throw new UnsupportedOperationException();
        this.bool = bool;
    }

    @Override
    protected VariableType deduceVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree) {
        return VariableType.Primitive.TYPE_BOOL;
    }
}
