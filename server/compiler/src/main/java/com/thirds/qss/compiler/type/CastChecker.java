package com.thirds.qss.compiler.type;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.Messenger;
import com.thirds.qss.compiler.Range;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates whether casts are valid between two variable types.
 */
public class CastChecker {
    public CastChecker() {

    }

    /**
     * Evaluates whether an implicit downcast is possible between the given variable types.
     * @param where Where should we emit error messages from?
     * @param expression The type of the expression we want to cast.
     * @param target The type we want to cast it to.
     * @return A successful messenger if an implicit downcast was valid.
     * This returns a failed messenger if the downcast was invalid.
     */
    public Messenger<Object> attemptDowncast(Range where, VariableType expression, VariableType target) {
        if (expression == VariableType.Primitive.TYPE_UNKNOWN || target == VariableType.Primitive.TYPE_UNKNOWN)
            return Messenger.fail(new ArrayList<>(0));

        if (expression instanceof VariableType.Primitive) {
            if (target instanceof VariableType.Primitive) {
                if (expression == target) {
                    return Messenger.success(new Object());
                }
            }
        } else if (expression instanceof VariableType.Maybe) {
            if (target instanceof VariableType.Maybe) {
                if (attemptDowncast(
                        where,
                        ((VariableType.Maybe) expression).getContentsType(),
                        ((VariableType.Maybe) target).getContentsType()
                ).getMessages().isEmpty()) {
                    return Messenger.success(new Object());
                }
            }
        } else if (expression instanceof VariableType.List) {
            if (target instanceof VariableType.List) {
                if (attemptDowncast(
                        where,
                        ((VariableType.List) expression).getElementType(),
                        ((VariableType.List) target).getElementType()
                ).getMessages().isEmpty()) {
                    return Messenger.success(new Object());
                }
            }
        } else if (expression instanceof VariableType.Map) {
            if (target instanceof VariableType.Map) {
                if (attemptDowncast(
                        where,
                        ((VariableType.Map) expression).getKeyType(),
                        ((VariableType.Map) target).getKeyType()
                ).getMessages().isEmpty()) {
                    if (attemptDowncast(
                            where,
                            ((VariableType.Map) expression).getValueType(),
                            ((VariableType.Map) target).getValueType()
                    ).getMessages().isEmpty()) {
                        return Messenger.success(new Object());
                    }
                }
            }
        } else if (expression instanceof VariableType.Function) {
            if (target instanceof VariableType.Function) {
                VariableType.Function expression1 = (VariableType.Function) expression;
                VariableType.Function target1 = (VariableType.Function) target;

                if (expression1.getParams().size() == target1.getParams().size()) {
                    if (attemptDowncast(
                            where,
                            expression1.getReturnType(),
                            target1.getReturnType()
                    ).getMessages().isEmpty()) {
                        boolean fail = false;
                        for (int i = 0; i < expression1.getParams().size(); i++) {
                            if (!attemptDowncast(
                                    where,
                                    expression1.getParams().get(i),
                                    target1.getParams().get(i)
                            ).getMessages().isEmpty()) {
                                fail = true;
                                break;
                            }
                        }
                        if (!fail) {
                            return Messenger.success(new Object());
                        }
                    }
                }
            }
        }

        return Messenger.fail(new ArrayList<>(List.of(new Message(
                where,
                Message.MessageSeverity.ERROR,
                "Expected an expression of type " + target + ", got " + expression
        ))));
    }
}
