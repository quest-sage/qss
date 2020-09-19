package com.thirds.qss.compiler.tree;

import com.thirds.qss.QualifiedName;
import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.indexer.TypeNameIndex;
import com.thirds.qss.compiler.indexer.TypeNameIndices;
import com.thirds.qss.compiler.lexer.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a data type.
 */
public abstract class Type extends Node {
    public Type(Range range) {
        super(range);
    }

    /**
     * Computes the qualified variable type by looking up type names in the given type index.
     * @return A list of possible alternatives for the resolution of the name;
     * an empty list if the type could not be found in the given type index. If the size of the list is exactly 1,
     * the variable type is resolved.
     */
    public abstract List<VariableType> resolve(TypeNameIndices typeNameIndices);

    public static class PrimitiveType extends Type {
        private final Token token;

        public PrimitiveType(Token token) {
            super(token.getRange());
            this.token = token;
        }

        @Override
        public List<VariableType> resolve(TypeNameIndices typeNameIndices) {
            switch (token.type) {
                case KW_INT:
                    return List.of(VariableType.Primitive.TYPE_INT);
                case KW_BOOL:
                    return List.of(VariableType.Primitive.TYPE_BOOL);
                case KW_STRING:
                    return List.of(VariableType.Primitive.TYPE_STRING);
                case KW_TEXT:
                    return List.of(VariableType.Primitive.TYPE_TEXT);
                case KW_ENTITY:
                    return List.of(VariableType.Primitive.TYPE_ENTITY);
                case KW_RATIO:
                    return List.of(VariableType.Primitive.TYPE_RATIO);
                case KW_COL:
                    return List.of(VariableType.Primitive.TYPE_COL);
                case KW_POS:
                    return List.of(VariableType.Primitive.TYPE_POS);
                case KW_TEXTURE:
                    return List.of(VariableType.Primitive.TYPE_TEXTURE);
                case KW_PLAYER:
                    return List.of(VariableType.Primitive.TYPE_PLAYER);
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
        public List<VariableType> resolve(TypeNameIndices typeNameIndices) {
            ArrayList<VariableType> alternatives = new ArrayList<>();
            TypeNameIndex.StructDefinition matchedDefinition = null;

            // TODO we might want to speed up this triple-nested for loop. Maybe we can cache a HashSet/HashMap of last segments of qualified names?
            for (Map.Entry<String, TypeNameIndices.Bundle> bundleEntry : typeNameIndices.getBundles().entrySet()) {
                for (Map.Entry<QualifiedName, TypeNameIndex> indexEntry : bundleEntry.getValue().getPackages().entrySet()) {
                    TypeNameIndex index = indexEntry.getValue();

                    for (Map.Entry<String, TypeNameIndex.StructDefinition> entry : index.getStructDefinitions().entrySet()) {
                        String s = entry.getKey();
                        TypeNameIndex.StructDefinition definition = entry.getValue();

                        QualifiedName qualifiedName = index.getPackage().appendSegment(s);
                        if (structName.matches(qualifiedName)) {
                            matchedDefinition = definition;
                            alternatives.add(new VariableType.Struct(qualifiedName));
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

            return alternatives;
        }

        @Override
        public void forChildren(Consumer<Node> consumer) {
            consumer.accept(structName);
        }
    }
}
