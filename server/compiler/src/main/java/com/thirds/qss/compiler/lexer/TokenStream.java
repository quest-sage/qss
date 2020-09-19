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
        currentIndex++;
        return tokens.get(currentIndex);
    }

    public boolean hasNext() {
        return currentIndex + 1 < tokens.size();
    }

    public Position currentPosition() {
        if (tokens.isEmpty()) {
            return new Position(0, 0);
        }
        return peek().map(tk -> tk.getRange().start).orElseGet(() -> tokens.get(tokens.size() - 1).getRange().end);
    }

    public void rewind() {
        currentIndex--;
    }
}
