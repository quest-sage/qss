package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.tree.NameLiteral;
import com.thirds.qss.compiler.tree.Node;

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
}
