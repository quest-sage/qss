package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.Messenger;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;
import com.thirds.qss.compiler.type.VariableTracker;

import java.util.ArrayList;

public class FunctionInvocationExpression extends Expression {
    private final Expression function;
    private final ArrayList<Expression> args;

    public FunctionInvocationExpression(Expression function, ArrayList<Expression> args) {
        super(args.isEmpty() ? function.getRange() : Range.combine(function.getRange(), args.get(args.size() - 1).getRange()));
        this.function = function;
        this.args = args;
    }

    @Override
    protected VariableType deduceVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree) {
        function.deduceAndAssignVariableType(expressionTypeDeducer, scopeTree);
        for (Expression arg : args) {
            arg.deduceAndAssignVariableType(expressionTypeDeducer, scopeTree);
        }

        if (function.getVariableType().isEmpty() || function.getVariableType().get() == VariableType.Primitive.TYPE_UNKNOWN) {
            return VariableType.Primitive.TYPE_UNKNOWN;
        }

        VariableType func = function.getVariableType().get();
        if (!(func instanceof VariableType.Function)) {
            expressionTypeDeducer.getMessages().add(new Message(
                    function.getRange(),
                    Message.MessageSeverity.ERROR,
                    "Expected function, got " + func
            ));
            return VariableType.Primitive.TYPE_UNKNOWN;
        }
        VariableType.Function funcType = (VariableType.Function) func;

        ArrayList<VariableType> argTypes = new ArrayList<>(args.size());
        for (Expression arg : args) {
            if (arg.getVariableType().isEmpty() || arg.getVariableType().get() == VariableType.Primitive.TYPE_UNKNOWN)
                return funcType.getReturnType();
            argTypes.add(arg.getVariableType().get());
        }

        ArrayList<VariableType> paramTypes = funcType.getParams();

        if (argTypes.size() != paramTypes.size()) {
            expressionTypeDeducer.getMessages().add(new Message(
                    function.getRange(),
                    Message.MessageSeverity.ERROR,
                    "Function expected " + paramTypes.size() + " parameters, but " + argTypes.size() + " arguments were supplied"
            ));
            return funcType.getReturnType();
        }

        for (int i = 0; i < argTypes.size(); i++) {
            VariableType argType = argTypes.get(i);
            VariableType paramType = paramTypes.get(i);

            Messenger<Object> downcast = expressionTypeDeducer.getCastChecker().attemptDowncast(
                    args.get(i).getRange(),
                    argType, paramType
            );
            expressionTypeDeducer.getMessages().addAll(downcast.getMessages());
        }

        return funcType.getReturnType();
    }
}
