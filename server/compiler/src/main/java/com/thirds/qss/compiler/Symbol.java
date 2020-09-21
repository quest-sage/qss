package com.thirds.qss.compiler;

import java.util.Optional;

/**
 * Represents an object in a script that you can click on; either a small node (e.g. NameLiteral) or a token.
 * Implement this interface to gain jump-to-definition and hover support.
 */
public interface Symbol extends Ranged {
    /**
     * Where does this symbol link to when you click it?
     */
    Optional<Location> getTargetLocation();

    /**
     * What documentation shows when you hover this symbol?
     */
    Optional<String> getTargetDocumentation();
}
