package com.thirds.qss.compiler.tree;

import com.thirds.qss.QualifiedName;
import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.indexer.TypeNameIndex;
import com.thirds.qss.compiler.lexer.Token;

import java.util.Map;
import java.util.Optional;
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
     * @return Optional.empty if the type could not be found in the given type index.
     */
    public abstract Optional<VariableType> resolve(TypeNameIndex typeNameIndex);

    public static class PrimitiveType extends Type {
        private final Token token;

        public PrimitiveType(Token token) {
            super(token.getRange());
            this.token = token;
        }

        @Override
        public Optional<VariableType> resolve(TypeNameIndex typeNameIndex) {
            switch (token.type) {
                case INT:
                    return Optional.of(VariableType.Primitive.TYPE_INT);
                case BOOL:
                    return Optional.of(VariableType.Primitive.TYPE_BOOL);
                case STRING:
                    return Optional.of(VariableType.Primitive.TYPE_STRING);
                case TEXT:
                    return Optional.of(VariableType.Primitive.TYPE_TEXT);
                case ENTITY:
                    return Optional.of(VariableType.Primitive.TYPE_ENTITY);
                case RATIO:
                    return Optional.of(VariableType.Primitive.TYPE_RATIO);
                case COL:
                    return Optional.of(VariableType.Primitive.TYPE_COL);
                case POS:
                    return Optional.of(VariableType.Primitive.TYPE_POS);
                case TEXTURE:
                    return Optional.of(VariableType.Primitive.TYPE_TEXTURE);
                case PLAYER:
                    return Optional.of(VariableType.Primitive.TYPE_PLAYER);
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
        public Optional<VariableType> resolve(TypeNameIndex typeNameIndex) {
            for (Map.Entry<String, TypeNameIndex.StructDefinition> entry : typeNameIndex.getStructDefinitions().entrySet()) {
                String s = entry.getKey();
                TypeNameIndex.StructDefinition definition = entry.getValue();

                QualifiedName qualifiedName = typeNameIndex.getPackage().appendSegment(s);
                if (structName.matches(qualifiedName)) {
                    structName.setTarget(
                            definition.getLocation(),
                            definition.getDocumentation()
                    );
                    return Optional.of(new VariableType.Struct(qualifiedName));
                }
            }
            return Optional.empty();
        }

        @Override
        public void forChildren(Consumer<Node> consumer) {
            consumer.accept(structName);
        }
    }
}
