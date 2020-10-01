package com.thirds.qss.compiler.tree;

import com.thirds.qss.QualifiedName;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.ScriptPath;
import com.thirds.qss.compiler.tree.script.*;

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
    private final ArrayList<Documentable<FuncHook>> funcHooks;
    private final ArrayList<Documentable<GetHook>> getHooks;
    private final ArrayList<Documentable<SetHook>> setHooks;
    private final ArrayList<Documentable<NewStructHook>> newStructHooks;
    private final ArrayList<Documentable<Trait>> traits;
    private final ArrayList<Documentable<TraitImpl>> traitImpls;

    public Script(ScriptPath filePath, Range range, QualifiedName packageName, ScriptPath bundleRoot, ArrayList<Import> imports,
                  ArrayList<Documentable<Struct>> structs,
                  ArrayList<Documentable<Func>> funcs, ArrayList<Documentable<FuncHook>> funcHooks,
                  ArrayList<Documentable<GetHook>> getHooks, ArrayList<Documentable<SetHook>> setHooks,
                  ArrayList<Documentable<NewStructHook>> newStructHooks,
                  ArrayList<Documentable<Trait>> traits, ArrayList<Documentable<TraitImpl>> traitImpls) {
        super(range);
        this.filePath = filePath;
        this.packageName = packageName;
        this.bundleRoot = bundleRoot;
        this.imports = imports;
        this.structs = structs;
        this.funcs = funcs;
        this.funcHooks = funcHooks;
        this.getHooks = getHooks;
        this.setHooks = setHooks;
        this.newStructHooks = newStructHooks;
        this.traits = traits;
        this.traitImpls = traitImpls;
        importedPackages = Stream.concat(Stream.of(packageName), imports.stream().map(i -> i.packageName.toQualifiedName()))
                .collect(Collectors.toSet());

        updateAllContainers();
    }

    @Override
    public String toString() {
        return "Script{" +
                "filePath=" + filePath +
                ", packageName=" + packageName +
                ", bundleRoot=" + bundleRoot +
                ", imports=" + imports +
                ", importedPackages=" + importedPackages +
                ", structs=" + structs +
                ", funcs=" + funcs +
                ", funcHooks=" + funcHooks +
                ", getHooks=" + getHooks +
                ", setHooks=" + setHooks +
                ", newStructHooks=" + newStructHooks +
                ", traits=" + traits +
                ", traitImpls=" + traitImpls +
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

    public ArrayList<Documentable<FuncHook>> getFuncHooks() {
        return funcHooks;
    }

    public ArrayList<Documentable<GetHook>> getGetHooks() {
        return getHooks;
    }

    public ArrayList<Documentable<SetHook>> getSetHooks() {
        return setHooks;
    }

    public ArrayList<Documentable<NewStructHook>> getNewStructHooks() {
        return newStructHooks;
    }

    public ArrayList<Documentable<Trait>> getTraits() {
        return traits;
    }

    public ArrayList<Documentable<TraitImpl>> getTraitImpls() {
        return traitImpls;
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
        for (Documentable<FuncHook> funcHook : funcHooks) {
            consumer.accept(funcHook);
        }
        for (Documentable<GetHook> getHook : getHooks) {
            consumer.accept(getHook);
        }
        for (Documentable<SetHook> setHook : setHooks) {
            consumer.accept(setHook);
        }
        for (Documentable<Trait> trait : traits) {
            consumer.accept(trait);
        }
        for (Documentable<TraitImpl> traitImpl : traitImpls) {
            consumer.accept(traitImpl);
        }
    }
}
