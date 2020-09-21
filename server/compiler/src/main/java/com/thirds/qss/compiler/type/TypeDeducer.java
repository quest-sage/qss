package com.thirds.qss.compiler.type;

import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.tree.script.Func;

import java.util.ArrayList;

/**
 * Deduces types of each expression in a function body.
 */
public class TypeDeducer {
    private final Compiler compiler;

    public TypeDeducer(Compiler compiler) {
        this.compiler = compiler;
    }

    public void computeTypesIn(Func func, ArrayList<Message> messages) {
        if (func.getFuncBlock().isNative())
            return;

        VariableUsageChecker variableUsageChecker = new VariableUsageChecker(compiler, messages);
        variableUsageChecker.deduceVariableUsage(func);
    }
}
