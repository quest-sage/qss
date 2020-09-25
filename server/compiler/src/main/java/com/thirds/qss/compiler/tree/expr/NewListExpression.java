package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.resolve.Resolver;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.tree.Type;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;
import com.thirds.qss.compiler.type.VariableTracker;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Creates a new list containing the supplied values.
 */
public class NewListExpression extends Expression {
    private final Type type;
    private final ArrayList<Expression> values;

    public NewListExpression(Range totalRange, Type type, ArrayList<Expression> values) {
        super(totalRange);
        this.type = type;
        this.values = values;
    }

    @Override
    protected VariableType deduceVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree) {
        Resolver.resolveType(expressionTypeDeducer.getCompiler(), expressionTypeDeducer.getScript(), expressionTypeDeducer.getMessages(), "new list", type);
        if (!(type.getResolvedType() instanceof VariableType.List)) {
            throw new UnsupportedOperationException(type.getResolvedType().toString());
        }
        return type.getResolvedType();
    }

    public ArrayList<Expression> getValues() {
        return values;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        consumer.accept(type);
        for (Expression value : values) {
            consumer.accept(value);
        }
    }
}
