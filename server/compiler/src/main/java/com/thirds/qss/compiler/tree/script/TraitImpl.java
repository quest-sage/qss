package com.thirds.qss.compiler.tree.script;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.lexer.TokenType;
import com.thirds.qss.compiler.tree.Documentable;
import com.thirds.qss.compiler.tree.NameLiteral;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.tree.Type;

import java.util.ArrayList;
import java.util.function.Consumer;

public class TraitImpl extends Node {
    private final NameLiteral trait;
    private final Type type;
    private final ArrayList<Documentable<Func>> funcImpls;

    public TraitImpl(Range range, NameLiteral trait, Type type, ArrayList<Documentable<Func>> funcImpls) {
        super(range);
        this.trait = trait;
        this.type = type;
        this.funcImpls = funcImpls;
    }

    public NameLiteral getTrait() {
        return trait;
    }

    public Type getType() {
        return type;
    }

    public ArrayList<Documentable<Func>> getFuncImpls() {
        return funcImpls;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        consumer.accept(trait);
        consumer.accept(type);
        for (Documentable<Func> funcImpl : funcImpls) {
            consumer.accept(funcImpl);
        }
    }

    @Override
    public String toString() {
        return "impl " + trait + " for " + type + " @" + getRange();
    }
}
