package com.thirds.qss.compiler.tree;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.resolve.ResolveAlternative;
import com.thirds.qss.compiler.resolve.ResolveResult;
import com.thirds.qss.compiler.resolve.Resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Represents a data type.
 */
public abstract class Type extends Node {
    public Type(Range range) {
        super(range);
    }

    /**
     * DO NOT CALL MANUALLY: Use Resolver.deduceType - this produces a list of messages for you to output to the
     * user!
     *
     * <p>Computes the qualified variable type by looking up type names in the given name index.
     * @return A list of possible alternatives for the resolution of the name;
     * an empty list if the type could not be found in the given name index. If the size of the alternatives list is
     * exactly 1, the variable type is resolved.
     */
    public ResolveResult<VariableType> resolve(Compiler compiler, Script script) {
        ResolveResult<VariableType> result = resolveImpl(compiler, script);
        resolved = true;
        if (result.alternatives.size() == 1)
            resolvedType = result.alternatives.get(0).value;
        else
            resolvedType = VariableType.Primitive.TYPE_UNKNOWN;
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
     * If {@link #resolve} was called, this will return a non-null value.
     * If the resolve was unsuccessful, this will return VariableType.Primitive.TYPE_UNKNOWN.
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
                case T_INT:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_INT, List.of())));
                case T_BOOL:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_BOOL, List.of())));
                case T_STRING:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_STRING, List.of())));
                case T_TEXT:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_TEXT, List.of())));
                case T_ENTITY:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_ENTITY, List.of())));
                case T_RATIO:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_RATIO, List.of())));
                case T_COL:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_COL, List.of())));
                case T_POS:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_POS, List.of())));
                case T_TEXTURE:
                    return ResolveResult.success(List.of(new ResolveAlternative<>(VariableType.Primitive.TYPE_TEXTURE, List.of())));
                case T_PLAYER:
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
            ArrayList<Message> messages = new ArrayList<>();
            ResolveResult<Resolver.StructNameAlternative> result = Resolver.resolveStructName(compiler, script, messages, structName);
            if (result.alternatives.size() >= 1) {
                return ResolveResult.success(result.alternatives
                        .stream()
                        .map(alt -> new ResolveAlternative<>((VariableType) new VariableType.Struct(alt.value.name), alt.imports))
                        .collect(Collectors.toList()));
            } else {
                return ResolveResult.nonImported(result.nonImportedAlternatives
                        .stream()
                        .map(alt -> new ResolveAlternative<>((VariableType) new VariableType.Struct(alt.value.name), alt.imports))
                        .collect(Collectors.toList()));
            }
        }

        @Override
        public void forChildren(Consumer<Node> consumer) {
            consumer.accept(structName);
        }
    }

    public static class MaybeType extends Type {
        private final Type contentsType;

        public MaybeType(Type contentsType, Token maybeToken) {
            super(Range.combine(contentsType.getRange(), maybeToken.getRange()));
            this.contentsType = contentsType;
        }

        @Override
        public ResolveResult<VariableType> resolveImpl(Compiler compiler, Script script) {
            ResolveResult<VariableType> contentsTypeResolved = contentsType.resolve(compiler, script);
            ArrayList<ResolveAlternative<VariableType>> alternatives = new ArrayList<>(1);
            ArrayList<ResolveAlternative<VariableType>> nonImportedAlternatives = new ArrayList<>(0);

            for (ResolveAlternative<VariableType> alternative : contentsTypeResolved.alternatives) {
                alternatives.add(new ResolveAlternative<>(
                        new VariableType.Maybe(alternative.value),
                        alternative.imports
                ));
            }
            for (ResolveAlternative<VariableType> alternative : contentsTypeResolved.nonImportedAlternatives) {
                nonImportedAlternatives.add(new ResolveAlternative<>(
                        new VariableType.Maybe(alternative.value),
                        alternative.imports
                ));
            }

            return new ResolveResult<>(alternatives, nonImportedAlternatives);
        }

        @Override
        public void forChildren(Consumer<Node> consumer) {
            consumer.accept(contentsType);
        }
    }

    public static class ListType extends Type {
        private final Type elementType;

        public ListType(Type elementType, Token startToken, Token endToken) {
            super(Range.combine(startToken.getRange(), endToken.getRange()));
            this.elementType = elementType;
        }

        @Override
        public ResolveResult<VariableType> resolveImpl(Compiler compiler, Script script) {
            ResolveResult<VariableType> elementTypeResolved = elementType.resolve(compiler, script);
            ArrayList<ResolveAlternative<VariableType>> alternatives = new ArrayList<>(1);
            ArrayList<ResolveAlternative<VariableType>> nonImportedAlternatives = new ArrayList<>(0);

            for (ResolveAlternative<VariableType> alternative : elementTypeResolved.alternatives) {
                alternatives.add(new ResolveAlternative<>(
                        new VariableType.List(alternative.value),
                        alternative.imports
                ));
            }
            for (ResolveAlternative<VariableType> alternative : elementTypeResolved.nonImportedAlternatives) {
                nonImportedAlternatives.add(new ResolveAlternative<>(
                        new VariableType.List(alternative.value),
                        alternative.imports
                ));
            }

            return new ResolveResult<>(alternatives, nonImportedAlternatives);
        }

        @Override
        public void forChildren(Consumer<Node> consumer) {
            consumer.accept(elementType);
        }
    }

    public static class MapType extends Type {
        private final Type keyType;
        private final Type valueType;

        public MapType(Type keyType, Type valueType, Token startToken, Token endToken) {
            super(Range.combine(startToken.getRange(), endToken.getRange()));
            this.keyType = keyType;
            this.valueType = valueType;
        }

        @Override
        public ResolveResult<VariableType> resolveImpl(Compiler compiler, Script script) {
            ResolveResult<VariableType> keyTypeResolved = keyType.resolve(compiler, script);
            ResolveResult<VariableType> valueTypeResolved = valueType.resolve(compiler, script);

            ArrayList<ResolveAlternative<VariableType>> alternatives = new ArrayList<>(1);
            ArrayList<ResolveAlternative<VariableType>> nonImportedAlternatives = new ArrayList<>(0);

            // Zip all possible alternatives and non-imported alternatives for key/value types together.
            for (ResolveAlternative<VariableType> alternative : keyTypeResolved.alternatives) {
                for (ResolveAlternative<VariableType> alternative2 : valueTypeResolved.alternatives) {
                    alternatives.add(new ResolveAlternative<>(
                            new VariableType.Map(alternative.value, alternative2.value),
                            new ArrayList<>() {{
                                addAll(alternative.imports);
                                addAll(alternative2.imports);
                            }}
                    ));
                }
                for (ResolveAlternative<VariableType> alternative2 : valueTypeResolved.nonImportedAlternatives) {
                    nonImportedAlternatives.add(new ResolveAlternative<>(
                            new VariableType.Map(alternative.value, alternative2.value),
                            new ArrayList<>() {{
                                addAll(alternative.imports);
                                addAll(alternative2.imports);
                            }}
                    ));
                }
            }
            for (ResolveAlternative<VariableType> alternative : keyTypeResolved.nonImportedAlternatives) {
                for (ResolveAlternative<VariableType> alternative2 : valueTypeResolved.alternatives) {
                    nonImportedAlternatives.add(new ResolveAlternative<>(
                            new VariableType.Map(alternative.value, alternative2.value),
                            new ArrayList<>() {{
                                addAll(alternative.imports);
                                addAll(alternative2.imports);
                            }}
                    ));
                }
                for (ResolveAlternative<VariableType> alternative2 : valueTypeResolved.nonImportedAlternatives) {
                    nonImportedAlternatives.add(new ResolveAlternative<>(
                            new VariableType.Map(alternative.value, alternative2.value),
                            new ArrayList<>() {{
                                addAll(alternative.imports);
                                addAll(alternative2.imports);
                            }}
                    ));
                }
            }

            return new ResolveResult<>(alternatives, nonImportedAlternatives);
        }

        @Override
        public void forChildren(Consumer<Node> consumer) {
            consumer.accept(keyType);
            consumer.accept(valueType);
        }
    }
}
