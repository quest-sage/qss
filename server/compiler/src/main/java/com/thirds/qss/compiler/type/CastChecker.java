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
        }

        return Messenger.fail(new ArrayList<>(List.of(new Message(
                where,
                Message.MessageSeverity.ERROR,
                "Expected an expression of type " + target + ", got " + expression
        ))));
    }
}
