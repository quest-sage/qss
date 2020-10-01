package com.thirds.qss.compiler.validator;

import com.thirds.qss.QualifiedName;
import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.ScriptPath;
import com.thirds.qss.compiler.lexer.TokenType;
import com.thirds.qss.compiler.resolve.ResolveResult;
import com.thirds.qss.compiler.resolve.Resolver;
import com.thirds.qss.compiler.tree.Documentable;
import com.thirds.qss.compiler.tree.NameLiteral;
import com.thirds.qss.compiler.tree.Script;
import com.thirds.qss.compiler.tree.Type;
import com.thirds.qss.compiler.tree.script.*;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class Validator {
    private final Compiler compiler;
    private final Script script;
    private final ScriptPath filePath;
    private final ArrayList<Message> messages = new ArrayList<>();

    public Validator(Compiler compiler, Script script, ScriptPath filePath) {
        this.compiler = compiler;
        this.script = script;
        this.filePath = filePath;
    }

    /**
     * Execute some validation checks on the script that was passed in via the constructor.
     * @return A list of error/warning/info messages to display to the user.
     */
    public ArrayList<Message> validate() {
        messages.clear();

        checkFuncHookType();
        checkGetSetHookType();
        checkFuncThis();
        checkTraitImpl();

        return messages;
    }

    private VariableType.Function generateFunctionType(FuncOrHook func) {
        ArrayList<VariableType> params = func.getParamList().getParams()
                .stream().map(param -> param.getType().getResolvedType())
                .collect(Collectors.toCollection(ArrayList::new));
        VariableType.Function function = new VariableType.Function(
                false,
                params,
                func.getReturnType() == null ? null : func.getReturnType().getResolvedType()
        );
        function.setPurity(func.getPurity());
        return function;
    }

    /**
     * Ensure that all function hooks' signatures match the signature of the original function.
     * This resolves the jump-to-definition link on the function hook.
     */
    private void checkFuncHookType() {
        for (Documentable<FuncHook> funcHook : script.getFuncHooks()) {
            ResolveResult<Resolver.FuncAlternative> result = Resolver.resolveFunc(compiler, script, messages, funcHook.getContent().getName(), "func");
            if (result.alternatives.size() == 1) {
                // The resolve succeeded.
                VariableType.Function actualType = generateFunctionType(funcHook.getContent());
                VariableType.Function expectedType = result.alternatives.get(0).value.func.getType();
                if (!actualType.equals(expectedType)) {
                    messages.add(new Message(
                            funcHook.getContent().getName().getRange(),
                            Message.MessageSeverity.ERROR,
                            "Hook was of incorrect type; expected " + expectedType + ", got " + actualType
                    ).addInfo(new Message.MessageRelatedInformation(
                            result.alternatives.get(0).value.func.getLocation(),
                            "Original function was defined here"
                    )));
                }
            }
        }
    }

    /**
     * Ensure that all get/set hooks' signatures match the type of the original struct and field.
     * This resolves the jump-to-definition links on the get/set hook.
     */
    private void checkGetSetHookType() {
        for (Documentable<GetHook> getHook : script.getGetHooks()) {
            checkGetSetHookType(
                    getHook.getContent().getStructName(),
                    getHook.getContent().getFieldName(),
                    getHook.getContent().getFieldType()
            );
        }
        for (Documentable<SetHook> setHook : script.getSetHooks()) {
            checkGetSetHookType(
                    setHook.getContent().getStructName(),
                    setHook.getContent().getFieldName(),
                    setHook.getContent().getFieldType()
            );
        }
    }

    private void checkGetSetHookType(NameLiteral structName, NameLiteral fieldName, Type fieldType) {
        ResolveResult<Resolver.StructNameAlternative> structResolved = Resolver.resolveStructName(compiler, script, messages, structName);
        if (structResolved.alternatives.size() == 1) {
            ResolveResult<Resolver.StructFieldAlternative> fieldResolved = Resolver.resolveStructField(compiler, script, messages, structResolved.alternatives.get(0).value.name, fieldName);
            if (fieldResolved.alternatives.size() == 1) {
                // The resolve succeeded.
                VariableType actualType = fieldType.getResolvedType();
                VariableType expectedType = fieldResolved.alternatives.get(0).value.type;
                if (!actualType.equals(expectedType)) {
                    messages.add(new Message(
                            fieldType.getRange(),
                            Message.MessageSeverity.ERROR,
                            "Hook was of incorrect type; expected " + expectedType + ", got " + actualType
                    ).addInfo(new Message.MessageRelatedInformation(
                            fieldResolved.alternatives.get(0).value.location,
                            "Original function was defined here"
                    )));
                }
            }
        }
    }

    /**
     * Check that the keyword 'this' is only used in argument 0 position.
     */
    private void checkFuncThis() {
        for (Documentable<Func> func : script.getFuncs()) {
            ArrayList<Param> params = func.getContent().getParamList().getParams();
            for (int i = 1; i < params.size(); i++) {
                if (params.get(i).getName().type == TokenType.KW_THIS) {
                    messages.add(new Message(
                            params.get(i).getName().getRange(),
                            Message.MessageSeverity.ERROR,
                            "Only the first function parameter may be named 'this'"
                    ));
                }
            }
        }
    }

    /**
     * Check that trait implementations actually implement all the required functions.
     */
    private void checkTraitImpl() {
        for (Documentable<TraitImpl> traitImpl : script.getTraitImpls()) {
            // We need to re-resolve the trait name using the normal index not the name index.
            // By doing this we can see the names and types of all the trait functions.
            QualifiedName originalTraitName = traitImpl.getContent().getTrait().getTargetQualifiedName();
            if (originalTraitName == null)
                continue;

            ResolveResult<Resolver.TraitAlternative> resolved = Resolver.resolveTrait(compiler, script, messages, traitImpl.getContent().getTrait());
            if (!traitImpl.getContent().getTrait().getTargetQualifiedName().equals(originalTraitName) || resolved.alternatives.size() != 1) {
                // We've got a problem - the second resolve resolved to a different qualified name (or failed to resolve)!
                // This is a compiler bug.
                throw new UnsupportedOperationException(traitImpl.getContent().getTrait().toString() + " != " + originalTraitName + " (" + resolved.alternatives.size() + ")");
            }

            Resolver.TraitAlternative trait = resolved.alternatives.get(0).value;

            Resolver.TypeParameterInfo typeParameterInfo = Resolver.generateTypeParameterInfo(traitImpl.getContent());

            // Check that all the required trait functions were correctly implemented.
            trait.trait.getTraitFuncDefinitions().forEach((funcName, funcDefinition) -> {
                boolean wasImplemented = false;
                for (Documentable<Func> funcImpl : traitImpl.getContent().getFuncImpls()) {
                    if (funcImpl.getContent().getName().contents.equals(funcName)) {
                        wasImplemented = true;

                        // Check that the implementation of the trait func had the right type.
                        VariableType.Function actualType = generateFunctionType(funcImpl.getContent());
                        actualType = (VariableType.Function) Resolver.resolveTypeParameters(funcImpl.getRange(), messages, actualType, typeParameterInfo);
                        VariableType.Function expectedType = funcDefinition.getType();
                        expectedType = (VariableType.Function) Resolver.resolveTypeParameters(funcImpl.getRange(), messages, expectedType, typeParameterInfo);
                        if (!actualType.equals(expectedType)) {
                            messages.add(new Message(
                                    funcImpl.getContent().getName().getRange(),
                                    Message.MessageSeverity.ERROR,
                                    "Trait function implementation was of incorrect type; expected " + expectedType + ", got " + actualType
                            ).addInfo(new Message.MessageRelatedInformation(
                                    funcDefinition.getLocation(),
                                    "Original function was defined here"
                            )));
                        }

                        break;
                    }
                }

                if (!wasImplemented) {
                    messages.add(new Message(
                            traitImpl.getContent().getTrait().getRange(),
                            Message.MessageSeverity.ERROR,
                            "Trait implementation did not implement function " + funcName
                    ).addInfo(new Message.MessageRelatedInformation(
                            funcDefinition.getLocation(),
                            "Original function was defined here"
                    )));
                }
            });

            // Check that there are no extra functions defined.
            for (Documentable<Func> funcImpl : traitImpl.getContent().getFuncImpls()) {
                if (!trait.trait.getTraitFuncDefinitions().containsKey(funcImpl.getContent().getName().contents)) {
                    messages.add(new Message(
                            funcImpl.getContent().getName().getRange(),
                            Message.MessageSeverity.ERROR,
                            "Unknown trait function " + funcImpl.getContent().getName().contents
                    ).addInfo(new Message.MessageRelatedInformation(
                            trait.trait.getLocation(),
                            "Trait was defined here"
                    )));
                }
            }
        }
    }
}
