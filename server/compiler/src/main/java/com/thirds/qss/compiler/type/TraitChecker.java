package com.thirds.qss.compiler.type;

import com.thirds.qss.QualifiedName;
import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.resolve.ResolveResult;
import com.thirds.qss.compiler.resolve.Resolver;
import com.thirds.qss.compiler.tree.Script;

import java.util.ArrayList;

/**
 * Checks whether a given type implements a trait. The check is performed at compile time; it is perfectly feasible
 * that another unknown resource bundle makes an implementation that we can't see. This means that certain trait checks
 * must be performed at runtime.
 */
public class TraitChecker {
    private final Compiler compiler;
    private final Script script;
    private final ArrayList<Message> messages;

    public TraitChecker(Compiler compiler, Script script, ArrayList<Message> messages) {
        this.compiler = compiler;
        this.script = script;
        this.messages = messages;
    }

    /**
     * Automatically outputs an error message if this returns false.
     * @param where Where should error messages be emitted from?
     */
    public boolean doesImplement(Range where, VariableType type, QualifiedName trait) {
        ResolveResult<Resolver.TraitImplAlternative> implResolved = Resolver.resolveTraitImpl(compiler, script, messages, where, type, trait);
        return implResolved.alternatives.size() == 1;
    }
}
