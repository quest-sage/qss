package com.thirds.qss.compiler.lexing;

import java.util.PrimitiveIterator;

public class CodePointIterator {
    private final PrimitiveIterator.OfInt internal;
    private int peekedCodePoint;

    public CodePointIterator(String text) {
        internal = text.codePoints().iterator();
        peekedCodePoint = internal.nextInt();
    }

    public int next() {
        int next = peekedCodePoint;
        if (internal.hasNext())
            peekedCodePoint = internal.nextInt();
        else
            peekedCodePoint = -1;
        return peekedCodePoint;
    }

    public int peek() {
        return peekedCodePoint;
    }

    public boolean hasNext() {
        return peekedCodePoint != -1;
    }
}
