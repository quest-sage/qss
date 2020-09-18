package com.thirds.qss.compiler.tree;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;

import java.util.Collections;
import java.util.List;

/**
 * This can represent a qualified name such as <code>std::entity::spawn</code>, or a local name like <code>foo</code>.
 */
public class NameLiteral extends Node {
    private final List<Token> segments;

    public NameLiteral(Range range, List<Token> segments) {
        super(range);
        this.segments = Collections.unmodifiableList(segments);
    }

    public List<Token> getSegments() {
        return segments;
    }
}
