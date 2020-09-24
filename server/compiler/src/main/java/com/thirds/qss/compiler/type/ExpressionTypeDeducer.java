package com.thirds.qss.compiler.type;

import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.Location;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.ScriptPath;
import com.thirds.qss.compiler.resolve.Resolver;
import com.thirds.qss.compiler.tree.Script;
import com.thirds.qss.compiler.tree.expr.Identifier;

import java.util.ArrayList;

/**
 * Calculates the type of an expression based on the types of its arguments.
 */
public class ExpressionTypeDeducer {
    private final Compiler compiler;
    private final Script script;
    private final ScriptPath filePath;
    private final ArrayList<Message> messages;
    private final CastChecker castChecker;

    public ExpressionTypeDeducer(Compiler compiler, Script script, ScriptPath filePath, ArrayList<Message> messages) {
        this.compiler = compiler;
        this.script = script;
        this.filePath = filePath;
        this.messages = messages;
        castChecker = new CastChecker();
    }

    public Compiler getCompiler() {
        return compiler;
    }

    public Script getScript() {
        return script;
    }

    public ScriptPath getFilePath() {
        return filePath;
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public CastChecker getCastChecker() {
        return castChecker;
    }

    public void resolveIdentifier(VariableTracker.ScopeTree scopeTree, Identifier identifier) {
        for (String variableName : scopeTree.allVariableNames()) {
            if (identifier.getName().matches(variableName)) {
                VariableTracker.VariableUsageState state = scopeTree.getState(variableName);
                identifier.getName().setTarget(new Location(filePath, state.variable.getRange()), null);
                identifier.setLocal(true);
                scopeTree.getVariableType(variableName).ifPresent(identifier::setVariableType);
                return;
            }
        }

        Resolver.resolveFunc(compiler, script, messages, identifier, "name");

        // TODO tell user if they spelt something wrong (low Levenshtein distance to another name in scope)
    }
}
