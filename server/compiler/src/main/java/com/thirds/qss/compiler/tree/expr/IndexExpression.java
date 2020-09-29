package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.Messenger;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;

import java.util.ArrayList;
import java.util.List;

public class IndexExpression extends TypeDeducerBinaryExpression {
    private boolean requireList = false;

    public IndexExpression(Expression left, Expression right) {
        super(left, right);
    }

    @Override
    public Messenger<VariableType> forTypePair(ExpressionTypeDeducer expressionTypeDeducer, VariableType left, VariableType right) {
        if (left instanceof VariableType.List) {
            VariableType elementType = ((VariableType.List) left).getElementType();
            Messenger<Object> downcast = expressionTypeDeducer.getCastChecker().attemptDowncast(getRight().getRange(), right, VariableType.Primitive.TYPE_INT);
            return downcast.then(() -> Messenger.success(new VariableType.Maybe(elementType)));
        } else if (left instanceof VariableType.Map && !requireList) {
            VariableType keyType = ((VariableType.Map) left).getKeyType();
            VariableType valueType = ((VariableType.Map) left).getValueType();
            Messenger<Object> downcast = expressionTypeDeducer.getCastChecker().attemptDowncast(getRight().getRange(), right, keyType);
            return downcast.then(() -> Messenger.success(new VariableType.Maybe(valueType)));
        } else {
            if (requireList) {
                String messageSuffix = (left instanceof VariableType.Map) ? "; try using 'for key => value in map' syntax" : "";
                return Messenger.fail(new ArrayList<>(List.of(new Message(
                        getLeft().getRange(),
                        Message.MessageSeverity.ERROR,
                        "Expected a list, got expression of type " + left + messageSuffix
                ))));
            } else {
                return Messenger.fail(new ArrayList<>(List.of(new Message(
                        getLeft().getRange(),
                        Message.MessageSeverity.ERROR,
                        "Expected a list or map, got expression of type " + left
                ))));
            }
        }
    }

    /**
     * Requires that the left argument of this expression is a list, not a map.
     * @return This for chaining.
     */
    public IndexExpression requireList() {
        this.requireList = true;
        return this;
    }
}
