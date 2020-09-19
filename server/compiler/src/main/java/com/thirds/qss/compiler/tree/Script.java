package com.thirds.qss.compiler.tree;

import com.thirds.qss.QualifiedName;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.ScriptPath;

import java.util.ArrayList;
import java.util.function.Consumer;

public class Script extends Node {
    /**
     * Relative to the workspace root.
     */
    private final ScriptPath filePath;
    private final NameLiteral packageName;
    private final ArrayList<Documentable<Struct>> structs;

    public Script(ScriptPath filePath, Range range, NameLiteral packageName, ArrayList<Documentable<Struct>> structs) {
        super(range);
        this.filePath = filePath;
        this.packageName = packageName;
        this.structs = structs;
    }

    @Override
    public String toString() {
        return "Script{" +
                "filePath=" + filePath +
                ", packageName=" + packageName +
                ", structs=" + structs +
                '}';
    }

    public ArrayList<Documentable<Struct>> getStructs() {
        return structs;
    }

    public NameLiteral getPackageName() {
        return packageName;
    }

    public ScriptPath getFilePath() {
        return filePath;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        for (Documentable<Struct> struct : structs) {
            consumer.accept(struct);
        }
    }
}
