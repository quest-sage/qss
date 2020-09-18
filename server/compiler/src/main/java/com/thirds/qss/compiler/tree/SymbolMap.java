package com.thirds.qss.compiler.tree;

import com.thirds.qss.compiler.Position;
import com.thirds.qss.compiler.Symbol;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Maps a position in the document to the smallest possible node that intersects the position.
 * Used for jump-to-definition and hover, by locating which symbol we're currently looking at.
 */
public class SymbolMap {
    /**
     * Maps the starting position of each symbol to the symbol.
     * Once this map is built, it is a very fast operation to detect which symbol was clicked.
     */
    private final TreeMap<Position, Symbol> symbolMap = new TreeMap<>();

    /**
     * Computes the symbol map from the given script.
     */
    public SymbolMap(Script script) {
        script.forAllChildren(n -> {
            if (n instanceof Symbol) {
                emplace((Symbol) n);
            }
        });
    }

    private void emplace(Symbol symbol) {
        symbolMap.put(symbol.getRange().start, symbol);
    }

    /**
     * Which symbol is under the caret?
     * @return Optional.empty() if no symbol was under the caret.
     */
    public Optional<Symbol> getSelected(Position caretPosition) {
        Map.Entry<Position, Symbol> symbolEntry = symbolMap.floorEntry(caretPosition);
        if (symbolEntry == null)
            return Optional.empty();
        if (symbolEntry.getValue().getRange().contains(caretPosition)) {
            return Optional.of(symbolEntry.getValue());
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return symbolMap.toString();
    }
}
