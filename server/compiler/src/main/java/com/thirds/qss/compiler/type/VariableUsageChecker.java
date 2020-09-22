package com.thirds.qss.compiler.type;

import com.thirds.qss.QssLogger;
import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.*;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.tree.Script;
import com.thirds.qss.compiler.tree.Type;
import com.thirds.qss.compiler.tree.expr.Expression;
import com.thirds.qss.compiler.tree.expr.Identifier;
import com.thirds.qss.compiler.tree.script.Func;
import com.thirds.qss.compiler.tree.statement.*;

import java.util.*;
import java.util.function.Consumer;

public class VariableUsageChecker {
    private final Compiler compiler;
    private final Script script;
    private final ScriptPath filePath;
    private final ArrayList<Message> messages;

    public VariableUsageChecker(Compiler compiler, Script script, ScriptPath filePath, ArrayList<Message> messages) {
        this.compiler = compiler;
        this.script = script;
        this.filePath = filePath;
        this.messages = messages;
    }

    /**
     * Traverses each statement in the function looking for where and how variables are used, throwing error and warning
     * messages on invalid code.
     *
     * @return The types of each variable used in the block.
     */
    public Map<String, VariableType> deduceVariableUsage(Func func) {
        Map<String, VariableType> variableTypeMap = new HashMap<>();
        ScopeTree scopeTree = new ScopeTree(variableTypeMap);
        boolean trackResult = false;

        Type returnType = func.getReturnType();
        if (returnType != null) {
            VariableType returnType2 = returnType.getResolvedType();
            if (returnType2 != null) {
                scopeTree.put("result", new VariableUsageState(returnType, "result", func.getFuncBlock().getBlock()));
                trackResult = true;
            }
        }

        if (func.getFuncBlock().isNative())
            return variableTypeMap;

        scopeTree = deduceVariableUsage(func.getFuncBlock().getBlock(), scopeTree);
        if (trackResult) {
            VariableUsageState resultState = scopeTree.getState("result");
            if (resultState.isNeverAssigned()) {
                messages.add(new Message(
                        returnType.getRange(),
                        Message.MessageSeverity.ERROR,
                        "A value was not returned at the end of this function"
                ));
            } else if (resultState.isConditionallyAssigned()) {
                StringBuilder sb = new StringBuilder("A value was not returned at the end of this function on all paths");
                assignVariableInBlocks(sb, resultState.nonAssignedBlocks);
                messages.add(new Message(
                        returnType.getRange(),
                        Message.MessageSeverity.ERROR,
                        sb.toString()
                ));
            }
        }

        return variableTypeMap;
    }

    /**
     * Walks through each branch of the function block to work out where variables are used.
     *
     * @return A map that maps variable IDs onto their states. When the variable leaves scope, the entry
     * should be removed from the map.
     */
    private ScopeTree deduceVariableUsage(CompoundStatement block, ScopeTree outerScopes) {
        ScopeTree scopeTree = new ScopeTree(outerScopes);
        if (block == null)
            return scopeTree;

        ArrayList<String> namesDeclaredInThisScope = new ArrayList<>();

        for (Statement statement : block.getStatements()) {
            scopeTree = deduceVariableUsageStatement(statement, block, scopeTree, namesDeclaredInThisScope);
        }

        scopeTree.removeNames(namesDeclaredInThisScope);
        return scopeTree;
    }

