package com.thirds.qss.compiler.tree.statement;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.tree.Node;

import java.util.ArrayList;
import java.util.function.Consumer;

public class CompoundStatement extends Statement {
    private final ArrayList<Statement> statements;

    public CompoundStatement(Range range, ArrayList<Statement> statements) {
        super(range);
        this.statements = statements;
    }

    public ArrayList<Statement> getStatements() {
        return statements;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        for (Statement statement : statements) {
            consumer.accept(statement);
        }
    }
}
