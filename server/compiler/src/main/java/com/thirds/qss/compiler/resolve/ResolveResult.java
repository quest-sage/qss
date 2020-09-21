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

    @Override
    public String toString() {
        return "ResolveResult{" +
                "alternatives=" + alternatives +
                ", nonImportedAlternatives=" + nonImportedAlternatives +
                '}';
    }
}
