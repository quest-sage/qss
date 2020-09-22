package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Location;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.Messenger;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class TypeMapBinaryExpression extends TypeDeducerBinaryExpression {
    public TypeMapBinaryExpression(Expression left, Expression right) {
        super(left, right);
    }

    protected static class RightHandSide {
        private final VariableType rightHandSide;
        private final VariableType resultType;

        public RightHandSide(VariableType rightHandSide, VariableType resultType) {
            this.rightHandSide = rightHandSide;
            this.resultType = resultType;
        }
    }

    /**
     * The name of the expression type to be used in type deduction error messages,
     * e.g. "addition"; "function invocation".
     */
    public abstract String getExpressionTypeName();

    /**
     * Should return a map that maps valid types of left-hand-side expressions to corresponding valid types for
     * right-hand-side expressions.
     *
     * E.g. for addition, the map is: [ Int -> [Int->Int,Ratio->Ratio], Ratio -> [Int->Ratio,Ratio->Ratio] ]
     * because an Int left-hand-side implies an Int or Ratio right-hand-side, which imply that the result type
     * of this expression is an Int or Ratio respectively.
     */
    public abstract Map<VariableType, Map<VariableType, VariableType>> getVariableTypeMap();

    @Override
    public Messenger<VariableType> forTypePair(ExpressionTypeDeducer expressionTypeDeducer, VariableType left, VariableType right) {
        Map<VariableType, Map<VariableType, VariableType>> typeMap = getVariableTypeMap();
        if (typeMap.containsKey(left)) {
            Map<VariableType, VariableType> rights = typeMap.get(left);
            if (rights.containsKey(right)) {
                return Messenger.success(rights.get(right));
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Expected right hand side of type ");
                Stream<String> sortedValidTypes = rights.keySet().stream().map(Object::toString).sorted();
                sb.append(sortedValidTypes.collect(Collectors.joining(" or ")));
                sb.append(" in ").append(getExpressionTypeName()).append(" expression, got ").append(right);

                return Messenger.fail(new ArrayList<>(List.of(new Message(
                        getRight().getRange(),
                        Message.MessageSeverity.ERROR,
                        sb.toString()
                ).addInfo(new Message.MessageRelatedInformation(
                        new Location(expressionTypeDeducer.getFilePath(), getLeft().getRange()),
                        "Required because the left hand side of this expression is of type " + left
                )))));
            }
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Expected left hand side of type ");
            Stream<String> sortedValidTypes = typeMap.keySet().stream().map(Object::toString).sorted();
            sb.append(sortedValidTypes.collect(Collectors.joining(" or ")));
            sb.append(" in ").append(getExpressionTypeName()).append(" expression, got ").append(right);

            return Messenger.fail(new ArrayList<>(List.of(new Message(
                    getRight().getRange(),
                    Message.MessageSeverity.ERROR,
                    sb.toString()
            ))));
        }
    }
}
