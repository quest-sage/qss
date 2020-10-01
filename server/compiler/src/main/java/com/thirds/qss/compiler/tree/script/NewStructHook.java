package com.thirds.qss.compiler.tree.script;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.lexer.TokenType;
import com.thirds.qss.compiler.tree.NameLiteral;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.tree.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents an <code>after new</code> hook.
 */
public class NewStructHook extends FuncOrHook {
    /**
     * When should the hook execute? Either KW_BEFORE or KW_AFTER.
     */
    private final Token time;
    private final NameLiteral structName;

    public NewStructHook(Range range, Token time, Token getToken, NameLiteral structName, FuncBlock funcBlock) {
        super(
                range, VariableType.Function.Purity.PURE,
                new ParamList(getToken.getRange(), new ArrayList<>(0)),
                new Type.StructType(structName.getRange(), structName), funcBlock
        );
        this.time = time;

        this.structName = structName;
    }

    public Token getTime() {
        return time;
    }

    public NameLiteral getStructName() {
        return structName;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        super.forChildren(consumer);
        consumer.accept(structName);
    }
}
