package com.thirds.qss.compiler.tree;

import com.thirds.qss.QualifiedName;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.ScriptPath;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Script extends Node {
    /**
     * Relative to the workspace root.
     */
    private final ScriptPath filePath;
    private final QualifiedName packageName;
    private final ScriptPath bundleRoot;
    private final ArrayList<Import> imports;
    private final Set<QualifiedName> importedPackages;

    private final ArrayList<Documentable<Struct>> structs;
    private final ArrayList<Documentable<Func>> funcs;

    public Script(ScriptPath filePath, Range range, QualifiedName packageName, ScriptPath bundleRoot, ArrayList<Import> imports,
                  ArrayList<Documentable<Struct>> structs, ArrayList<Documentable<Func>> funcs) {
        super(range);
        this.filePath = filePath;
        this.packageName = packageName;
        this.bundleRoot = bundleRoot;
        this.imports = imports;
        this.structs = structs;
        this.funcs = funcs;
        importedPackages = Stream.concat(Stream.of(packageName), imports.stream().map(i -> i.packageName.toQualifiedName()))
                .collect(Collectors.toSet());
    }

    @Override
    public String
    toString() {
        return "Script{" +
                "filePath=" + filePath +
                ", packageName=" + packageName +
                ", bundleRoot=" + bundleRoot +
                ", imports=" + imports +
                ", importedPackages=" + importedPackages +
                ", structs=" + structs +
                ", funcs=" + funcs +
                '}';
    }

    public ArrayList<Import> getImports() {
        return imports;
    }

    public Set<QualifiedName> getImportedPackages() {
        return importedPackages;
    }

    public ArrayList<Documentable<Struct>> getStructs() {
        return structs;
    }

    public ArrayList<Documentable<Func>> getFuncs() {
        return funcs;
    }

    public QualifiedName getPackageName() {
        return packageName;
    }

    public ScriptPath getBundleRoot() {
        return bundleRoot;
    }

    public String getBundle() {
        if (bundleRoot.getSegments().isEmpty()) {
            return "bundle";
        }
        return bundleRoot.lastSegment();
    }

    public ScriptPath getFilePath() {
        return filePath;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        for (Import anImport : imports) {
            consumer.accept(anImport);
        }
        for (Documentable<Struct> struct : structs) {
            consumer.accept(struct);
        }
        for (Documentable<Func> func : funcs) {
            consumer.accept(func);
        }
    }
}
