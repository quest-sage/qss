package com.thirds.qss.compiler.type;

import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.ScriptPath;
import com.thirds.qss.compiler.tree.Script;
import com.thirds.qss.compiler.tree.script.Func;
import com.thirds.qss.compiler.tree.script.FuncHook;
import com.thirds.qss.compiler.tree.script.FuncOrHook;

import java.util.ArrayList;

/**
 * Deduces types of each expression in a function body.
 */
public class TypeDeducer {
    private final Compiler compiler;
    private final Script script;
    private final ScriptPath filePath;

    public TypeDeducer(Compiler compiler, Script script, ScriptPath filePath) {
        this.compiler = compiler;
        this.script = script;
        this.filePath = filePath;
    }

    public void computeTypesIn(FuncOrHook func, ArrayList<Message> messages) {
        if (func.getFuncBlock().isNative())
            return;

        new VariableTracker(compiler, script, filePath, messages, func);
    }
}
