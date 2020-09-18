package com.thirds.qss.compiler;

import com.thirds.qss.QualifiedName;

/**
 * Represents a location inside any text file - not necessarily the one currently being validated/parsed.
 */
public class Location {
    private final QualifiedName packageName;
    private final String fileName;
    private final Range range;

    public Location(QualifiedName packageName, String fileName, Range range) {
        this.packageName = packageName;
        this.fileName = fileName;
        this.range = range;
    }

    public QualifiedName getPackageName() {
        return packageName;
    }

    public String getFileName() {
        return fileName;
    }

    public Range getRange() {
        return range;
    }
}
