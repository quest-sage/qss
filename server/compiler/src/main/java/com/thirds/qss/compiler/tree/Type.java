package com.thirds.qss.compiler.tree;

import com.thirds.qss.BundleQualifiedName;
import com.thirds.qss.QualifiedName;
import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.indexer.TypeNameIndex;
import com.thirds.qss.compiler.indexer.TypeNameIndices;
import com.thirds.qss.compiler.lexer.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Represents a data type.
 */
public abstract class Type extends Node {
    public Type(Range range) {
        super(range);
    }

    /**
     * Represents one possible resolve of a data type.
     */
    public static class ResolveAlternative {
        /**
         * The variable type that this alternative will resolve to.
         */
        public final VariableType type;
        /**
         * What imports are required for this alternative to be resolved?
         */
        public final List<BundleQualifiedName> imports;

        public ResolveAlternative(VariableType type, List<BundleQualifiedName> imports) {
            this.type = type;
            this.imports = imports;
        }
    }

    public static class ResolveResult {
        public final List<ResolveAlternative> alternatives;

        /**
         * What are the alternatives that we could have if only we imported some other packages?
         * Don't assign any value to this if there are some valid alternatives. This is just used for helping
         * the programmer if there's any errors.
         */
        public final List<ResolveAlternative> nonImportedAlternatives;

        public ResolveResult(List<ResolveAlternative> alternatives, List<ResolveAlternative> nonImportedAlternatives) {
            this.alternatives = alternatives;
            this.nonImportedAlternatives = nonImportedAlternatives;
        }

        public static ResolveResult success(List<ResolveAlternative> alternatives) {
            return new ResolveResult(alternatives, List.of());
        }

        public static ResolveResult nonImported(List<ResolveAlternative> nonImported) {
            return new ResolveResult(List.of(), nonImported);
        }
    }

    /**
     * Computes the qualified variable type by looking up type names in the given type name index.
     * @param imports The set of imported packages in the file.
     * @return A list of possible alternatives for the resolution of the name;
     * an empty list if the type could not be found in the given type name index. If the size of the alternatives list is
     * exactly 1, the variable type is resolved.
     */
    public abstract ResolveResult resolve(Set<QualifiedName> imports, TypeNameIndices typeNameIndices);

    public static class PrimitiveType extends Type {
        private final Token token;

        public PrimitiveType(Token token) {
            super(token.getRange());
            this.token = token;
        }

        @Override
        public ResolveResult resolve(Set<QualifiedName> imports, TypeNameIndices typeNameIndices) {
            switch (token.type) {
                case KW_INT:
                    return ResolveResult.success(List.of(new ResolveAlternative(VariableType.Primitive.TYPE_INT, List.of())));
                case KW_BOOL:
                    return ResolveResult.success(List.of(new ResolveAlternative(VariableType.Primitive.TYPE_BOOL, List.of())));
                case KW_STRING:
                    return ResolveResult.success(List.of(new ResolveAlternative(VariableType.Primitive.TYPE_STRING, List.of())));
                case KW_TEXT:
                    return ResolveResult.success(List.of(new ResolveAlternative(VariableType.Primitive.TYPE_TEXT, List.of())));
                case KW_ENTITY:
                    return ResolveResult.success(List.of(new ResolveAlternative(VariableType.Primitive.TYPE_ENTITY, List.of())));
                case KW_RATIO:
                    return ResolveResult.success(List.of(new ResolveAlternative(VariableType.Primitive.TYPE_RATIO, List.of())));
                case KW_COL:
                    return ResolveResult.success(List.of(new ResolveAlternative(VariableType.Primitive.TYPE_COL, List.of())));
                case KW_POS:
                    return ResolveResult.success(List.of(new ResolveAlternative(VariableType.Primitive.TYPE_POS, List.of())));
                case KW_TEXTURE:
                    return ResolveResult.success(List.of(new ResolveAlternative(VariableType.Primitive.TYPE_TEXTURE, List.of())));
                case KW_PLAYER:
                    return ResolveResult.success(List.of(new ResolveAlternative(VariableType.Primitive.TYPE_PLAYER, List.of())));
            }
            throw new UnsupportedOperationException(token.toString());
        }
    }

    public static class StructType extends Type {
        private final NameLiteral structName;

        public StructType(Range range, NameLiteral structName) {
            super(range);
            this.structName = structName;
        }

        @Override
        public ResolveResult resolve(Set<QualifiedName> imports, TypeNameIndices typeNameIndices) {
            ArrayList<ResolveAlternative> alternatives = new ArrayList<>();
            ArrayList<ResolveAlternative> nonImportedAlternatives = new ArrayList<>();
            TypeNameIndex.StructDefinition matchedDefinition = null;

            // TODO we might want to speed up this triple-nested for loop. Maybe we can cache a HashSet/HashMap of last segments of qualified names?
            for (Map.Entry<String, TypeNameIndices.Bundle> bundleEntry : typeNameIndices.getBundles().entrySet()) {
                for (Map.Entry<QualifiedName, TypeNameIndex> indexEntry : bundleEntry.getValue().getPackages().entrySet()) {
                    TypeNameIndex index = indexEntry.getValue();
                    boolean packageWasImported = imports.contains(indexEntry.getKey());
                    if (!packageWasImported && !alternatives.isEmpty())
                        continue;

                    for (Map.Entry<String, TypeNameIndex.StructDefinition> entry : index.getStructDefinitions().entrySet()) {
                        String s = entry.getKey();
                        TypeNameIndex.StructDefinition definition = entry.getValue();

                        QualifiedName qualifiedName = index.getPackage().appendSegment(s);
                        if (structName.matches(qualifiedName)) {
                            if (packageWasImported) {
                                matchedDefinition = definition;
                                alternatives.add(new ResolveAlternative(
                                        new VariableType.Struct(qualifiedName),
                                        List.of(new BundleQualifiedName(bundleEntry.getKey(), indexEntry.getKey()))));
                            } else {
                                nonImportedAlternatives.add(new ResolveAlternative(
                                        new VariableType.Struct(qualifiedName),
                                        List.of(new BundleQualifiedName(bundleEntry.getKey(), indexEntry.getKey()))));
                            }
                        }
                    }
                }
            }

            if (alternatives.size() == 1) {
                structName.setTarget(
                        matchedDefinition.getLocation(),
                        matchedDefinition.getDocumentation()
                );
            }

            if (alternatives.isEmpty())
                return ResolveResult.nonImported(nonImportedAlternatives);
            else
                return ResolveResult.success(alternatives);
        }

        @Override
        public void forChildren(Consumer<Node> consumer) {
            consumer.accept(structName);
        }
    }
}
