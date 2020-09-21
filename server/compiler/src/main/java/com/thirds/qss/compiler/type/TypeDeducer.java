package com.thirds.qss.compiler.type;

import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.ScriptPath;
import com.thirds.qss.compiler.tree.script.Func;

import java.util.ArrayList;

/**
 * Deduces types of each expression in a function body.
 */
public class TypeDeducer {
    private final Compiler compiler;
    private final ScriptPath filePath;

    public TypeDeducer(Compiler compiler, ScriptPath filePath) {
        this.compiler = compiler;
        this.filePath = filePath;
    }

    public void computeTypesIn(Func func, ArrayList<Message> messages) {
        if (func.getFuncBlock().isNative())
            return;

        VariableUsageChecker variableUsageChecker = new VariableUsageChecker(compiler, filePath, messages);
        variableUsageChecker.deduceVariableUsage(func);
    }
}
