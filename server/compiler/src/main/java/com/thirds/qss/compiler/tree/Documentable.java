package com.thirds.qss.compiler.tree;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;

import java.util.function.Consumer;

/**
 * Represents a trait, impl, struct or func.
 */
public class Documentable<T extends Node> extends Node {
    /**
     * Nullable.
     */
    private final Token documentation;
    private final T content;

    /**
     * @param documentation Must have type DOCUMENTATION_COMMENT if not null.
     */
    public Documentable(Token documentation, T content) {
        super(documentation == null ? content.getRange() : Range.combine(documentation.getRange(), content.getRange()));
        this.documentation = documentation;
        this.content = content;
    }

    public Token getDocumentation() {
        return documentation;
    }

    public T getContent() {
        return content;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        consumer.accept(content);
    }
}
