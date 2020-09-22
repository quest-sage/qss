package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;

import java.util.Map;

public class SubtractExpression extends TypeMapBinaryExpression {
    public SubtractExpression(Expression left, Expression right) {
        super(left, right);
    }

    @Override
    public String getExpressionTypeName() {
        return "subtraction";
    }

    @Override
    public Map<VariableType, Map<VariableType, VariableType>> getVariableTypeMap() {
        return Map.of(
                VariableType.Primitive.TYPE_INT,
                Map.of(
                        VariableType.Primitive.TYPE_INT, VariableType.Primitive.TYPE_INT,
                        VariableType.Primitive.TYPE_RATIO, VariableType.Primitive.TYPE_RATIO
                ),

                VariableType.Primitive.TYPE_RATIO,
                Map.of(
                        VariableType.Primitive.TYPE_INT, VariableType.Primitive.TYPE_RATIO,
                        VariableType.Primitive.TYPE_RATIO, VariableType.Primitive.TYPE_RATIO
                )
        );
    }
}
