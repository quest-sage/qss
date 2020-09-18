package com.thirds.qss.compiler.lexer;

import com.thirds.qss.compiler.Position;

import java.util.ArrayList;
import java.util.Optional;

public class TokenStream {
    private final ArrayList<Token> tokens;
    private int currentIndex = -1;

    public TokenStream(ArrayList<Token> tokens) {
        this.tokens = tokens;
    }

    public Optional<Token> peek() {
        if (hasNext())
            return Optional.of(tokens.get(currentIndex + 1));
        return Optional.empty();
    }

    public Token next() {
        currentIndex += 1;
        return tokens.get(currentIndex);
    }

    public boolean hasNext() {
        return currentIndex + 1 < tokens.size();
    }

    public Position currentPosition() {
        return peek().map(tk -> tk.range.start).orElseGet(() -> tokens.get(tokens.size() - 1).range.end);
    }
}
