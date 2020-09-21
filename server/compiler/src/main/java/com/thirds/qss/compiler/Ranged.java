package com.thirds.qss.compiler;

/**
 * Represents an object in a script like a node or a token.
 */
public interface Ranged {
    /**
     * Where is this symbol in the document?
     */
    Range getRange();
}
