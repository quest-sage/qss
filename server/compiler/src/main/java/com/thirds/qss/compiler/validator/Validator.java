package com.thirds.qss.compiler.validator;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.ScriptPath;
import com.thirds.qss.compiler.resolve.ResolveResult;
import com.thirds.qss.compiler.resolve.Resolver;
import com.thirds.qss.compiler.tree.Documentable;
import com.thirds.qss.compiler.tree.Script;
import com.thirds.qss.compiler.tree.script.FuncHook;

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

        return messages;
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
                ArrayList<VariableType> params = funcHook.getContent().getParamList().getParams()
                        .stream().map(param -> param.getType().getResolvedType())
                        .collect(Collectors.toCollection(ArrayList::new));
                VariableType.Function actualType = new VariableType.Function(
                        false,
                        params,
                        null
                );

                VariableType expectedType = result.alternatives.get(0).value.func.getType();
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
}
