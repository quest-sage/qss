package com.thirds.qss.compiler;

import java.nio.file.Path;

/**
 * Represents a location inside any text file - not necessarily the one currently being validated/parsed.
 */
public class Location {
    /**
     * Relative to the workspace root.
     */
    private final Path filePath;
    private final Range range;

    public Location(Path filePath, Range range) {
        this.filePath = filePath;
        this.range = range;
    }

    public Path getFilePath() {
        return filePath;
    }

    public Range getRange() {
        return range;
    }
}
