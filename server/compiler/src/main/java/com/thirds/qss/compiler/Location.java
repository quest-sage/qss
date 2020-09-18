package com.thirds.qss.compiler;

/**
 * Represents a location inside any text file - not necessarily the one currently being validated/parsed.
 */
public class Location {
    private final ScriptPath filePath;
    private final Range range;

    public Location(ScriptPath filePath, Range range) {
        this.filePath = filePath;
        this.range = range;
    }

    public ScriptPath getFilePath() {
        return filePath;
    }

    public Range getRange() {
        return range;
    }
}
