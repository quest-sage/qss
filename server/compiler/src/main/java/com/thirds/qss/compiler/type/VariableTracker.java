package com.thirds.qss.compiler.type;

import com.thirds.qss.QssLogger;
import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.*;
import com.thirds.qss.compiler.lexer.TokenType;
import com.thirds.qss.compiler.resolve.Resolver;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.tree.Script;
import com.thirds.qss.compiler.tree.Type;
import com.thirds.qss.compiler.tree.expr.Expression;
import com.thirds.qss.compiler.tree.expr.Identifier;
import com.thirds.qss.compiler.tree.expr.ResultExpression;
import com.thirds.qss.compiler.tree.script.Func;
import com.thirds.qss.compiler.tree.script.FuncHook;
import com.thirds.qss.compiler.tree.script.FuncOrHook;
import com.thirds.qss.compiler.tree.script.Param;
import com.thirds.qss.compiler.tree.statement.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * Tracks variables over the course of a function block. This class tracks:
 * <ul>
 *     <li>variable type</li>
 *     <li>where the variable was assigned</li>
 *     <li>where the variable was used</li>
 * </ul>
 */
public class VariableTracker {
    private final Compiler compiler;
    private final Script script;
    private final ScriptPath filePath;
    private final ArrayList<Message> messages;
    private final FuncOrHook func;
    private final ExpressionTypeDeducer expressionTypeDeducer;

    public VariableTracker(Compiler compiler,
                           Script script,
                           ScriptPath filePath,
                           ArrayList<Message> messages,
                           FuncOrHook func) {
        this.compiler = compiler;
        this.script = script;
        this.filePath = filePath;
        this.messages = messages;
        this.func = func;
        expressionTypeDeducer = new ExpressionTypeDeducer(compiler, script, filePath, messages);
        track();
    }