    private ScopeTree deduceVariableUsageStatement(Statement statement, CompoundStatement block, ScopeTree scopeTree, ArrayList<String> namesDeclaredInThisScope) {
        if (statement == null)
            return scopeTree;

        if (statement instanceof LetAssignStatement) {
            LetAssignStatement letAssignStatement = (LetAssignStatement) statement;
            if (scopeTree.containsName(letAssignStatement.getName().contents)) {
                messages.add(new Message(
                        letAssignStatement.getName().getRange(),
                        Message.MessageSeverity.ERROR,
                        "Name " + letAssignStatement.getName().contents + " was defined twice in the same block"
                ));
            } else {
                namesDeclaredInThisScope.add(letAssignStatement.getName().contents);
                VariableUsageState state = new VariableUsageState(letAssignStatement.getName(), letAssignStatement.getName().contents, block);
                state = state.assign(letAssignStatement);
                deduceVariableUsageRvalue(letAssignStatement.getRvalue(), scopeTree);
                scopeTree.put(letAssignStatement.getName().contents, state);
            }
        } else if (statement instanceof LetWithTypeStatement) {
            LetWithTypeStatement letWithTypeStatement = (LetWithTypeStatement) statement;
            if (scopeTree.containsName(letWithTypeStatement.getName().contents)) {
                messages.add(new Message(
                        letWithTypeStatement.getName().getRange(),
                        Message.MessageSeverity.ERROR,
                        "Name " + letWithTypeStatement.getName().contents + " was defined twice in the same block"
                ));
            } else {
                namesDeclaredInThisScope.add(letWithTypeStatement.getName().contents);
                VariableUsageState state = new VariableUsageState(letWithTypeStatement.getName(), letWithTypeStatement.getName().contents, block);
                scopeTree.put(letWithTypeStatement.getName().contents, state);
                letWithTypeStatement.getType().resolve(compiler, script);
                scopeTree.setVariableType(letWithTypeStatement.getName().contents, letWithTypeStatement.getType().getResolvedType());
            }
        } else if (statement instanceof EvaluateStatement) {
            EvaluateStatement evaluateStatement = (EvaluateStatement) statement;
            deduceVariableUsageRvalue(evaluateStatement.getExpr(), scopeTree);
        } else if (statement instanceof AssignStatement) {
            AssignStatement assignStatement = (AssignStatement) statement;
            deduceVariableUsageRvalue(assignStatement.getRvalue(), scopeTree);
            deduceVariableUsageLvalue(assignStatement.getLvalue(), scopeTree);
        } else if (statement instanceof CompoundStatement) {
            CompoundStatement compoundStatement = (CompoundStatement) statement;
            scopeTree = deduceVariableUsage(compoundStatement, scopeTree);
        }

        return scopeTree;
    }

    /**
     * Checks the usage of variables when computing this rvalue expr.
     */
    private void deduceVariableUsageRvalue(Expression expr, ScopeTree scopeTree) {
        if (expr instanceof Identifier) {
            Identifier identifier = (Identifier) expr;
            resolveIdentifier(identifier, scopeTree);
            if (identifier.isLocal()) {
                String variableName = identifier.getName().getSegments().get(0).contents;
                VariableUsageState state = scopeTree.getState(variableName);
                if (state != null) {
                    scopeTree.setState(variableName, state.use(expr));
                    return;
                }
            }
        }

        expr.forChildren(child -> {
            if (child instanceof Expression)
                deduceVariableUsageRvalue(((Expression) child), scopeTree);
        });
    }

    /**
     * Checks the usage of variables when computing this lvalue expr.
     */
    private void deduceVariableUsageLvalue(Expression expr, ScopeTree scopeTree) {
        if (expr instanceof Identifier) {
            Identifier identifier = (Identifier) expr;
            resolveIdentifier(identifier, scopeTree);
            if (identifier.isLocal()) {
                String variableName = identifier.getName().getSegments().get(0).contents;
                VariableUsageState state = scopeTree.getState(variableName);
                if (state != null) {
                    scopeTree.setState(variableName, state.assign(expr));
                    return;
                }
            }
        }

        expr.forChildren(child -> {
            if (child instanceof Expression)
                deduceVariableUsageRvalue(((Expression) child), scopeTree);
        });
    }

