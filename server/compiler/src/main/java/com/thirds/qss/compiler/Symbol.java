package com.thirds.qss.compiler;

import java.util.Optional;

/**
 * Represents an object in a script that you can click on; either a small node (e.g. NameLiteral) or a token.
 * Implement this interface to gain jump-to-definition and hover support.
 */
public interface Symbol {
    /**
     * Where is this symbol in the document?
     */
    Range getRange();

    /**
     * Where does this symbol link to when you click it?
     */
    Optional<Location> getTargetLocation();
}
