package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.resolve.Resolver;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.tree.Type;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;
import com.thirds.qss.compiler.type.VariableTracker;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Creates a new map containing the supplied values.
 */
public class NewMapExpression extends Expression {
    private final Type type;
    private final ArrayList<MapField> values;

    public NewMapExpression(Range totalRange, Type type, ArrayList<MapField> values) {
        super(totalRange);
        this.type = type;
        this.values = values;
    }

    @Override
    protected VariableType deduceVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree) {
        Resolver.resolveType(expressionTypeDeducer.getCompiler(), expressionTypeDeducer.getScript(), expressionTypeDeducer.getMessages(), "new map", type);
        if (!(type.getResolvedType() instanceof VariableType.Map)) {
            throw new UnsupportedOperationException(type.getResolvedType().toString());
        }
        return type.getResolvedType();
    }

    public ArrayList<MapField> getValues() {
        return values;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        consumer.accept(type);
        for (MapField value : values) {
            consumer.accept(value.getKey());
            consumer.accept(value.getValue());
        }
    }
}
