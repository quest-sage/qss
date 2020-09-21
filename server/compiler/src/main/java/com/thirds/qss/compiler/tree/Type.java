package com.thirds.qss.compiler.tree;

import com.thirds.qss.QualifiedName;
import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.indexer.NameIndex;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.resolve.ResolveAlternative;
import com.thirds.qss.compiler.resolve.ResolveResult;
import com.thirds.qss.compiler.resolve.Resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Represents a data type.
 */
public abstract class Type extends Node {
    public Type(Range range) {
        super(range);
    }

    /**
     * Computes the qualified variable type by looking up type names in the given name index.
     * @return A list of possible alternatives for the resolution of the name;
     * an empty list if the type could not be found in the given name index. If the size of the alternatives list is
     * exactly 1, the variable type is resolved.
     */
    public ResolveResult<VariableType> resolve(Compiler compiler, Script script) {
        ResolveResult<VariableType> result = resolveImpl(compiler, script);
        resolved = true;
        if (result.alternatives.size() == 1)
            resolvedType = result.alternatives.get(0).value;
        return result;
    }

    protected abstract ResolveResult<VariableType> resolveImpl(Compiler compiler, Script script);

    /**
     * Will be set to true when resolve is called, regardless of whether the resolve was successful.
     */
    private boolean resolved = false;

    /**
     * If the resolve was successful, this will be set to a non-null value.
     */
    private VariableType resolvedType = null;

    /**
     * Will return true after {@link #resolve} is called, regardless of whether the resolve was successful.
     */
    public boolean isResolved() {
        return resolved;
    }

    /**
     * If {@link #resolve} was called, and the resolve was successful, this will return a non-null value.
     */
    public VariableType getResolvedType() {
        return resolvedType;
    }

    public static class PrimitiveType extends Type {
        private final Token token;

        public PrimitiveType(Token token) {
            super(token.getRange());
            this.token = token;
        }

        @Override
        public ResolveResult<VariableType> resolveImpl(Compiler compiler, Script script) {
            switch (token.type) {
                case KW_INT:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_INT, List.of())));
                case KW_BOOL:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_BOOL, List.of())));
                case KW_STRING:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_STRING, List.of())));
                case KW_TEXT:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_TEXT, List.of())));
                case KW_ENTITY:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_ENTITY, List.of())));
                case KW_RATIO:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_RATIO, List.of())));
                case KW_COL:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_COL, List.of())));
                case KW_POS:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_POS, List.of())));
                case KW_TEXTURE:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_TEXTURE, List.of())));
                case KW_PLAYER:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_PLAYER, List.of())));
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
        public ResolveResult<VariableType> resolveImpl(Compiler compiler, Script script) {
            AtomicReference<NameIndex.StructDefinition> matchedDefinition = new AtomicReference<>();
            ResolveResult<VariableType> result = Resolver.resolveGlobalScopeName(compiler, script, nameIndex -> {
                ArrayList<VariableType> alternatives = new ArrayList<>(0);
                for (Map.Entry<String, NameIndex.StructDefinition> entry : nameIndex.getStructDefinitions().entrySet()) {
                    String s = entry.getKey();
                    NameIndex.StructDefinition definition = entry.getValue();

                    QualifiedName qualifiedName = nameIndex.getPackage().appendSegment(s);
                    if (structName.matches(qualifiedName)) {
                        matchedDefinition.set(definition);
                        alternatives.add(new VariableType.Struct(qualifiedName));
                    }
                }
                return alternatives;
            });

            if (result.alternatives.size() == 1) {
                structName.setTarget(
                        matchedDefinition.get().getLocation(),
                        matchedDefinition.get().getDocumentation()
                );
            }

            return result;
        }

        @Override
        public void forChildren(Consumer<Node> consumer) {
            consumer.accept(structName);
        }
    }
}
