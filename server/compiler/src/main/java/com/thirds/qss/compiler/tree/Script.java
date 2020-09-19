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
    private final QualifiedName packageName;
    private final ScriptPath bundleRoot;
    private final ArrayList<Documentable<Struct>> structs;

    public Script(ScriptPath filePath, Range range, QualifiedName packageName, ScriptPath bundleRoot, ArrayList<Documentable<Struct>> structs) {
        super(range);
        this.filePath = filePath;
        this.packageName = packageName;
        this.bundleRoot = bundleRoot;
        this.structs = structs;
    }

    @Override
    public String toString() {
        return "Script{" +
                "filePath=" + filePath +
                ", structs=" + structs +
                '}';
    }

    public ArrayList<Documentable<Struct>> getStructs() {
        return structs;
    }

    public QualifiedName getPackageName() {
        return packageName;
    }

    public ScriptPath getBundleRoot() {
        return bundleRoot;
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
