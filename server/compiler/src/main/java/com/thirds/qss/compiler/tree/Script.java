package com.thirds.qss.compiler.tree;

import com.thirds.qss.compiler.Range;

import java.nio.file.Path;
import java.util.ArrayList;

public class Script extends Node {
    /**
     * Relative to the workspace root.
     */
    private final Path filePath;
    private final ArrayList<Struct> structs;

    public Script(Path filePath, Range range, ArrayList<Struct> structs) {
        super(range);
        this.filePath = filePath;
        this.structs = structs;
    }

    @Override
    public String toString() {
        return "Script{" +
                "filePath=" + filePath +
                ", structs=" + structs +
                '}';
    }

    public ArrayList<Struct> getStructs() {
        return structs;
    }

    public Path getFilePath() {
        return filePath;
    }
}
