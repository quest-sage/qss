package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;
import com.thirds.qss.compiler.type.VariableTracker;

import java.util.ArrayList;
import java.util.function.Consumer;

public class LogicExpression extends Expression {
    private final ArrayList<Expression> arguments;

    public LogicExpression(Token expressionType, ArrayList<Expression> arguments) {
        super(Range.combine(
                arguments.get(0).getRange(),
                arguments.get(arguments.size() - 1).getRange()
        ));
        this.arguments = arguments;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        for (Expression argument : arguments) {
            consumer.accept(argument);
        }
    }

    @Override
    protected VariableType deduceVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree) {
        for (Expression argument : arguments) {
            argument.deduceAndAssignVariableType(expressionTypeDeducer, scopeTree);
        }

        for (Expression argument : arguments) {
            if (argument.getVariableType().isEmpty() || argument.getVariableType().get() != VariableType.Primitive.TYPE_BOOL) {
                expressionTypeDeducer.getMessages().add(new Message(
                        argument.getRange(),
                        Message.MessageSeverity.ERROR,
                        "Expected an expression of type " + VariableType.Primitive.TYPE_BOOL + ", got " + argument.getVariableType().orElse(VariableType.Primitive.TYPE_UNKNOWN)
                ));
            }
        }

        return VariableType.Primitive.TYPE_BOOL;
    }
}
