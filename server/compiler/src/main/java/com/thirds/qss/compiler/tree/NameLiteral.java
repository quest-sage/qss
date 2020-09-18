package com.thirds.qss.compiler.tree;

import com.thirds.qss.QualifiedName;
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

    /**
     * Does this name literal match the given fully qualified name?
     * @return true if there is a matching tail - i.e, if one or more segments from the end of the qualified name
     * match the entire name literal.
     */
    public boolean matches(QualifiedName qualifiedName) {
        // TODO test the match functions
        if (qualifiedName.getSegments().size() < segments.size())
            return false;
        // Match the segments in reverse.
        for (int i = 1; i <= segments.size(); i++) {
            if (!qualifiedName.getSegments().get(qualifiedName.getSegments().size() - i).equals(segments.get(segments.size() - i).contents)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Does this name literal match the given local name?
     * @return true if the name has only one segment, and the segment matches the given name.
     */
    public boolean matches(String localName) {
        if (segments.size() != 1)
            return false;
        return segments.get(0).contents.equals(localName);
    }
}