    private void resolveIdentifier(Identifier identifier, ScopeTree scopeTree) {
        for (String variableName : scopeTree.allVariableNames()) {
            if (identifier.getName().matches(variableName)) {
                VariableUsageState state = scopeTree.getState(variableName);
                identifier.getName().setTarget(new Location(filePath, state.variable.getRange()), null);
                identifier.setLocal(true);
                identifier.setVariableType(scopeTree.getVariableType(variableName));
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

    /**
     * What is the resulting variable scope tree when two or more scopes are executed in
     * parallel, i.e. on different conditional arms? The initial list is consumed.
     *
     * For example:
     * <code><pre>
     *     if a {
     *         // foo
     *     } else {
     *         // bar
     *     }
     * </pre></code>
     *
     * The blocks marked <code>foo</code> and <code>bar</code> are executed "in parallel", like a parallel circuit
     * in electronics; each electron may go through only one of the two branches.
     */
    private ScopeTree parallel(List<ScopeTree> scopes) {
        if (scopes.isEmpty())
            throw new IllegalArgumentException("Scopes cannot be empty");
        if (scopes.size() == 1)
            return scopes.get(0);

        // Check that all scopes have the same variable list.
        ScopeTree baseTree = scopes.get(0);
        Set<String> names = baseTree.allVariableNames();
        for (int i = 1; i < scopes.size(); i++) {
            ScopeTree scope = scopes.get(i);
            if (!scope.allVariableNames().equals(names)) {
                QssLogger.logger.atSevere().log("The variables in two scopes could not be matched (this is a compiler bug): " + baseTree + "; " + scope);
                throw new UnsupportedOperationException(baseTree + " ; " + scope);
                //return null;
            }
        }

        // Now, execute the scope states in parallel.
        for (String name : names) {
            VariableUsageState state = baseTree.getState(name);
            for (int i = 1; i < scopes.size(); i++) {
                ScopeTree scope = scopes.get(i);
                state = state.parallel(scope.getState(name));
            }
            baseTree.setState(name, state);
        }

        return baseTree;
    }

    /**
     * Represents the state of each locally scoped variable in a given scope.
     */
    private static class ScopeTree {
        private final Map<String, VariableUsageState> stateMap = new HashMap<>();
        private final Map<String, VariableType> variableTypeMap;

        public ScopeTree(Map<String, VariableType> variableTypeMap) {
            this.variableTypeMap = variableTypeMap;
        }

        public ScopeTree(ScopeTree outerScopes) {
            variableTypeMap = outerScopes.variableTypeMap;
            for (Map.Entry<String, VariableUsageState> entry : outerScopes.stateMap.entrySet()) {
                stateMap.put(entry.getKey(), entry.getValue().duplicate());
            }
        }

        public void setVariableType(String variable, VariableType type) {
            variableTypeMap.put(variable, type);
        }

        public void put(String name, VariableUsageState state) {
            stateMap.put(name, state);
        }

        public VariableUsageState getState(String variableName) {
            VariableUsageState state = stateMap.get(variableName);
            return state;
        }

        private void removeName(String name) {
            VariableUsageState state = stateMap.remove(name);
            state.warnIfNotUsed();
        }

        public void removeNames(ArrayList<String> names) {
            for (String name : names) {
                removeName(name);
            }
        }

        public void forAllVariableNames(Consumer<String> func) {
            stateMap.forEach((s, state) -> func.accept(s));
        }

        public Set<String> allVariableNames() {
            Set<String> names = new HashSet<>();
            forAllVariableNames(names::add);
            return names;
        }

        public void setState(String name, VariableUsageState state) {
            stateMap.put(name, state);
        }

        @Override
        public String toString() {
            return stateMap.toString();
        }

        public boolean containsName(String variableName) {
            return getState(variableName) != null;
        }

        public VariableType getVariableType(String variableName) {
            return variableTypeMap.get(variableName);
        }
    }

    private void assignVariableInBlocks(StringBuilder sb, ArrayList<Node> nonAssignedBlocks) {
        sb.append(". You must assign the variable in the following blocks: ");
        for (int i = 0; i < nonAssignedBlocks.size(); i++) {
            Node nonAssignedBlock = nonAssignedBlocks.get(i);
            if (i != 0)
                sb.append(", ");
            sb.append("lines ").append(nonAssignedBlock.getRange().start.line).append("-").append(nonAssignedBlock.getRange().end.line);
        }
    }

    /**
     * Represents the state of the usage of a single variable after a particular block of code has been executed.
     * A variable might have been declared without assignment, conditionally assigned, assigned, or used.
     */
    public class VariableUsageState {
        /**
         * The list of blocks in which the variable is assigned. If this is non-empty, the variable has been
         * conditionally assigned.
         */
        private final ArrayList<Node> assignedBlocks = new ArrayList<>();
        /**
         * The list of blocks in which the variable is not assigned.
         */
        private final ArrayList<Node> nonAssignedBlocks = new ArrayList<>();
        /**
         * The node that defines this variable. E.g. the "a" in <code>let a = 1;</code>
         */
        private final Ranged variable;
        /**
         * The name of this variable.
         */
        private final String variableName;
        /**
         * In which block is this variable state valid?
         */
        private Statement block;
        /**
         * This flag is given to variables when they have been used. For example, in the following fragment of QSS:
         * <code><pre>
         * let a = 1;
         * let b = a;
         * </pre></code>
         * the variable <code>a</code> has been declared, assigned and used; the variable <code>b</code> has been declared
         * and assigned but not used.
         */
        private boolean usedAnywhere = false;

        public VariableUsageState(Ranged variable, String variableName, Statement block) {
            this.variable = variable;
            this.variableName = variableName;
            this.block = block;
        }

        /**
         * Either this, {@link #isConditionallyAssigned()} or {@link #isNeverAssigned()} is always true.
         */
        public boolean isUnconditionallyAssigned() {
            return !assignedBlocks.isEmpty() && nonAssignedBlocks.isEmpty();
        }

        /**
         * Either this, {@link #isUnconditionallyAssigned()} or {@link #isNeverAssigned()} is always true.
         */
        public boolean isConditionallyAssigned() {
            return !assignedBlocks.isEmpty() && !nonAssignedBlocks.isEmpty();
        }

        /**
         * Either this, {@link #isUnconditionallyAssigned()} or {@link #isConditionallyAssigned()} is always true.
         */
        public boolean isNeverAssigned() {
            return assignedBlocks.isEmpty();
        }

        public boolean isUsedAnywhere() {
            return usedAnywhere;
        }

        /**
         * What is the resulting variable usage state when these two states happen on different branches of a
         * conditional statement, like the two branches of an <code>if</code> or arms of a <code>match</code>?
         */
        public VariableUsageState parallel(VariableUsageState other) {
            VariableUsageState result = new VariableUsageState(variable, variableName, block);

            if (isUnconditionallyAssigned()) {
                result.assignedBlocks.add(block);
            } else if (isUnconditionallyAssigned()) {
                result.assignedBlocks.addAll(assignedBlocks);
                result.nonAssignedBlocks.addAll(nonAssignedBlocks);
            } else {
                result.nonAssignedBlocks.add(block);
            }

            if (other.isUnconditionallyAssigned()) {
                result.assignedBlocks.add(other.block);
            } else if (other.isUnconditionallyAssigned()) {
                result.assignedBlocks.addAll(other.assignedBlocks);
                result.nonAssignedBlocks.addAll(other.nonAssignedBlocks);
            } else {
                result.nonAssignedBlocks.add(other.block);
            }

            result.usedAnywhere = usedAnywhere || other.usedAnywhere;

            return result;
        }

        /**
         * Replaces the current 'block' to the new container. Used to specify that a variable usage state is valid
         * at the end of the execution of the container.
         */
        public void setBlock(Statement container) {
            block = container;
        }

        /**
         * What happens when the variable is assigned in a given location? This may throw errors using the
         * provided <code>ctx</code>.
         */
        public VariableUsageState assign(Node where) {
            VariableUsageState state = duplicate();
            state.assignedBlocks.add(where);
            return state;
        }

        /**
         * Call this method whenever the variable is used.
         */
        public VariableUsageState use(Node where) {
            VariableUsageState state = duplicate();

            if (isConditionallyAssigned()) {
                StringBuilder sb = new StringBuilder().append("Variable ").append(variableName);
                if (nonAssignedBlocks.isEmpty()) {
                    sb.append(" was not assigned on all paths before use");
                } else {
                    sb.append(" was not assigned before use");
                    assignVariableInBlocks(sb, nonAssignedBlocks);
                }
                messages.add(new Message(
                        where.getRange(),
                        Message.MessageSeverity.ERROR,
                        sb.toString()
                ));
            } else if (isNeverAssigned()) {
                StringBuilder sb = new StringBuilder().append("Variable ").append(variableName).append(" was not assigned before use");
                if (!nonAssignedBlocks.isEmpty()) {
                    assignVariableInBlocks(sb, nonAssignedBlocks);
                }
                messages.add(new Message(
                        where.getRange(),
                        Message.MessageSeverity.ERROR,
                        sb.toString()
                ));
            }

            state.usedAnywhere = true;
            return state;
        }

        public void warnIfNotUsed() {
            if (!usedAnywhere) {
                if (isNeverAssigned()) {
                    messages.add(new Message(
                            variable.getRange(),
                            Message.MessageSeverity.WARNING,
                            "Variable " + variableName + " was never assigned"
                    ));
                } else {
                    messages.add(new Message(
                            variable.getRange(),
                            Message.MessageSeverity.WARNING,
                            "Variable " + variableName + " was never used"
                    ));
                }
            }
        }

        public VariableUsageState duplicate() {
            VariableUsageState result = new VariableUsageState(variable, variableName, block);
            result.usedAnywhere = usedAnywhere;
            result.assignedBlocks.addAll(assignedBlocks);
            result.nonAssignedBlocks.addAll(nonAssignedBlocks);
            return result;
        }

        @Override
        public String toString() {
            return "VariableUsageState{" +
                    "assignedBlocks=" + assignedBlocks +
                    ", nonAssignedBlocks=" + nonAssignedBlocks +
                    ", variable=" + variable +
                    ", block=" + block +
                    ", usedAnywhere=" + usedAnywhere +
                    '}';
        }
    }
}
