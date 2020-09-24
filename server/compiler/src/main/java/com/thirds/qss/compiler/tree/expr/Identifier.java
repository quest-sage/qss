package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Location;
import com.thirds.qss.compiler.Symbol;
import com.thirds.qss.compiler.tree.NameLiteral;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;
import com.thirds.qss.compiler.type.VariableTracker;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * References a variable, such as a local variable, a parameter or a function.
 */
public class Identifier extends Expression implements Symbol {
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

    @Override
    public void forChildren(Consumer<Node> consumer) {
        consumer.accept(name);
    }

    @Override
    public Optional<Location> getTargetLocation() {
        return name.getTargetLocation();
    }

    @Override
    public Optional<String> getTargetDocumentation() {
        String header = "`" + name.toQualifiedName() + ": " + getVariableType().map(Objects::toString).orElse("<not evaluated>") + "`";
        return name.getTargetDocumentation().map(s -> header + "\n\n---\n\n" + s).or(() -> Optional.of(header));
    }

    @Override
    protected VariableType deduceVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree) {
        expressionTypeDeducer.resolveIdentifier(scopeTree, this);
        return getVariableType().orElse(null);
    }
}