    /**
     * Traverses each statement in the function looking for where and how variables are used, throwing error and warning
     * messages on invalid code.
     *
     */
    private void track() {
        ScopeTree scopeTree = new ScopeTree(new FunctionState());

        // Add the function parameters to the scope tree.
        for (Param param : func.getParamList().getParams()) {
            VariableUsageState duplicateState = scopeTree.getState(param.getName().contents);
            if (duplicateState == null) {
                VariableUsageState state = new VariableUsageState(param, param.getName().contents, func.getFuncBlock().getBlock())
                        .assign(func.getFuncBlock().getBlock());
                state.variableType = param.getType().getResolvedType();
                scopeTree.put(param.getName().contents, state);
            } else {
                messages.add(new Message(
                        param.getName().getRange(),
                        Message.MessageSeverity.ERROR,
                        "Parameter " + param.getName().contents + " was already defined"
                ).addInfo(new Message.MessageRelatedInformation(
                        new Location(filePath, duplicateState.variable.getRange()),
                        "Previously defined here"
                )));
            }
        }

        boolean trackResult = false;
        Type returnType = func.getReturnType();
        if (returnType != null) {
            VariableUsageState state = new VariableUsageState(returnType, "result", func.getFuncBlock().getBlock());
            VariableType returnType2 = returnType.getResolvedType();
            if (returnType2 != null) {
                state.variableType = returnType2;
                if (func instanceof Func)
                    trackResult = true;
            }
            scopeTree.put("result", state);
        }

        if (func.getFuncBlock().isNative())
            return;

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
                //assignVariableInBlocks(sb, resultState.nonAssignedBlocks);
                messages.add(new Message(
                        returnType.getRange(),
                        Message.MessageSeverity.ERROR,
                        sb.toString()
                ));
            }
        }

    }

    /**
     * What is the state of the function at this point in its execution?
     * E.g. have we already returned a value? Are we in a loop or not?
     */
    private static class FunctionState {
        boolean returnedValue = false;
        boolean inLoop = false;

        public FunctionState copy() {
            FunctionState result = new FunctionState();
            result.returnedValue = returnedValue;
            result.inLoop = inLoop;
            return result;
        }
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

        if (scopeTree.functionState.returnedValue) {
            messages.add(new Message(
                    statement.getRange(),
                    Message.MessageSeverity.WARNING,
                    "This statement is unreachable, the function already returned"
            ));
        }

        if (statement instanceof LetAssignStatement) {
            LetAssignStatement letAssignStatement = (LetAssignStatement) statement;
            if (scopeTree.containsName(letAssignStatement.getName().contents)) {
                messages.add(new Message(
                        letAssignStatement.getName().getRange(),
                        Message.MessageSeverity.ERROR,
                        "Name " + letAssignStatement.getName().contents + " was declared twice in the same block"
                ).addInfo(new Message.MessageRelatedInformation(
                        new Location(filePath, scopeTree.getState(letAssignStatement.getName().contents).variable.getRange()),
                        "Previously declared here"
                )));
            } else {
                namesDeclaredInThisScope.add(letAssignStatement.getName().contents);

                ScopeTree finalScopeTree = scopeTree;
                Optional<VariableType> rvalueType = deduceVariableUsageRvalue(letAssignStatement.getRvalue(), scopeTree);

                VariableUsageState state = new VariableUsageState(letAssignStatement.getName(), letAssignStatement.getName().contents, block);
                state = state.assign(letAssignStatement);
                scopeTree.put(letAssignStatement.getName().contents, state);

                rvalueType.ifPresent(type -> finalScopeTree.setVariableType(letAssignStatement.getName().contents, type));
            }
        } else if (statement instanceof LetWithTypeStatement) {
            LetWithTypeStatement letWithTypeStatement = (LetWithTypeStatement) statement;
            if (scopeTree.containsName(letWithTypeStatement.getName().contents)) {
                messages.add(new Message(
                        letWithTypeStatement.getName().getRange(),
                        Message.MessageSeverity.ERROR,
                        "Name " + letWithTypeStatement.getName().contents + " was declared twice in the same block"
                ).addInfo(new Message.MessageRelatedInformation(
                        new Location(filePath, scopeTree.getState(letWithTypeStatement.getName().contents).variable.getRange()),
                        "Previously declared here"
                )));
            } else {
                namesDeclaredInThisScope.add(letWithTypeStatement.getName().contents);
                VariableUsageState state = new VariableUsageState(letWithTypeStatement.getName(), letWithTypeStatement.getName().contents, block);
                scopeTree.put(letWithTypeStatement.getName().contents, state);
                Resolver.resolveType(compiler, script, messages, letWithTypeStatement.getName().contents, letWithTypeStatement.getType());
                scopeTree.setVariableType(letWithTypeStatement.getName().contents, letWithTypeStatement.getType().getResolvedType());
            }
        } else if (statement instanceof EvaluateStatement) {
            EvaluateStatement evaluateStatement = (EvaluateStatement) statement;
            deduceVariableUsageRvalue(evaluateStatement.getExpr(), scopeTree);
        } else if (statement instanceof AssignStatement) {
            AssignStatement assignStatement = (AssignStatement) statement;
            Optional<VariableType> optionalRvalue = deduceVariableUsageRvalue(assignStatement.getRvalue(), scopeTree);
            Optional<VariableType> optionalLvalue = deduceVariableUsageLvalue(assignStatement.getLvalue(), scopeTree);
            optionalLvalue.ifPresent(lvalue -> optionalRvalue.ifPresent(rvalue ->
                messages.addAll(expressionTypeDeducer.getCastChecker().attemptDowncast(
                        ((AssignStatement) statement).getRvalue().getRange(),
                        rvalue, lvalue
                ).getMessages())
            ));
        } else if (statement instanceof CompoundStatement) {
            CompoundStatement compoundStatement = (CompoundStatement) statement;
            scopeTree = deduceVariableUsage(compoundStatement, scopeTree);
        } else if (statement instanceof ReturnStatement) {
            ReturnStatement returnStatement = (ReturnStatement) statement;
            scopeTree.functionState.returnedValue = true;
            if (func instanceof FuncHook && ((FuncHook) func).getTime().type == TokenType.KW_BEFORE) {
                // We're not allowed to use "return" statements in "before" hooks.
                messages.add(new Message(
                        returnStatement.getRange(),
                        Message.MessageSeverity.ERROR,
                        "'return' statements are forbidden in 'before' hooks"
                ));
            } else {
                if (func.getReturnType() == null) {
                    // The function is not supposed to return a value.
                    if (returnStatement.didReturnValue()) {
                        messages.add(new Message(
                                returnStatement.getRange(),
                                Message.MessageSeverity.ERROR,
                                "This function is not supposed to return any value"
                        ));
                    }
                } else {
                    // The function is supposed to return a value.
                    if (!returnStatement.didReturnValue()) {
                        if (func instanceof FuncHook && ((FuncHook) func).getTime().type == TokenType.KW_AFTER) {
                            // But if we're in an "after" hook, we don't actually have to return anything - the return value
                            // has already been computed.
                        } else {
                            messages.add(new Message(
                                    returnStatement.getRange(),
                                    Message.MessageSeverity.ERROR,
                                    "This function is supposed to return an expression of type " +
                                            scopeTree.getVariableType("result").map(Object::toString).orElse("<not evaluated>") +
                                            ", but no return value was supplied"
                            ));
                        }
                    }
                }
            }
        } else if (statement instanceof IfStatement) {
            IfStatement ifStatement = (IfStatement) statement;
            Optional<VariableType> conditionType = deduceVariableUsageRvalue(ifStatement.getCondition(), scopeTree);
            conditionType.ifPresent(variableType -> messages.addAll(expressionTypeDeducer.getCastChecker().attemptDowncast(
                    ifStatement.getCondition().getRange(),
                    variableType,
                    VariableType.Primitive.TYPE_BOOL
            ).getMessages()));
            Statement trueBlock = ifStatement.getTrueBlock();
            Statement falseBlock = ifStatement.getFalseBlock();
            if (falseBlock == null) {
                // We don't know if any code will execute, so the scope tree may stay the same as it was before the block.
                scopeTree = parallel(List.of(
                        deduceVariableUsageStatement(trueBlock, block, scopeTree, namesDeclaredInThisScope),
                        scopeTree
                ));
            } else {
                scopeTree = parallel(List.of(
                        deduceVariableUsageStatement(trueBlock, block, scopeTree, namesDeclaredInThisScope),
                        deduceVariableUsageStatement(falseBlock, block, scopeTree, namesDeclaredInThisScope)
                ));
            }
        } else if (statement instanceof WhileStatement) {
            WhileStatement whileStatement = (WhileStatement) statement;
            Optional<VariableType> conditionType = deduceVariableUsageRvalue(whileStatement.getCondition(), scopeTree);
            conditionType.ifPresent(variableType -> messages.addAll(expressionTypeDeducer.getCastChecker().attemptDowncast(
                    whileStatement.getCondition().getRange(),
                    variableType,
                    VariableType.Primitive.TYPE_BOOL
            ).getMessages()));

            boolean alreadyInLoop = scopeTree.functionState.inLoop;
            scopeTree.functionState.inLoop = true;
            // We don't know if any code will execute, so the scope tree may stay the same as it was before the block.
            scopeTree = parallel(List.of(
                    deduceVariableUsageStatement(whileStatement.getBlock(), block, scopeTree, namesDeclaredInThisScope),
                    scopeTree
            ));
            scopeTree.functionState.inLoop = alreadyInLoop;
        } else if (statement instanceof BreakStatement) {
            // Check if the "break" statement was actually part of a loop.
            // If it wasn't inside a loop, the statement is invalid.
            if (!scopeTree.functionState.inLoop) {
                messages.add(new Message(
                        statement.getRange(),
                        Message.MessageSeverity.ERROR,
                        "'break' statement was not in a loop"
                ));
            }
        } else if (statement instanceof ContinueStatement) {
            // Check if the "continue" statement was actually part of a loop.
            // If it wasn't inside a loop, the statement is invalid.
            if (!scopeTree.functionState.inLoop) {
                messages.add(new Message(
                        statement.getRange(),
                        Message.MessageSeverity.ERROR,
                        "'continue' statement was not in a loop"
                ));
            }
        }

        return scopeTree;
    }

    /**
     * Checks the usage of variables when computing this rvalue expr.
     * @return The type of the expression, or empty if no type could be deduced.
     */
    private Optional<VariableType> deduceVariableUsageRvalue(Expression expr, ScopeTree scopeTree) {
        expr.deduceAndAssignVariableType(expressionTypeDeducer, scopeTree);
        Optional<VariableType> type = expr.getVariableType();
        if (expr instanceof Identifier) {
            Identifier identifier = (Identifier) expr;
            if (identifier.isLocal()) {
                String variableName = identifier.getName().getSegments().get(0).contents;
                VariableUsageState state = scopeTree.getState(variableName);
                if (state != null) {
                    scopeTree.setState(variableName, state.use(expr));
                }
            }
        } else if (expr instanceof ResultExpression) {
            if (func instanceof Func) {
                messages.add(new Message(
                        expr.getRange(),
                        Message.MessageSeverity.ERROR,
                        "The 'result' variable is not available in functions"
                ));
            } else if (func instanceof FuncHook) {
                if (((FuncHook) func).getTime().type == TokenType.KW_BEFORE) {
                    messages.add(new Message(
                            expr.getRange(),
                            Message.MessageSeverity.ERROR,
                            "The 'result' variable is not available in 'before' hooks"
                    ));
                } else {
                    // Time is 'after'.
                    if (func.getReturnType() == null || func.getReturnType().getResolvedType() == VariableType.Primitive.TYPE_VOID) {
                        messages.add(new Message(
                                expr.getRange(),
                                Message.MessageSeverity.ERROR,
                                "This function doesn't return a value, so the 'result' variable is not available"
                        ));
                    }
                }
            }
        } else {
            expr.forAllChildren(n -> {
                if (n instanceof Expression)
                    deduceVariableUsageRvalue((Expression) n, scopeTree);
            });
        }

        return type;
    }

    /**
     * Checks the usage of variables when computing this lvalue expr.
     * @return The type of the expression, or empty if no type could be deduced.
     */
    private Optional<VariableType> deduceVariableUsageLvalue(Expression expr, ScopeTree scopeTree) {
        expr.deduceAndAssignVariableType(expressionTypeDeducer, scopeTree);
        Optional<VariableType> type = expr.getVariableType();
        if (expr instanceof Identifier) {
            Identifier identifier = (Identifier) expr;
            if (identifier.isLocal()) {
                String variableName = identifier.getName().getSegments().get(0).contents;
                VariableUsageState state = scopeTree.getState(variableName);
                if (state != null) {
                    scopeTree.setState(variableName, state.assign(expr));
                }
            }
        } else if (expr instanceof ResultExpression) {
            VariableUsageState resultState = scopeTree.getState("result");
            if (func.getReturnType() != null) {
                // The function returns something.
                scopeTree.setState("result", resultState.assign(expr));
            }
        } else {
            expr.forAllChildren(n -> {
                if (n instanceof Expression)
                    deduceVariableUsageRvalue((Expression) n, scopeTree);
            });
        }

        return type;
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
        FunctionState functionState = baseTree.functionState.copy();
        for (int i = 1; i < scopes.size(); i++) {
            ScopeTree scope = scopes.get(i);
            if (!scope.functionState.returnedValue)
                functionState.returnedValue = false;
        }
        baseTree.functionState = functionState;

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
     * It also encapsulates the state of the function's execution at this point in the function.
     */
    public static class ScopeTree {
        private final Map<String, VariableUsageState> stateMap = new HashMap<>();
        private FunctionState functionState;

        public ScopeTree(FunctionState functionState) {
            this.functionState = functionState;
        }

        public ScopeTree(ScopeTree outerScopes) {
            for (Map.Entry<String, VariableUsageState> entry : outerScopes.stateMap.entrySet()) {
                stateMap.put(entry.getKey(), entry.getValue().duplicate());
            }
            functionState = outerScopes.functionState.copy();
        }

        public void setVariableType(String variable, VariableType type) {
            QssLogger.logger.atInfo().log("Set VT for %s: %s", variable, type);
            if (getState(variable) == null)
                QssLogger.logger.atSevere().log("No state for %s: %s", variable, stateMap);
            getState(variable).variableType = type;
        }

        public void put(String name, VariableUsageState state) {
            stateMap.put(name, state);
        }

        public VariableUsageState getState(String variableName) {
            return stateMap.get(variableName);
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

        public Optional<VariableType> getVariableType(String variableName) {
            VariableUsageState state = getState(variableName);
            if (state == null)
                return Optional.of(VariableType.Primitive.TYPE_UNKNOWN);
            return Optional.ofNullable(state.variableType);
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
        public final ArrayList<Node> assignedBlocks = new ArrayList<>();
        /**
         * The list of blocks in which the variable is not assigned.
         */
        public final ArrayList<Node> nonAssignedBlocks = new ArrayList<>();
        /**
         * The node that defines this variable. E.g. the "a" in <code>let a = 1;</code>
         */
        public final Ranged variable;
        /**
         * The name of this variable.
         */
        public final String variableName;
        /**
         * In which block is this variable state valid?
         */
        public Statement block;
        /**
         * This flag is given to variables when they have been used. For example, in the following fragment of QSS:
         * <code><pre>
         * let a = 1;
         * let b = a;
         * </pre></code>
         * the variable <code>a</code> has been declared, assigned and used; the variable <code>b</code> has been declared
         * and assigned but not used.
         */
        public boolean usedAnywhere = false;
        /**
         * Null if no type has been deduced yet.
         */
        public VariableType variableType;

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
            result.variableType = variableType;

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
            state.assignedBlocks.clear();
            state.nonAssignedBlocks.clear();
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
                    //assignVariableInBlocks(sb, nonAssignedBlocks);
                }
                messages.add(new Message(
                        where.getRange(),
                        Message.MessageSeverity.ERROR,
                        sb.toString()
                ));
            } else if (isNeverAssigned()) {
                StringBuilder sb = new StringBuilder().append("Variable ").append(variableName).append(" was not assigned before use");
                if (!nonAssignedBlocks.isEmpty()) {
                    //assignVariableInBlocks(sb, nonAssignedBlocks);
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
            result.variableType = variableType;
            return result;
        }

        @Override
        public String toString() {
            return "VariableUsageState{" +
                    "assignedBlocks=" + assignedBlocks +
                    ", nonAssignedBlocks=" + nonAssignedBlocks +
                    ", variable=" + variable +
                    ", variableName='" + variableName + '\'' +
                    ", block=" + block +
                    ", usedAnywhere=" + usedAnywhere +
                    ", variableType=" + variableType +
                    '}';
        }
    }
}
