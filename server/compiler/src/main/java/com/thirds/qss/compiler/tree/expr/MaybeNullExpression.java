package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.resolve.Resolver;
import com.thirds.qss.compiler.tree.Type;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;
import com.thirds.qss.compiler.type.VariableTracker;

/**
 * Creates a new maybe value containing no value.
 */
public class MaybeNullExpression extends Expression {
    private final Type type;

    public MaybeNullExpression(Token nullToken, Type type) {
        super(Range.combine(nullToken.getRange(), type.getRange()));
        this.type = type;
    }

    @Override
    protected VariableType deduceVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree) {
        Resolver.resolveType(expressionTypeDeducer.getCompiler(), expressionTypeDeducer.getScript(), expressionTypeDeducer.getMessages(), "null", type);
        return new VariableType.Maybe(type.getResolvedType());
    }
}
