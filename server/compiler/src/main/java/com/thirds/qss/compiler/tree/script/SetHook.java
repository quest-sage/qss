package com.thirds.qss.compiler.tree.script;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.lexer.TokenType;
import com.thirds.qss.compiler.tree.NameLiteral;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.tree.Type;
import com.thirds.qss.compiler.tree.expr.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a <code>before set</code> or <code>after set</code> hook.
 */
public class SetHook extends FuncOrHook {
    /**
     * When should the hook execute? Either KW_BEFORE or KW_AFTER.
     */
    private final Token time;
    private final NameLiteral structName;
    private final NameLiteral fieldName;
    private final Type fieldType;

    public SetHook(Range range, Token time, Token getToken, NameLiteral structName, NameLiteral fieldName, Type fieldType, FuncBlock funcBlock) {
        super(
                range, VariableType.Function.Purity.PURE,
                new ParamList(getToken.getRange(), new ArrayList<>(List.of(new Param(
                        getToken.getRange(), new Token(TokenType.KW_THIS, "this", getToken.getRange()), new Type.StructType(structName.getRange(), structName)
                ), new Param(
                        getToken.getRange(), new Token(TokenType.IDENTIFIER, "value", getToken.getRange()), fieldType
                )))),
                null, funcBlock
        );
        this.time = time;

        this.structName = structName;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    public Token getTime() {
        return time;
    }

    public NameLiteral getStructName() {
        return structName;
    }

    public NameLiteral getFieldName() {
        return fieldName;
    }

    public Type getFieldType() {
        return fieldType;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        super.forChildren(consumer);
        consumer.accept(structName);
        consumer.accept(fieldName);
        consumer.accept(fieldName);
    }
}
