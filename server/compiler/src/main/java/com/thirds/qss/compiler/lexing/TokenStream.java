package com.thirds.qss.compiler.lexing;

import java.util.ArrayList;

public class TokenStream {
    private final ArrayList<Token> tokens;

    public TokenStream(ArrayList<Token> tokens) {
        this.tokens = tokens;
    }
}
