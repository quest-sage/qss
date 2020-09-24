package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.resolve.ResolveResult;
import com.thirds.qss.compiler.resolve.Resolver;
import com.thirds.qss.compiler.tree.NameLiteral;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;
import com.thirds.qss.compiler.type.VariableTracker;

import java.util.function.Consumer;

public class FieldExpression extends Expression {
    private final Expression value;
    private final NameLiteral field;

    public FieldExpression(Expression value, NameLiteral field) {
        super(Range.combine(value.getRange(), field.getRange()));
        this.value = value;
        this.field = field;
    }

    public Expression getValue() {
        return value;
    }

    public NameLiteral getField() {
        return field;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        consumer.accept(value);
        consumer.accept(field);
    }

    @Override
    protected VariableType deduceVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree) {
        value.deduceAndAssignVariableType(expressionTypeDeducer, scopeTree);
        if (value.getVariableType().isEmpty())
            return VariableType.Primitive.TYPE_UNKNOWN;
        VariableType type = value.getVariableType().get();
        if (!(type instanceof VariableType.Struct)) {
            expressionTypeDeducer.getMessages().add(new Message(
                    value.getRange(),
                    Message.MessageSeverity.ERROR,
                    "Expected a struct, got expression of type " + type
            ));
            return VariableType.Primitive.TYPE_UNKNOWN;
        }
        VariableType.Struct struct = (VariableType.Struct) type;
        ResolveResult<Resolver.StructFieldAlternative> alternatives = Resolver.resolveStructField(
                expressionTypeDeducer.getCompiler(),
                expressionTypeDeducer.getScript(),
                expressionTypeDeducer.getMessages(),
                struct.getName(),
                field
        );
        if (alternatives.alternatives.size() == 1) {
            return alternatives.alternatives.get(0).value.type;
        }
        return VariableType.Primitive.TYPE_UNKNOWN;
    }
}
