package com.thirds.qss.compiler.resolve;

import com.thirds.qss.BundleQualifiedName;
import com.thirds.qss.QualifiedName;
import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.indexer.NameIndex;
import com.thirds.qss.compiler.indexer.NameIndices;
import com.thirds.qss.compiler.tree.Script;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents the possible qualified names that are represented by a given NameLiteral. Used to convert literals like
 * <code>spawn_entity</code> into qualified names like <code>std::entity::spawn_entity</code>.
 * @param <T> The type that we're searching for in dependency files.
 */
public class ResolveResult<T> {
    public final List<ResolveAlternative<T>> alternatives;

    /**
     * What are the alternatives that we could have if only we imported some other packages?
     * Don't assign any value to this if there are some valid alternatives. This is just used for helping
     * the programmer if there's any errors.
     */
    public final List<ResolveAlternative<T>> nonImportedAlternatives;

    public ResolveResult(List<ResolveAlternative<T>> alternatives, List<ResolveAlternative<T>> nonImportedAlternatives) {
        this.alternatives = alternatives;
        this.nonImportedAlternatives = nonImportedAlternatives;
    }

    public static <T> ResolveResult<T> success(List<ResolveAlternative<T>> alternatives) {
        return new ResolveResult<>(alternatives, List.of());
    }

    public static <T> ResolveResult<T> nonImported(List<ResolveAlternative<T>> nonImported) {
        return new ResolveResult<>(List.of(), nonImported);
    }

    /**
     * Resolves a name in global scope. Essentially, it loops through all the packages we've loaded searching for the
     * qualified name that matches a name written in QSS. For example, writing <code>spawn_entity</code> with the
     * <code>std::entity</code> package imported equates to the <code>std::entity::spawn_entity</code> function. This
     * function works out which globally scoped name you're referring to when you write a given name in QSS code.
     *
     * This "given name" is encapsulated by the resolver parameter; the resolver makes its own deduction about whether
     * any item in the index matches.
     *
     * This automatically deduces what alternatives are valid based on the list of imports declared by the script.
     * @param compiler The compiler we're using. By this point, we need to have generated the name indices so
     *                 we can search the dependencies for names.
     * @param script The script we're currently compiling. This is used for finding the list of import statements so
     *               that we can tell what alternatives we have for the given name.
     * @param resolver This function will be called for each name index we're searching in. It must return a
     *                 list of items that match the name we're querying. For example, we're searching for a struct
     *                 with a given name. For each name index it's given, this function will return a list of all
     *                 structs that match the given name.
     * @param <T> The type of item we're searching for. E.g. Struct (if we're searching for a struct that matches a
     *           given name), Func (if we're searching for a Func).
     */
    public static <T> ResolveResult<T> resolveGlobalScope(Compiler compiler, Script script, Function<NameIndex, List<T>> resolver) {
        ArrayList<ResolveAlternative<T>> alternatives = new ArrayList<>();

        // TODO we might want to speed up this nested for loop. Maybe we can cache a HashSet/HashMap of last segments of qualified names?

        // First, we do a run through just checking imported packages.
        for (Map.Entry<String, NameIndices.Bundle> bundleEntry : compiler.getNameIndices().getBundles().entrySet()) {
            for (Map.Entry<QualifiedName, NameIndex> indexEntry : bundleEntry.getValue().getPackages().entrySet()) {
                boolean packageWasImported = script.getImportedPackages().contains(indexEntry.getKey());
                if (!packageWasImported)
                    continue;

                NameIndex index = indexEntry.getValue();
                List<T> result = resolver.apply(index);
                for (T t : result) {
                    alternatives.add(new ResolveAlternative<>(t, List.of(new BundleQualifiedName(bundleEntry.getKey(), indexEntry.getKey()))));
                }
            }
        }

        if (!alternatives.isEmpty()) {
            // We found at least one matching name.
            return ResolveResult.success(alternatives);
        }

        // If we didn't find a matching name, redo the whole process looking in every single package, regardless
        // if it's imported. We need to tell the user which package it's actually in.
        for (Map.Entry<String, NameIndices.Bundle> bundleEntry : compiler.getNameIndices().getBundles().entrySet()) {
            for (Map.Entry<QualifiedName, NameIndex> indexEntry : bundleEntry.getValue().getPackages().entrySet()) {
                NameIndex index = indexEntry.getValue();
                List<T> result = resolver.apply(index);
                for (T t : result) {
                    alternatives.add(new ResolveAlternative<>(t, List.of(new BundleQualifiedName(bundleEntry.getKey(), indexEntry.getKey()))));
                }
            }
        }

        return ResolveResult.nonImported(alternatives);
    }

    @Override
    public String toString() {
        return "ResolveResult{" +
                "alternatives=" + alternatives +
                ", nonImportedAlternatives=" + nonImportedAlternatives +
                '}';
    }
}
