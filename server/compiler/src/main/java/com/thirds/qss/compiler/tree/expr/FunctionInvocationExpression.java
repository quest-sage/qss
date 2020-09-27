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
    private final boolean isReceiverStyle;

    public FunctionInvocationExpression(Expression function, ArrayList<Expression> args) {
        this(function, args, false);
    }

    public FunctionInvocationExpression(Expression function, ArrayList<Expression> args, boolean isReceiverStyle) {
        super(args.isEmpty() ? function.getRange() : Range.combine(function.getRange(), args.get(args.size() - 1).getRange()));
        this.function = function;
        this.args = args;
        this.isReceiverStyle = isReceiverStyle;
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

        int parameterCount = funcType.isReceiverStyle() ? paramTypes.size() - 1 : paramTypes.size();
        int argumentCount = isReceiverStyle ? argTypes.size() - 1 : argTypes.size();
        boolean parameterMismatch = parameterCount != argumentCount;
        boolean styleMismatch = isReceiverStyle != funcType.isReceiverStyle();
        if (parameterMismatch || styleMismatch) {
            StringBuilder sb = new StringBuilder();
            sb.append("Function expected ");

            if (parameterMismatch && styleMismatch) {
                if (funcType.isReceiverStyle()) {
                    sb.append("a receiver and ");
                } else {
                    sb.append("no receiver and ");
                }
                sb.append(parameterCount).append(" parameters, but ");
                if (isReceiverStyle) {
                    sb.append("a receiver and ");
                } else {
                    sb.append("no receiver and ");
                }
                sb.append(argumentCount).append(" arguments were supplied");
            } else if (parameterMismatch) {
                sb.append(parameterCount).append(" parameters, but ").append(argumentCount).append(" arguments were supplied");
            } else {
                // style mismatch
                if (funcType.isReceiverStyle()) {
                    sb.append("a receiver");
                } else {
                    sb.append("no receiver");
                }
                sb.append(", but ");
                if (isReceiverStyle) {
                    sb.append("a receiver");
                } else {
                    sb.append("no receiver");
                }
                sb.append(" was supplied");
            }

            expressionTypeDeducer.getMessages().add(new Message(
                    function.getRange(),
                    Message.MessageSeverity.ERROR,
                    sb.toString()
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

    public boolean isReceiverStyle() {
        return isReceiverStyle;
    }
}
