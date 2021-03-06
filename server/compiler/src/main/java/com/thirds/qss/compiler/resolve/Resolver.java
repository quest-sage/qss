package com.thirds.qss.compiler.resolve;

import com.thirds.qss.BundleQualifiedName;
import com.thirds.qss.QssLogger;
import com.thirds.qss.QualifiedName;
import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.*;
import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.indexer.Index;
import com.thirds.qss.compiler.indexer.Indices;
import com.thirds.qss.compiler.indexer.NameIndex;
import com.thirds.qss.compiler.indexer.NameIndices;
import com.thirds.qss.compiler.tree.NameLiteral;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.tree.Script;
import com.thirds.qss.compiler.tree.Type;
import com.thirds.qss.compiler.tree.expr.Identifier;
import com.thirds.qss.compiler.tree.script.Trait;
import com.thirds.qss.compiler.tree.script.TraitImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Resolver {
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
    public static <T> ResolveResult<T> resolveGlobalScopeName(Compiler compiler, Script script, Function<NameIndex, List<T>> resolver) {
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

    /**
     * Resolves an item in global scope. Essentially, it loops through all the packages we've loaded searching for the
     * qualified name that matches a name written in QSS. For example, writing <code>spawn_entity</code> with the
     * <code>std::entity</code> package imported equates to the <code>std::entity::spawn_entity</code> function. This
     * function works out which globally scoped name you're referring to when you write a given name in QSS code.
     *
     * This "given name" is encapsulated by the resolver parameter; the resolver makes its own deduction about whether
     * any item in the index matches.
     *
     * This automatically deduces what alternatives are valid based on the list of imports declared by the script.
     * @param compiler The compiler we're using. By this point, we need to have generated the indices so
     *                 we can search the dependencies for items.
     * @param script The script we're currently compiling. This is used for finding the list of import statements so
     *               that we can tell what alternatives we have for the given name.
     * @param resolver This function will be called for each name index we're searching in. It must return a
     *                 list of items that match the name we're querying. For example, we're searching for a struct
     *                 with a given name. For each name index it's given, this function will return a list of all
     *                 structs that match the given name.
     * @param <T> The type of item we're searching for. E.g. Struct (if we're searching for a struct that matches a
     *           given name), Func (if we're searching for a Func).
     */
    public static <T> ResolveResult<T> resolveGlobalScope(Compiler compiler, Script script, Function<Index, List<T>> resolver) {
        ArrayList<ResolveAlternative<T>> alternatives = new ArrayList<>();

        // TODO we might want to speed up this nested for loop. Maybe we can cache a HashSet/HashMap of last segments of qualified names?

        // First, we do a run through just checking imported packages.
        for (Map.Entry<String, Indices.Bundle> bundleEntry : compiler.getIndices().getBundles().entrySet()) {
            for (Map.Entry<QualifiedName, Index> indexEntry : bundleEntry.getValue().getPackages().entrySet()) {
                boolean packageWasImported = script.getImportedPackages().contains(indexEntry.getKey());
                if (!packageWasImported)
                    continue;

                Index index = indexEntry.getValue();
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
        for (Map.Entry<String, Indices.Bundle> bundleEntry : compiler.getIndices().getBundles().entrySet()) {
            for (Map.Entry<QualifiedName, Index> indexEntry : bundleEntry.getValue().getPackages().entrySet()) {
                Index index = indexEntry.getValue();
                List<T> result = resolver.apply(index);
                for (T t : result) {
                    alternatives.add(new ResolveAlternative<>(t, List.of(new BundleQualifiedName(bundleEntry.getKey(), indexEntry.getKey()))));
                }
            }
        }

        return ResolveResult.nonImported(alternatives);
    }

    /**
     * @param variableName The name of the variable we're deducing the type of (will be used in error messages).
     * @param messages An output array that will contain the messages if there were any.
     */
    public static ResolveResult<VariableType> resolveType(Compiler compiler, Script script, ArrayList<Message> messages, String variableName, Type type) {
        ResolveResult<VariableType> fieldTypeAlternatives = type.resolve(compiler, script);
        TypeParameterInfo typeParameterInfo = generateTypeParameterInfo(type);
        // Resolve all instances of generics such as This to concrete types.
        fieldTypeAlternatives = new ResolveResult<>(
                fieldTypeAlternatives.alternatives
                        .stream()
                        .map(alt -> new ResolveAlternative<>(
                                resolveTypeParameters(type.getRange(), messages, alt.value, typeParameterInfo),
                                alt.imports
                        ))
                        .collect(Collectors.toCollection(ArrayList::new)),
                fieldTypeAlternatives.nonImportedAlternatives
                        .stream()
                        .map(alt -> new ResolveAlternative<>(
                                resolveTypeParameters(type.getRange(), messages, alt.value, typeParameterInfo),
                                alt.imports
                        ))
                        .collect(Collectors.toCollection(ArrayList::new))
        );
        if (fieldTypeAlternatives.alternatives.size() == 1)
            type.setResolvedType(fieldTypeAlternatives.alternatives.get(0).value);

        if (fieldTypeAlternatives.alternatives.isEmpty()) {
            StringBuilder message = new StringBuilder("Could not resolve type of ").append(variableName);
            if (!fieldTypeAlternatives.nonImportedAlternatives.isEmpty()) {
                message.append("; try one of the following:");
                for (ResolveAlternative<VariableType> alt : fieldTypeAlternatives.nonImportedAlternatives) {
                    // \u2022 is the bullet character
                    message.append("\n").append("\u2022 import ").append(alt.imports.stream().map(i -> i.name.toString()).collect(Collectors.joining(", ")));
                }
            }
            messages.add(new Message(
                    type.getRange(),
                    Message.MessageSeverity.ERROR,
                    message.toString()
            ));
        } else if (fieldTypeAlternatives.alternatives.size() > 1) {
            messages.add(new Message(
                    type.getRange(),
                    Message.MessageSeverity.ERROR,
                    "Type of " + variableName + " was ambiguous, possibilities were: " +
                            fieldTypeAlternatives.alternatives.stream().map(alt -> alt.value.toString()).collect(Collectors.joining(", "))
            ));
        }
        return fieldTypeAlternatives;
    }

    /**
     * Represents a possible resolve alternative when searching for a struct's name.
     */
    public static class StructNameAlternative {
        public final QualifiedName name;
        public final NameIndex.StructDefinition struct;

        public StructNameAlternative(QualifiedName name, NameIndex.StructDefinition struct) {
            this.name = name;
            this.struct = struct;
        }
    }

    /**
     * @param compiler The name index must be built.
     */
    public static ResolveResult<StructNameAlternative> resolveStructName(Compiler compiler, Script script, ArrayList<Message> messages, NameLiteral funcName) {
        ResolveResult<StructNameAlternative> structResolved = resolveGlobalScopeName(compiler, script, nameIndex -> {
            ArrayList<StructNameAlternative> alternatives = new ArrayList<>(0);
            nameIndex.getStructDefinitions().forEach((name, func) -> {
                QualifiedName qualifiedName = nameIndex.getPackage().appendSegment(name);
                if (funcName.matches(qualifiedName)) {
                    alternatives.add(new StructNameAlternative(qualifiedName, func));
                }
            });
            return alternatives;
        });

        if (structResolved.alternatives.isEmpty()) {
            StringBuilder message = new StringBuilder("Could not resolve struct ").append(funcName);
            if (!structResolved.nonImportedAlternatives.isEmpty()) {
                message.append("; try one of the following:");
                for (ResolveAlternative<StructNameAlternative> alt : structResolved.nonImportedAlternatives) {
                    // \u2022 is the bullet character
                    message.append("\n").append("\u2022 import ").append(alt.imports.stream().map(i -> i.name.toString()).collect(Collectors.joining(", ")));
                }
            }
            messages.add(new Message(
                    funcName.getRange(),
                    Message.MessageSeverity.ERROR,
                    message.toString()
            ));
        } else if (structResolved.alternatives.size() == 1) {
            ResolveAlternative<StructNameAlternative> resolved = structResolved.alternatives.get(0);
            funcName.setTarget(resolved.value.name, resolved.value.struct.getLocation(), resolved.value.struct.getDocumentation());
        } else {
            messages.add(new Message(
                    funcName.getRange(),
                    Message.MessageSeverity.ERROR,
                    "Reference to struct " + funcName + " was ambiguous, possibilities were: " +
                            structResolved.alternatives.stream().map(alt -> alt.value.name.toString()).collect(Collectors.joining(", "))
            ));
        }

        return structResolved;
    }

    /**
     * Represents a possible resolve alternative when searching for a trait's name.
     */
    public static class TraitNameAlternative {
        public final QualifiedName name;
        public final NameIndex.TraitDefinition trait;

        public TraitNameAlternative(QualifiedName name, NameIndex.TraitDefinition trait) {
            this.name = name;
            this.trait = trait;
        }
    }

    /**
     * @param compiler The name index must be built.
     */
    public static ResolveResult<TraitNameAlternative> resolveTraitName(Compiler compiler, Script script, ArrayList<Message> messages, NameLiteral funcName) {
        ResolveResult<TraitNameAlternative> traitResolved = resolveGlobalScopeName(compiler, script, nameIndex -> {
            ArrayList<TraitNameAlternative> alternatives = new ArrayList<>(0);
            nameIndex.getTraitDefinitions().forEach((name, trait) -> {
                QualifiedName qualifiedName = nameIndex.getPackage().appendSegment(name);
                if (funcName.matches(qualifiedName)) {
                    alternatives.add(new TraitNameAlternative(qualifiedName, trait));
                }
            });
            return alternatives;
        });

        if (traitResolved.alternatives.isEmpty()) {
            StringBuilder message = new StringBuilder("Could not resolve trait ").append(funcName);
            if (!traitResolved.nonImportedAlternatives.isEmpty()) {
                message.append("; try one of the following:");
                for (ResolveAlternative<TraitNameAlternative> alt : traitResolved.nonImportedAlternatives) {
                    // \u2022 is the bullet character
                    message.append("\n").append("\u2022 import ").append(alt.imports.stream().map(i -> i.name.toString()).collect(Collectors.joining(", ")));
                }
            }
            messages.add(new Message(
                    funcName.getRange(),
                    Message.MessageSeverity.ERROR,
                    message.toString()
            ));
        } else if (traitResolved.alternatives.size() == 1) {
            ResolveAlternative<TraitNameAlternative> resolved = traitResolved.alternatives.get(0);
            funcName.setTarget(resolved.value.name, resolved.value.trait.getLocation(), resolved.value.trait.getDocumentation());
        } else {
            messages.add(new Message(
                    funcName.getRange(),
                    Message.MessageSeverity.ERROR,
                    "Reference to trait " + funcName + " was ambiguous, possibilities were: " +
                            traitResolved.alternatives.stream().map(alt -> alt.value.name.toString()).collect(Collectors.joining(", "))
            ));
        }

        return traitResolved;
    }

    /**
     * Represents a possible resolve alternative when searching for a trait.
     */
    public static class TraitAlternative {
        public final QualifiedName name;
        public final Index.TraitDefinition trait;

        public TraitAlternative(QualifiedName name, Index.TraitDefinition trait) {
            this.name = name;
            this.trait = trait;
        }
    }

    /**
     * @param compiler The index must be built.
     */
    public static ResolveResult<TraitAlternative> resolveTrait(Compiler compiler, Script script, ArrayList<Message> messages, NameLiteral funcName) {
        ResolveResult<TraitAlternative> traitResolved = resolveGlobalScope(compiler, script, nameIndex -> {
            ArrayList<TraitAlternative> alternatives = new ArrayList<>(0);
            nameIndex.getTraitDefinitions().forEach((name, trait) -> {
                QualifiedName qualifiedName = nameIndex.getPackage().appendSegment(name);
                if (funcName.matches(qualifiedName)) {
                    alternatives.add(new TraitAlternative(qualifiedName, trait));
                }
            });
            return alternatives;
        });

        if (traitResolved.alternatives.isEmpty()) {
            StringBuilder message = new StringBuilder("Could not resolve trait ").append(funcName);
            if (!traitResolved.nonImportedAlternatives.isEmpty()) {
                message.append("; try one of the following:");
                for (ResolveAlternative<TraitAlternative> alt : traitResolved.nonImportedAlternatives) {
                    // \u2022 is the bullet character
                    message.append("\n").append("\u2022 import ").append(alt.imports.stream().map(i -> i.name.toString()).collect(Collectors.joining(", ")));
                }
            }
            messages.add(new Message(
                    funcName.getRange(),
                    Message.MessageSeverity.ERROR,
                    message.toString()
            ));
        } else if (traitResolved.alternatives.size() == 1) {
            ResolveAlternative<TraitAlternative> resolved = traitResolved.alternatives.get(0);
            funcName.setTarget(resolved.value.name, resolved.value.trait.getLocation(), resolved.value.trait.getDocumentation());
        } else {
            messages.add(new Message(
                    funcName.getRange(),
                    Message.MessageSeverity.ERROR,
                    "Reference to trait " + funcName + " was ambiguous, possibilities were: " +
                            traitResolved.alternatives.stream().map(alt -> alt.value.name.toString()).collect(Collectors.joining(", "))
            ));
        }

        return traitResolved;
    }

    /**
     * Represents a possible resolve alternative when searching for a function.
     */
    public static class FuncAlternative {
        public final QualifiedName name;
        public final Index.FuncDefinition func;

        public FuncAlternative(QualifiedName name, Index.FuncDefinition func) {
            this.name = name;
            this.func = func;
        }
    }

    /**
     * @param compiler The index must be built.
     * @param whatSearchingFor The name of what we're searching for - it might not just be a func. Possible parameters:
     *                         "name" (we're actually looking for any name and a function is just one kind of name),
     *                         "func" (we're specifically looking for a function)
     */
    public static ResolveResult<FuncAlternative> resolveFunc(Compiler compiler, Script script, ArrayList<Message> messages, Identifier funcName, String whatSearchingFor) {
        ResolveResult<FuncAlternative> funcResolved = resolveGlobalScope(compiler, script, index -> {
            ArrayList<FuncAlternative> alternatives = new ArrayList<>(0);
            index.getFuncDefinitions().forEach((name, func) -> {
                QualifiedName qualifiedName = index.getPackage().appendSegment(name);
                if (funcName.getName().matches(qualifiedName)) {
                    alternatives.add(new FuncAlternative(qualifiedName, func));
                }
            });
            index.getTraitDefinitions().forEach((name, trait) -> {
                // TODO should the qualified name include the trait name? e.g. std::Trait::foo vs std::foo?
                trait.getTraitFuncDefinitions().forEach((traitFuncName, func) -> {
                    QualifiedName qualifiedName = index.getPackage().appendSegment(traitFuncName);
                    if (funcName.getName().matches(qualifiedName)) {
                        alternatives.add(new FuncAlternative(qualifiedName, func));
                    }
                });
            });
            return alternatives;
        });

        if (funcResolved.alternatives.isEmpty()) {
            StringBuilder message = new StringBuilder("Could not resolve " + whatSearchingFor + " ").append(funcName.getName().toQualifiedName());
            if (!funcResolved.nonImportedAlternatives.isEmpty()) {
                message.append("; try one of the following:");
                for (ResolveAlternative<FuncAlternative> alt : funcResolved.nonImportedAlternatives) {
                    // \u2022 is the bullet character
                    message.append("\n").append("\u2022 import ").append(alt.imports.stream().map(i -> i.name.toString()).collect(Collectors.joining(", ")));
                }
            }
            messages.add(new Message(
                    funcName.getRange(),
                    Message.MessageSeverity.ERROR,
                    message.toString()
            ));
        } else if (funcResolved.alternatives.size() == 1) {
            ResolveAlternative<FuncAlternative> resolved = funcResolved.alternatives.get(0);
            funcName.getName().setTarget(resolved.value.name, resolved.value.func.getLocation(), resolved.value.func.getDocumentation());
            funcName.setVariableType(resolved.value.func.getType());
        } else {
            messages.add(new Message(
                    funcName.getRange(),
                    Message.MessageSeverity.ERROR,
                    "Reference to " + whatSearchingFor + " " + funcName.getName().toQualifiedName() + " was ambiguous, possibilities were: " +
                            funcResolved.alternatives.stream().map(alt -> alt.value.name.toString()).collect(Collectors.joining(", "))
            ));
        }

        return funcResolved;
    }

    /**
     * Represents a possible resolve alternative when searching for a trait implementation.
     */
    public static class TraitImplAlternative {
        public final Index.TraitImplDefinition impl;

        public TraitImplAlternative(Index.TraitImplDefinition impl) {
            this.impl = impl;
        }
    }

    /**
     * @param compiler The index must be built.
     * @param where Where should errors be emitted from?
     */
    public static ResolveResult<TraitImplAlternative> resolveTraitImpl(Compiler compiler, Script script, ArrayList<Message> messages, Range where, VariableType thisType, QualifiedName trait) {
        ResolveResult<TraitImplAlternative> implResolved = resolveGlobalScope(compiler, script, index -> {
            ArrayList<TraitImplAlternative> alternatives = new ArrayList<>(0);
            index.getTraitImplDefinitions().forEach((name, impls) -> {
                if (impls.containsKey(thisType)) {
                    alternatives.add(new TraitImplAlternative(impls.get(thisType)));
                }
            });
            return alternatives;
        });

        if (implResolved.alternatives.isEmpty()) {
            StringBuilder message = new StringBuilder("Could not resolve impl of ").append(trait).append(" for ").append(thisType);
            if (!implResolved.nonImportedAlternatives.isEmpty()) {
                message.append("; try one of the following:");
                for (ResolveAlternative<TraitImplAlternative> alt : implResolved.nonImportedAlternatives) {
                    // \u2022 is the bullet character
                    message.append("\n").append("\u2022 import ").append(alt.imports.stream().map(i -> i.name.toString()).collect(Collectors.joining(", ")));
                }
            }
            messages.add(new Message(
                    where,
                    Message.MessageSeverity.ERROR,
                    message.toString()
            ));
        } else if (implResolved.alternatives.size() > 1) {
            String message = "Reference to impl of " + trait + " for " + thisType +
                    " was ambiguous, possibilities were: " +
                    implResolved.alternatives.stream().map(alt -> alt.imports.get(0).getName().toString()).collect(Collectors.joining(", "));
            messages.add(new Message(
                    where,
                    Message.MessageSeverity.ERROR,
                    message
            ));
        }

        return implResolved;
    }

    /**
     * Represents a possible resolve alternative when searching for a struct's field.
     */
    public static class StructFieldAlternative {
        public final Location location;
        public final String documentation;
        public final QualifiedName name;
        public final VariableType type;

        public StructFieldAlternative(Location location, String documentation, QualifiedName name, VariableType type) {
            this.location = location;
            this.documentation = documentation;
            this.name = name;
            this.type = type;
        }
    }

    /**
     * @param compiler The index must be built.
     */
    public static ResolveResult<StructFieldAlternative> resolveStructField(Compiler compiler, Script script, ArrayList<Message> messages, QualifiedName structName, NameLiteral fieldName) {
        ResolveResult<StructFieldAlternative> fieldResolved = resolveGlobalScope(compiler, script, index -> {
            ArrayList<StructFieldAlternative> alternatives = new ArrayList<>(0);

            // Check if we're even in the right package for the struct.
            if (!index.getPackage().equals(structName.trimLastSegment()))
                return alternatives;

            Index.StructDefinition structDefinition = index.getStructDefinitions().get(structName.lastSegment());
            if (structDefinition != null) {
                for (Map.Entry<String, Index.FieldDefinition> field : structDefinition.getFields().entrySet()) {
                    QualifiedName qualifiedName = index.getPackage().appendSegment(field.getKey());
                    if (fieldName.matches(qualifiedName)) {
                        Index.FieldDefinition fieldDefinition = field.getValue();
                        StructFieldAlternative alt = new StructFieldAlternative(
                                fieldDefinition.getLocation(),
                                fieldDefinition.getDocumentation(),
                                qualifiedName, fieldDefinition.getVariableType()
                        );
                        alternatives.add(alt);
                    }
                }
            }

            return alternatives;
        });

        if (fieldResolved.alternatives.isEmpty()) {
            StringBuilder message = new StringBuilder("Could not resolve field ").append(fieldName.toQualifiedName()).append(" belonging to struct ").append(structName);
            if (!fieldResolved.nonImportedAlternatives.isEmpty()) {
                message.append("; try one of the following:");
                for (ResolveAlternative<StructFieldAlternative> alt : fieldResolved.nonImportedAlternatives) {
                    // \u2022 is the bullet character
                    message.append("\n").append("\u2022 import ").append(alt.imports.stream().map(i -> i.name.toString()).collect(Collectors.joining(", ")));
                }
            }
            messages.add(new Message(
                    fieldName.getRange(),
                    Message.MessageSeverity.ERROR,
                    message.toString()
            ));
        } else if (fieldResolved.alternatives.size() == 1) {
            ResolveAlternative<StructFieldAlternative> resolved = fieldResolved.alternatives.get(0);
            fieldName.setTarget(resolved.value.name, resolved.value.location, resolved.value.documentation);
        } else {
            messages.add(new Message(
                    fieldName.getRange(),
                    Message.MessageSeverity.ERROR,
                    "Reference to field " + fieldName.toQualifiedName() + " belonging to struct " + structName + " was ambiguous, possibilities were: " +
                            fieldResolved.alternatives.stream().map(alt -> alt.value.name.toString()).collect(Collectors.joining(", "))
            ));
        }

        return fieldResolved;
    }

    /**
     * Converts a variable type that may or may not contain type parameters (e.g. This, T) to a concrete
     * variable type by substituting the given type parameters. If the concrete type of a type parameter was not
     * listed in the type parameter info, an error message will be printed.
     * @param where Where was this type from in the script? We need a location so we can print error messages there.
     */
    public static VariableType resolveTypeParameters(Range where, ArrayList<Message> messages, VariableType type, TypeParameterInfo typeParams) {
        if (type instanceof VariableType.This) {
            if (typeParams.thisType == null) {
                messages.add(new Message(
                        where,
                        Message.MessageSeverity.ERROR,
                        "'This' type is not allowed outside traits and impl blocks"
                ));
                return VariableType.Primitive.TYPE_UNKNOWN;
            } else {
                return typeParams.thisType;
            }
        } else if (type instanceof VariableType.Maybe) {
            return new VariableType.Maybe(
                    resolveTypeParameters(where, messages, ((VariableType.Maybe) type).getContentsType(), typeParams)
            );
        } else if (type instanceof VariableType.List) {
            return new VariableType.List(
                    resolveTypeParameters(where, messages, ((VariableType.List) type).getElementType(), typeParams)
            );
        } else if (type instanceof VariableType.Map) {
            return new VariableType.Map(
                    resolveTypeParameters(where, messages, ((VariableType.Map) type).getKeyType(), typeParams),
                    resolveTypeParameters(where, messages, ((VariableType.Map) type).getValueType(), typeParams)
            );
        } else if (type instanceof VariableType.Function) {
            VariableType.Function result = new VariableType.Function(
                    ((VariableType.Function) type).isReceiverStyle(),
                    ((VariableType.Function) type).getParams().stream().map(vt -> resolveTypeParameters(where, messages, vt, typeParams)).collect(Collectors.toCollection(ArrayList::new)),
                    resolveTypeParameters(where, messages, ((VariableType.Function) type).getReturnType(), typeParams)
            );
            result.setPurity(((VariableType.Function) type).getPurity());
            result.setNative(((VariableType.Function) type).isNative());
            return result;
        }

        // No conversion is necessary.
        return type;
    }

    public static class TypeParameterInfo {
        /**
         * If we're in a trait impl, this is set to the type we're implementing the trait for.
         * If we're in a trait definition, this is set to VariableType.This.
         */
        private final VariableType thisType;

        public TypeParameterInfo(VariableType thisType) {
            this.thisType = thisType;
        }
    }

    /**
     * Computes the type parameter info at a specific place in code.
     */
    public static TypeParameterInfo generateTypeParameterInfo(Node where) {
        // Compute the This type.
        VariableType thisType = where.getContainerOfType(Trait.class).map(trait ->
                (VariableType) new VariableType.This()
        ).or(() -> where.getContainerOfType(TraitImpl.class).map(traitImpl ->
                traitImpl.getType().getResolvedType()
        )).orElse(null);
        QssLogger.logger.atInfo().log("Generated This=%s at %s", thisType, where);
        return new TypeParameterInfo(thisType);
    }
}
