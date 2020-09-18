package com.thirds.qss.compiler.tree;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.ScriptPath;

import java.util.ArrayList;

public class Script extends Node {
    /**
     * Relative to the workspace root.
     */
    private final ScriptPath filePath;
    private final ArrayList<Struct> structs;

    public Script(ScriptPath filePath, Range range, ArrayList<Struct> structs) {
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

    public ScriptPath getFilePath() {
        return filePath;
    }
}
