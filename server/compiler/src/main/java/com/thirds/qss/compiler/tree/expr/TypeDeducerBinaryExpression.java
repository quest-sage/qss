package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Messenger;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;
import com.thirds.qss.compiler.type.VariableTracker;

import java.util.Optional;

/**
 * This class represents binary expressions where the type of the left and right expressions must be specific
 * values, and the resulting variable type is easily deducible from the types of said expressions.
 */
public abstract class TypeDeducerBinaryExpression extends BinaryExpression {
    public TypeDeducerBinaryExpression(Expression left, Expression right) {
        super(left, right);
    }

    /**
     * Deduce the variable type of this expression given the types of the left and right expressions.
     *
     * @return A successful messenger if the given types were valid inputs.
     * Messages will be propagated upwards.
     */
    public abstract Messenger<VariableType> forTypePair(ExpressionTypeDeducer expressionTypeDeducer, VariableType left, VariableType right);

    @Override
    protected VariableType deduceVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree) {
        getLeft().deduceAndAssignVariableType(expressionTypeDeducer, scopeTree);
        getRight().deduceAndAssignVariableType(expressionTypeDeducer, scopeTree);
        Optional<Messenger<VariableType>> messenger = getLeft().getVariableType().flatMap(left -> getRight().getVariableType().map(right -> forTypePair(expressionTypeDeducer, left, right)));
        if (messenger.isPresent()) {
            expressionTypeDeducer.getMessages().addAll(messenger.get().getMessages());
            return messenger.get().getValue().orElse(null);
        }
        return null;
    }
}
