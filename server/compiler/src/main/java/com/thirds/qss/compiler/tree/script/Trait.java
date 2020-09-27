package com.thirds.qss.compiler.tree.script;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.lexer.TokenType;
import com.thirds.qss.compiler.tree.Documentable;
import com.thirds.qss.compiler.tree.Node;

import java.util.ArrayList;
import java.util.function.Consumer;

public class Trait extends Node {
    private final Token name;
    private final ArrayList<Documentable<TraitFunc>> traitFuncs;

    public Trait(Range range, Token name, ArrayList<Documentable<TraitFunc>> traitFuncs) {
        super(range);
        this.name = name;
        this.traitFuncs = traitFuncs;
        if (this.name.type != TokenType.IDENTIFIER) {
            throw new UnsupportedOperationException("Trait name token " + name + " must have type " + TokenType.IDENTIFIER);
        }
    }

    public Token getName() {
        return name;
    }

    public ArrayList<Documentable<TraitFunc>> getTraitFuncs() {
        return traitFuncs;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        for (Documentable<TraitFunc> traitFunc : traitFuncs) {
            consumer.accept(traitFunc);
        }
    }

    @Override
    public String toString() {
        return "trait " + name.contents + "@" + getRange();
    }
}
