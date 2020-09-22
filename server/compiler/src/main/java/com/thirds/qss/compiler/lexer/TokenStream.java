package com.thirds.qss.compiler.lexer;

import com.thirds.qss.QssLogger;
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

    /**
     * @param peekAmount How many tokens to peek ahead?
     */
    public Optional<Token> peek(int peekAmount) {
        if (currentIndex + peekAmount < tokens.size())
            return Optional.of(tokens.get(currentIndex + peekAmount));
        return Optional.empty();
    }

    public Token next() {
        currentIndex++;
        return tokens.get(currentIndex);
    }

    public boolean hasNext() {
        return currentIndex + 1 < tokens.size();
    }

    /**
     * Retrieves the start position of the next token to be read.
     */
    public Position currentPosition() {
        if (tokens.isEmpty()) {
            return new Position(0, 0);
        }
        return peek().map(tk -> tk.getRange().start).orElseGet(() -> tokens.get(tokens.size() - 1).getRange().end);
    }

    /**
     * Retrieves the end position of the previous token that was read.
     */
    public Position currentEndPosition() {
        if (tokens.size() < 2) {
            return new Position(0, 0);
        }
        return peek(0).map(tk -> tk.getRange().end).orElseGet(() -> tokens.get(tokens.size() - 1).getRange().end);
    }

    public void rewind() {
        currentIndex--;
    }
}
