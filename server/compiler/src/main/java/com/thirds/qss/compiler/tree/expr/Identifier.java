package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.compiler.tree.NameLiteral;

/**
 * Represents the name of a variable, such as a local variable, a parameter or a function.
 */
public class Identifier extends Expression {
    private final NameLiteral name;

    /**
     * If true, this identifier references a local variable.
     */
    private boolean local = false;

    public Identifier(NameLiteral name) {
        super(name.getRange());
        this.name = name;
    }

    public NameLiteral getName() {
        return name;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }
}
