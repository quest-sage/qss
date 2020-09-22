package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.lexer.TokenType;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;
import com.thirds.qss.compiler.type.VariableTracker;

public class IntegerLiteral extends Expression {
    private final Token integer;

    public IntegerLiteral(Token integer) {
        super(integer.getRange());
        if (integer.type != TokenType.INTEGER_LITERAL)
            throw new UnsupportedOperationException();
        this.integer = integer;
    }

    @Override
    protected VariableType deduceVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree) {
        return VariableType.Primitive.TYPE_INT;
    }
}
