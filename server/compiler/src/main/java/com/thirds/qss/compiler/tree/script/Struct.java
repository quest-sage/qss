package com.thirds.qss.compiler.tree.script;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.lexer.TokenType;
import com.thirds.qss.compiler.tree.Documentable;
import com.thirds.qss.compiler.tree.Node;

import java.util.ArrayList;
import java.util.function.Consumer;

public class Struct extends Node {
    private final Token name;
    private final ArrayList<Documentable<Field>> fields;

    public Struct(Range range, Token name, ArrayList<Documentable<Field>> fields) {
        super(range);
        this.name = name;
        this.fields = fields;
        if (this.name.type != TokenType.IDENTIFIER) {
            throw new UnsupportedOperationException("Struct name token " + name + " must have type " + TokenType.IDENTIFIER);
        }
    }

    public Token getName() {
        return name;
    }

    public ArrayList<Documentable<Field>> getFields() {
        return fields;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        for (Documentable<Field> field : fields) {
            consumer.accept(field);
        }
    }

    @Override
    public String toString() {
        return "struct " + name.contents + "@" + getRange();
    }
}
