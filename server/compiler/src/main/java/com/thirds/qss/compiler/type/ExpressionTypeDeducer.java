package com.thirds.qss.compiler.type;

import com.thirds.qss.QssLogger;
import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Location;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.ScriptPath;
import com.thirds.qss.compiler.tree.expr.Expression;
import com.thirds.qss.compiler.tree.expr.Identifier;
import com.thirds.qss.compiler.tree.expr.IntegerLiteral;
import com.thirds.qss.compiler.tree.expr.StringLiteral;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Calculates the type of an expression based on the types of its arguments.
 */
public class ExpressionTypeDeducer {
    private final ScriptPath filePath;
    private final ArrayList<Message> messages;

    public ExpressionTypeDeducer(ScriptPath filePath, ArrayList<Message> messages) {
        this.filePath = filePath;
        this.messages = messages;
    }

    /**
     * @return Optional.empty() if no type could be deduced for this expression. If this returns empty, an error
     * message MUST be output in the messages list. This is because down the line, empty values might be ignored so
     * we need to raise an error as soon as we produce an empty value.
     */
    public Optional<VariableType> deduceType(VariableTracker.ScopeTree scopeTree, Expression expr) {
        if (expr instanceof Identifier) {
            resolveIdentifier(scopeTree, ((Identifier) expr));
            return expr.getVariableType();
        } else if (expr instanceof IntegerLiteral) {
            return Optional.of(VariableType.Primitive.TYPE_INT);
        } else if (expr instanceof StringLiteral) {
            return Optional.of(VariableType.Primitive.TYPE_STRING);
        }

        QssLogger.logger.atSevere().log("Unknown expression type %s", expr.getClass());
        messages.add(new Message(
                expr.getRange(),
                Message.MessageSeverity.ERROR,
                "Unknown expression type " + expr.getClass().getSimpleName() + " (this is a compiler bug)"
        ));
        return Optional.empty();
    }

    private void resolveIdentifier(VariableTracker.ScopeTree scopeTree, Identifier identifier) {
        for (String variableName : scopeTree.allVariableNames()) {
            if (identifier.getName().matches(variableName)) {
                VariableTracker.VariableUsageState state = scopeTree.getState(variableName);
                identifier.getName().setTarget(new Location(filePath, state.variable.getRange()), null);
                identifier.setLocal(true);
                scopeTree.getVariableType(variableName).ifPresent(identifier::setVariableType);
                return;
            }
        }

        // TODO compiler.getIndices() resolve func names

        // TODO tell user if they spelt something wrong (low Levenshtein distance to another name in scope)

        messages.add(new Message(
                identifier.getRange(),
                Message.MessageSeverity.ERROR,
                "Could not resolve name " + identifier.getName().toQualifiedName()
        ));
    }
}
