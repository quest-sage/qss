package com.thirds.qss.compiler.parser;

import com.thirds.qss.QualifiedName;
import com.thirds.qss.compiler.*;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.lexer.TokenStream;
import com.thirds.qss.compiler.lexer.TokenType;
import com.thirds.qss.compiler.tree.*;
import com.thirds.qss.compiler.tree.expr.*;
import com.thirds.qss.compiler.tree.script.*;
import com.thirds.qss.compiler.tree.statement.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Represents an LL(1) recursive descent parser for QSS.
 *
 * The {@link TokenStream#rewind()} method allows for acting as if it were an LL(2) parser. This is useful for
 * pre-documentation of items and fields etc.
 */
public class Parser {
    private final ScriptPath filePath;

    public Parser(ScriptPath filePath) {
        this.filePath = filePath;
    }

    public Messenger<Script> parse(TokenStream tokens) {
        Messenger<Script> script = parseScript(tokens);

        tokens.peek().ifPresent(token -> script.getMessages().add(new Message(
                token.getRange(),
                Message.MessageSeverity.ERROR,
                "Unexpected extra data at end of file, got " + token.type
        )));

        return script;
    }

    //#region Script-wide items

    @SuppressWarnings("unchecked")
    private Messenger<Script> parseScript(TokenStream tokens) {
        Position start = tokens.currentPosition();

        ListMessenger<Import> imports = parseGreedy(() -> parseImport(tokens));

        ListMessenger<Documentable<?>> items = parseGreedy(() -> parseItem(tokens));

        return imports.map(imports2 -> items.map(items2 -> {
            ArrayList<Documentable<Struct>> structs = new ArrayList<>();
            ArrayList<Documentable<Func>> funcs = new ArrayList<>();
            ArrayList<Documentable<FuncHook>> funcHooks = new ArrayList<>();

            for (Documentable<?> documentable : items2) {
                Node content = documentable.getContent();
                if (content instanceof Struct)
                    structs.add((Documentable<Struct>) documentable);
                else if (content instanceof Func)
                    funcs.add((Documentable<Func>) documentable);
                else if (content instanceof FuncHook)
                    funcHooks.add((Documentable<FuncHook>) documentable);
            }

            // Deduce what package the script is in by reversing directories until we hit the bundle.toml file.
            QualifiedName packageName = new QualifiedName();
            ScriptPath packagePath = filePath.trimLastSegment();
            boolean packageNameResolved = false;
            while (!packagePath.getSegments().isEmpty()) {
                String lastSegment = packagePath.lastSegment();
                packagePath = packagePath.trimLastSegment();

                if (packagePath.toPath().resolve("bundle.toml").toFile().isFile()) {
                    packageNameResolved = true;
                    break;
                }

                packageName = packageName.prependSegment(lastSegment);
            }

            ArrayList<Message> messages = new ArrayList<>(0);
            if (!packageNameResolved) {
                messages.add(new Message(
                        new Range(new Position(0, 0)),
                        Message.MessageSeverity.ERROR,
                        "Script was not in a bundle"
                ));
            }

            return Messenger.success(new Script(
                    filePath, new Range(start, tokens.currentEndPosition()),
                    packageName,
                    packagePath,
                    imports2,
                    structs, funcs, funcHooks
            ), messages);
        }));
    }

    private Optional<Messenger<Import>> parseImport(TokenStream tokens) {
        if (tokens.peek().isPresent() && tokens.peek().get().type == TokenType.KW_IMPORT) {
            tokens.next();  // consume the KW_IMPORT token
            return Optional.of(parseName(tokens).map(name -> consumeToken(tokens, TokenType.SEMICOLON).then(() -> Messenger.success(new Import(name)))));
        }
        return Optional.empty();
    }

    private Optional<Messenger<Documentable<?>>> parseItem(TokenStream tokens) {
        Token docs;
        if (tokens.peek().isPresent() && tokens.peek().get().type == TokenType.DOCUMENTATION_COMMENT) {
            docs = tokens.next();
        } else {
            docs = null;
        }

        Optional<Messenger<Struct>> struct = parseStruct(tokens);
        if (struct.isPresent()) {
            return Optional.of(struct.get().map(s -> consumeToken(tokens, TokenType.SEMICOLON).then(() -> Messenger.success(new Documentable<>(docs, s)))));
        }

        Optional<Messenger<Func>> func = parseFunc(tokens);
        if (func.isPresent()) {
            return Optional.of(func.get().map(s -> consumeToken(tokens, TokenType.SEMICOLON).then(() -> Messenger.success(new Documentable<>(docs, s)))));
        }

        Optional<Messenger<FuncHook>> funcHook = parseHook(tokens);
        if (funcHook.isPresent()) {
            return Optional.of(funcHook.get().map(s -> consumeToken(tokens, TokenType.SEMICOLON).then(() -> Messenger.success(new Documentable<>(docs, s)))));
        }

        if (docs != null)
            tokens.rewind();
        return Optional.empty();
    }

    /**
     * <code>Struct := "struct" Identifier "{" Field* "}"</code>
     * @return Null if the token stream did not represent a struct.
     */
    @SuppressWarnings("unchecked")
    private Optional<Messenger<Struct>> parseStruct(TokenStream tokens) {
        if (tokens.peek().isEmpty() || tokens.peek().get().type != TokenType.KW_STRUCT)
            return Optional.empty();

        Position start = tokens.currentPosition();

        return Optional.of(parseMulti(List.of(
                () -> consumeToken(tokens, TokenType.KW_STRUCT),    // 0
                () -> consumeToken(tokens, TokenType.IDENTIFIER),   // 1
                () -> consumeToken(tokens, TokenType.LBRACE),       // 2
                () -> parseFields(tokens),                          // 3
                () -> consumeToken(tokens, TokenType.RBRACE)        // 4
        )).map(list -> {
            Token identifier = (Token) list.get(1);
            ArrayList<Documentable<Field>> fields = (ArrayList<Documentable<Field>>) list.get(3);

            Struct struct = new Struct(
                    new Range(start, tokens.currentEndPosition()),
                    identifier, fields
            );

            return Messenger.success(struct);
        }));
    }

    private ListMessenger<Documentable<Field>> parseFields(TokenStream tokens) {
        return parseGreedy(() -> parseField(tokens));
    }

    /**
     * <code>Field := Documentation? Identifier ":" Type</code>
     */
    private Optional<Messenger<Documentable<Field>>> parseField(TokenStream tokens) {
        Token docs;
        if (tokens.peek().isPresent() && tokens.peek().get().type == TokenType.DOCUMENTATION_COMMENT) {
            docs = tokens.next();
        } else {
            docs = null;
        }

        if (tokens.peek().isEmpty() || tokens.peek().get().type != TokenType.IDENTIFIER) {
            if (docs != null)
                tokens.rewind();
            return Optional.empty();
        }

        return Optional.of(
                consumeToken(tokens, TokenType.IDENTIFIER).map(identifier ->
                        consumeToken(tokens, TokenType.TYPE).then(() ->
                                parseType(tokens).map(type -> consumeToken(tokens, TokenType.SEMICOLON).then(() -> Messenger.success(new Documentable<>(docs, new Field(
                                        Range.combine(identifier.getRange(), type.getRange()),
                                        identifier, type
                                ))))))));
    }

    @SuppressWarnings("unchecked")
    private Optional<Messenger<Func>> parseFunc(TokenStream tokens) {
        if (tokens.peek().isEmpty() || tokens.peek().get().type != TokenType.KW_FUNC)
            return Optional.empty();

        Position start = tokens.currentPosition();

        return Optional.of(parseMulti(List.of(
                () -> consumeToken(tokens, TokenType.KW_FUNC),      // 0
                () -> consumeToken(tokens, TokenType.IDENTIFIER),   // 1
                () -> parseParamList(tokens),                       // 2
                () -> parseReturnType(tokens),                      // 3
                () -> parseFuncBlock(tokens)                        // 4
        )).map(list -> {
            Token identifier = (Token) list.get(1);
            ParamList paramList = (ParamList) list.get(2);
            Optional<Type> returnType = (Optional<Type>) list.get(3);
            FuncBlock funcBlock = (FuncBlock) list.get(4);

            Func func = new Func(
                    new Range(start, tokens.currentEndPosition()),
                    identifier, paramList, returnType.orElse(null), funcBlock
            );

            return Messenger.success(func);
        }));
    }

    @SuppressWarnings("unchecked")
    private Optional<Messenger<FuncHook>> parseHook(TokenStream tokens) {
        if (tokens.peek().isEmpty() || (tokens.peek().get().type != TokenType.KW_BEFORE && tokens.peek().get().type != TokenType.KW_AFTER))
            return Optional.empty();

        Position start = tokens.currentPosition();

        Token time = tokens.next();

        if (tokens.peek().isEmpty())
            return Optional.of(Messenger.fail(new ArrayList<>(List.of(new Message(
                    new Range(tokens.currentPosition()),
                    Message.MessageSeverity.ERROR,
                    "Expected hook target (func), got end of file"
            )))));
        Token target = tokens.next();
        if (target.type == TokenType.KW_FUNC) {
            // This is a func hook.
            return Optional.of(parseMulti(List.of(
                    () -> parseName(tokens),                            // 0
                    () -> parseParamList(tokens),                       // 1
                    () -> parseReturnType(tokens),                      // 2
                    () -> parseFuncBlock(tokens)                        // 3
            )).map(list -> {
                NameLiteral name = (NameLiteral) list.get(0);
                ParamList paramList = (ParamList) list.get(1);
                Optional<Type> returnType = (Optional<Type>) list.get(2);
                FuncBlock funcBlock = (FuncBlock) list.get(3);

                ArrayList<Message> messages = new ArrayList<>(0);
                if (funcBlock.isNative()) {
                    messages.add(new Message(
                            funcBlock.getRange(),
                            Message.MessageSeverity.ERROR,
                            "Native blocks are not allowed in hooks"
                    ));
                }

                FuncHook hook = new FuncHook(
                        new Range(start, tokens.currentEndPosition()),
                        time, name, paramList, returnType.orElse(null), funcBlock
                );

                return Messenger.success(hook, messages);
            }));
        } else {
            return Optional.of(Messenger.fail(new ArrayList<>(List.of(new Message(
                    new Range(tokens.currentPosition()),
                    Message.MessageSeverity.ERROR,
                    "Expected hook target (func), got " + target.type
            )))));
        }
    }

    /**
     * An optional comma may be used after the last parameter.
     */
    private ListMessenger<Param> parseParamListInternal(TokenStream tokens) {
        ListMessenger<Param> result = new ListMessenger<>();
        AtomicBoolean expectingMoreParams = new AtomicBoolean(true);
        while (expectingMoreParams.get() && tokens.peek().isPresent() && tokens.peek().get().type == TokenType.IDENTIFIER) {
            Messenger<Param> param = parseMulti(List.of(
                    () -> consumeToken(tokens, TokenType.IDENTIFIER),   // 0
                    () -> consumeToken(tokens, TokenType.TYPE),         // 1
                    () -> parseType(tokens)                             // 2
            )).map(list -> {
                Token name = (Token) list.get(0);
                Type type = (Type) list.get(2);

                if (tokens.peek().isPresent() && tokens.peek().get().type == TokenType.COMMA) {
                    consumeToken(tokens, TokenType.COMMA);
                } else {
                    expectingMoreParams.set(false);
                }

                return Messenger.success(new Param(Range.combine(name.getRange(), type.getRange()), name, type));
            });
            result.add(param);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Messenger<ParamList> parseParamList(TokenStream tokens) {
        Position start = tokens.currentPosition();
        return parseMulti(List.of(
                () -> consumeToken(tokens, TokenType.LPARENTH),     // 0
                () -> parseParamListInternal(tokens),               // 1
                () -> consumeToken(tokens, TokenType.RPARENTH)      // 2
        )).map(list -> {
            ArrayList<Param> params = (ArrayList<Param>) list.get(1);
            return Messenger.success(new ParamList(new Range(start, tokens.currentEndPosition()), params));
        });
    }

    private Messenger<Optional<Type>> parseReturnType(TokenStream tokens) {
        if (tokens.peek().isPresent() && tokens.peek().get().type == TokenType.RETURNS) {
            tokens.next();  // consume the RETURNS token
            return parseType(tokens).map(t -> Messenger.success(Optional.of(t)));
        }
        return Messenger.success(Optional.empty());
    }

    private Messenger<FuncBlock> parseFuncBlock(TokenStream tokens) {
        if (tokens.peek(2).isPresent() && tokens.peek(2).get().type == TokenType.KW_NATIVE) {
            // This is a { native } block.
            return parseMulti(List.of(
                    () -> consumeToken(tokens, TokenType.LBRACE),       // 0
                    () -> consumeToken(tokens, TokenType.KW_NATIVE),    // 1
                    () -> consumeToken(tokens, TokenType.RBRACE)        // 2
            )).map(list -> {
                Token first = ((Token) list.get(0));
                Token last = ((Token) list.get(2));
                Range totalRange = Range.combine(first.getRange(), last.getRange());

                return Messenger.success(new FuncBlock(totalRange, null));
            });
        }
        return parseCompoundStatement(tokens).map(block -> Messenger.success(new FuncBlock(new Range(tokens.currentPosition()), block)));
    }

    //#endregion

    //#region Statements

    @SuppressWarnings("unchecked")
    private Messenger<CompoundStatement> parseCompoundStatement(TokenStream tokens) {
        return parseMulti(List.of(
                () -> consumeToken(tokens, TokenType.LBRACE),       // 0
                () -> parseGreedy(() -> parseStatement(tokens)),    // 1
                () -> consumeToken(tokens, TokenType.RBRACE)        // 2
        )).map(list -> {
            Token first = ((Token) list.get(0));
            ArrayList<Statement> statements = ((ArrayList<Statement>) list.get(1));
            Token last = ((Token) list.get(2));
            Range totalRange = Range.combine(first.getRange(), last.getRange());

            return Messenger.success(new CompoundStatement(totalRange, statements));
        });
    }

    private Optional<Messenger<Statement>> parseStatement(TokenStream tokens) {
        if (tokens.peek().isEmpty())
            return Optional.empty();
        Token peek = tokens.peek().get();
        Messenger<Statement> result;
        switch (peek.type) {
            case LBRACE:
                // The map(Messenger::success) downcasts the Messenger<CompoundStatement> to a Messenger<Statement>.
                result = parseCompoundStatement(tokens).map(Messenger::success);
                break;
            case KW_LET:
                result = parseLetStmt(tokens);
                break;
            case KW_RETURN:
                result = parseReturnStmt(tokens);
                break;
            case KW_IF:
                result = parseIfStmt(tokens);
                break;
            case KW_FOR:
                result = parseForStmt(tokens);
                break;
            case KW_WHILE:
                result = parseWhileStmt(tokens);
                break;
            default: {
                if (!peekExpr(tokens))
                    return Optional.empty();
                result = parseExprStmt(tokens);
            }
        }
        // All statements end in a semicolon.
        return Optional.of(consumeToken(tokens, TokenType.SEMICOLON).then(() -> result));
    }

    /**
     * Does the token stream look like a list of tokens that could represent an expression?
     */
    private boolean peekExpr(TokenStream tokens) {
        if (tokens.peek().isEmpty())
            return false;
        switch (tokens.peek().get().type) {
            case IDENTIFIER:
            case INTEGER_LITERAL:
            case STRING_LITERAL:
            case MINUS:
            case NOT:
            case KW_TRUE:
            case KW_FALSE:
            case LPARENTH:
            case KW_RESULT:
                return true;
        }
        return false;
    }

    /**
     * Parses a statement that begins with an expression.
     */
    private Messenger<Statement> parseExprStmt(TokenStream tokens) {
        Messenger<Expression> expr = parseExpr(tokens);
        if (tokens.peek().isPresent() && tokens.peek().get().type == TokenType.ASSIGN) {
            // This is an assign statement.
            // Assign := Expr '=' Expr
            Messenger<Token> assign = consumeToken(tokens, TokenType.ASSIGN);
            Messenger<Expression> expr2 = parseExpr(tokens);
            return expr.map(lvalue -> assign.then(() -> expr2.map(rvalue -> Messenger.success(new AssignStatement(lvalue, rvalue)))));
        }
        return expr.map(expression -> Messenger.success(new EvaluateStatement(expression)));
    }

    private Messenger<Statement> parseLetStmt(TokenStream tokens) {
        Messenger<Token> let = consumeToken(tokens, TokenType.KW_LET);
        Messenger<Token> name = consumeToken(tokens, TokenType.IDENTIFIER);
        if (tokens.peek().isEmpty()) {
            return Messenger.fail(new ArrayList<>(List.of(new Message(
                    new Range(tokens.currentPosition()),
                    Message.MessageSeverity.ERROR,
                    "Expected " + TokenType.TYPE + " or " + TokenType.ASSIGN + ", got end of file"
            ))));
        }
        if (tokens.peek().get().type == TokenType.TYPE) {
            // This is a let-with-type statement.
            // LetWithType := "let" Identifier ":" Type
            Messenger<Token> typeSymbol = consumeToken(tokens, TokenType.TYPE);
            Messenger<Type> type = parseType(tokens);
            return let.map(let2 -> name.map(name2 -> typeSymbol.then(() -> type.map(type2 -> Messenger.success(new LetWithTypeStatement(
                    Range.combine(let2.getRange(), type2.getRange()),
                    name2, type2
            ))))));
        } else if (tokens.peek().get().type == TokenType.ASSIGN) {
            // This is a let-assign statement.
            // LetAssign := "let" Identifier "=" Expr
            Messenger<Token> assignSymbol = consumeToken(tokens, TokenType.ASSIGN);
            Messenger<Expression> expr = parseExpr(tokens);

            return let.map(let2 -> name.map(name2 -> assignSymbol.then(() -> expr.map(expr2 -> Messenger.success(new LetAssignStatement(
                    Range.combine(let2.getRange(), expr2.getRange()),
                    name2, expr2
            ))))));
        } else {
            return Messenger.fail(new ArrayList<>(List.of(new Message(
                    new Range(tokens.currentPosition()),
                    Message.MessageSeverity.ERROR,
                    "Expected " + TokenType.TYPE + " or " + TokenType.ASSIGN + ", got " + tokens.peek().get().type
            ))));
        }
    }

    private Messenger<Statement> parseReturnStmt(TokenStream tokens) {
        Messenger<Token> ret = consumeToken(tokens, TokenType.KW_RETURN);
        if (peekExpr(tokens)) {
            Messenger<Expression> expr = parseExpr(tokens);
            return ret.map(r -> expr.map(e -> {
                ArrayList<Statement> statements = new ArrayList<>();
                statements.add(AssignStatement.returnExpr(r, e));
                statements.add(new ReturnStatement(r.getRange(), true));
                return Messenger.success(new CompoundStatement(
                        Range.combine(statements.get(0).getRange(), statements.get(1).getRange()),
                        statements
                ));
            }));
        } else {
            return ret.map(r -> Messenger.success(new ReturnStatement(r.getRange(), false)));
        }
    }

    private Messenger<Statement> parseIfStmt(TokenStream tokens) {
        return parseMulti(List.of(
                () -> consumeToken(tokens, TokenType.KW_IF),    // 0
                () -> parseExpr(tokens),                        // 1
                () -> parseCompoundStatement(tokens)            // 2
        )).map(list -> {
            Expression condition = (Expression) list.get(1);
            Statement trueBlock = (Statement) list.get(2);

            if (tokens.peek().isPresent() && tokens.peek().get().type == TokenType.KW_ELSE) {
                // There is an else statement. Is it an else-if block or an else block?
                return parseElseStmt(tokens, condition, trueBlock);
            } else {
                return Messenger.success(new IfStatement(condition, trueBlock, null));
            }
        });
    }

    private Messenger<Statement> parseElseStmt(TokenStream tokens, Expression condition, Statement trueBlock) {
        return consumeToken(tokens, TokenType.KW_ELSE).map(elseToken -> {
            Messenger<Statement> elseBlock;
            if (tokens.peek().isPresent() && tokens.peek().get().type == TokenType.KW_IF) {
                elseBlock = parseIfStmt(tokens);
            } else {
                elseBlock = parseCompoundStatement(tokens).map(Messenger::success);
            }
            return elseBlock.map(falseBlock -> Messenger.success(new IfStatement(condition, trueBlock, falseBlock)));
        });
    }

    /**
     * For-in loops are desugared into while loops.
     *
     * <code><pre>
     *     for a in b {
     *         // foo
     *     }
     * </pre></code>
     *
     * is converted into
     *
     * <code><pre>
     *     let some_unique_identifier = 0
     *     while true {
     *         with a in b[some_unique_identifier] {
     *             some_unique_identifier = some_unique_identifier + 1
     *         } else {
     *             break
     *         }
     *     }
     * </pre></code>
     */
    private Messenger<Statement> parseForStmt(TokenStream tokens) {
        return parseMulti(List.of(
                () -> consumeToken(tokens, TokenType.KW_FOR),       // 0
                () -> consumeToken(tokens, TokenType.IDENTIFIER),   // 1
                () -> consumeToken(tokens, TokenType.KW_IN),        // 2
                () -> parseExpr(tokens),                            // 3
                () -> parseCompoundStatement(tokens)                // 4
        )).map(list -> {
            Token forToken = (Token) list.get(0);
            Token name = (Token) list.get(1);
            Expression container = (Expression) list.get(3);
            CompoundStatement block = (CompoundStatement) list.get(4);

            Range totalRange = Range.combine(forToken.getRange(), block.getRange());

            // TODO not implemented
            throw new UnsupportedOperationException();
        });
    }

    private Messenger<Statement> parseWhileStmt(TokenStream tokens) {
        return parseMulti(List.of(
                () -> consumeToken(tokens, TokenType.KW_WHILE), // 0
                () -> parseExpr(tokens),                        // 1
                () -> parseCompoundStatement(tokens)            // 2
        )).map(list -> {
            Token whileToken = (Token) list.get(0);
            Expression condition = (Expression) list.get(1);
            CompoundStatement block = (CompoundStatement) list.get(2);

            Range totalRange = Range.combine(whileToken.getRange(), block.getRange());

            return Messenger.success(new WhileStatement(totalRange, condition, block));
        });
    }

    //#endregion

    //#region Expressions

    /**
     * Expression messengers make use of <code>something.map(Messenger::success)</code> so that we can
     * downcast <code>Messenger&lt;? super T&gt;</code> to <code>Messenger&lt;T&gt;</code>.
     */
    private Messenger<Expression> parseExpr(TokenStream tokens) {
        return parseLogic(tokens);
    }

    /**
     * Logic := Relation (("or"|"and") Relation)?
     * Mixing use of "or"/"and" is forbidden, and will be validated against.
     */
    private Messenger<Expression> parseLogic(TokenStream tokens) {
        ArrayList<Message> messages;
        ArrayList<Expression> arguments = new ArrayList<>();
        Token expressionType = null;

        {
            Messenger<Expression> messenger = parseRelation(tokens);
            messages = new ArrayList<>(messenger.getMessages());
            messenger.getValue().ifPresent(arguments::add);
        }

        while (tokens.peek().isPresent() &&
                (tokens.peek().get().type == TokenType.LOGICAL_OR || tokens.peek().get().type == TokenType.LOGICAL_AND)) {
            Token logic = tokens.next();  // consume the logic token
            if (expressionType == null) {
                expressionType = logic;
            } else {
                // Check whether the given logic token is compatible with the expression type.
                if (expressionType.type != logic.type) {
                    messages.add(new Message(
                            logic.getRange(),
                            Message.MessageSeverity.ERROR,
                            "Incompatible logic operator; expected " + expressionType.type
                    ).addInfo(new Message.MessageRelatedInformation(
                            new Location(filePath, expressionType.getRange()),
                            "Incompatible with " + expressionType.type
                    )));
                }
            }

            Messenger<Expression> messenger = parseRelation(tokens);
            messages.addAll(messenger.getMessages());
            messenger.getValue().ifPresent(arguments::add);
        }

        if (arguments.isEmpty()) {
            return Messenger.fail(messages);
        } else if (arguments.size() == 1) {
            return Messenger.success(arguments.get(0), messages);
        } else {
            return Messenger.success(new LogicExpression(expressionType, arguments), messages);
        }
    }

    /**
     * Logic := Add (("or"|"and") Logic)?
     * Mixing use of greater/less/equal is forbidden, and will be validated against.
     * TODO make this slightly more lenient? E.g. 0 < x <= 10
     */
    private Messenger<Expression> parseRelation(TokenStream tokens) {
        ArrayList<Message> messages;
        ArrayList<Expression> arguments = new ArrayList<>();
        Token expressionType = null;

        {
            Messenger<Expression> messenger = parseAdd(tokens);
            messages = new ArrayList<>(messenger.getMessages());
            messenger.getValue().ifPresent(arguments::add);
        }

        while (tokens.peek().isPresent() &&
                (tokens.peek().get().type == TokenType.EQUAL ||
                        tokens.peek().get().type == TokenType.NOT_EQUAL ||
                        tokens.peek().get().type == TokenType.LESS ||
                        tokens.peek().get().type == TokenType.LESS_EQUAL ||
                        tokens.peek().get().type == TokenType.GREATER ||
                        tokens.peek().get().type == TokenType.GREATER_EQUAL)) {
            Token relation = tokens.next();  // consume the relation token
            if (expressionType == null) {
                expressionType = relation;
            } else {
                // Check whether the given logic token is compatible with the expression type.
                if (expressionType.type != relation.type) {
                    messages.add(new Message(
                            relation.getRange(),
                            Message.MessageSeverity.ERROR,
                            "Incompatible relation operator; expected " + expressionType.type
                    ).addInfo(new Message.MessageRelatedInformation(
                            new Location(filePath, expressionType.getRange()),
                            "Incompatible with " + expressionType.type
                    )));
                }
            }

            Messenger<Expression> messenger = parseAdd(tokens);
            messages.addAll(messenger.getMessages());
            messenger.getValue().ifPresent(arguments::add);
        }

        if (arguments.isEmpty()) {
            return Messenger.fail(messages);
        } else if (arguments.size() == 1) {
            return Messenger.success(arguments.get(0), messages);
        } else {
            return Messenger.success(new RelationExpression(expressionType, arguments), messages);
        }
    }

    /**
     * Add := Multiply (('+'|'-') Add)?
     * Addition/subtraction are left-associative.
     */
    private Messenger<Expression> parseAdd(TokenStream tokens) {
        Messenger<Expression> expr = parseMultiply(tokens);
        while (tokens.peek().isPresent()) {
            Messenger<Expression> finalExpr = expr;
            if (tokens.peek().get().type == TokenType.PLUS) {
                expr = consumeToken(tokens, TokenType.PLUS).then(() -> parseMultiply(tokens).map(right -> finalExpr.map(e -> Messenger.success(new AddExpression(e, right)))));
            } else if (tokens.peek().get().type == TokenType.MINUS) {
                expr = consumeToken(tokens, TokenType.MINUS).then(() -> parseMultiply(tokens).map(right -> finalExpr.map(e -> Messenger.success(new SubtractExpression(e, right)))));
            } else {
                break;
            }
        }
        return expr;
    }

    /**
     * Multiply := Prefix (('*'|'/') Multiply)?
     */
    private Messenger<Expression> parseMultiply(TokenStream tokens) {
        Messenger<Expression> expr = parsePrefix(tokens);
        while (tokens.peek().isPresent()) {
            Messenger<Expression> finalExpr = expr;
            if (tokens.peek().get().type == TokenType.STAR) {
                expr = consumeToken(tokens, TokenType.STAR).then(() -> parsePrefix(tokens).map(right -> finalExpr.map(e -> Messenger.success(new MultiplyExpression(e, right)))));
            } else if (tokens.peek().get().type == TokenType.SLASH) {
                expr = consumeToken(tokens, TokenType.SLASH).then(() -> parsePrefix(tokens).map(right -> finalExpr.map(e -> Messenger.success(new DivideExpression(e, right)))));
            } else {
                break;
            }
        }
        return expr;
    }

    /**
     * Prefix := ('!'|'-')* Postfix
     */
    private Messenger<Expression> parsePrefix(TokenStream tokens) {
        return ifConsumeToken(tokens, TokenType.NOT, () ->
                parsePrefix(tokens).map(arg -> Messenger.success((Expression) new LogicalNotExpression(arg)))
        ).or(() -> ifConsumeToken(tokens, TokenType.MINUS, () ->
                parsePrefix(tokens).map(arg -> Messenger.success(new UnaryMinusExpression(arg)))
        )).orElseGet(() -> parsePostfix(tokens));
    }

    /**
     * Postfix := Term ( '[' Expr ']' | '(' ArgList ')' | '.' Name )*
     */
    private Messenger<Expression> parsePostfix(TokenStream tokens) {
        return parseTerm(tokens).map(term -> parsePostfixOnExpression(term, tokens));
    }

    private Messenger<Expression> parsePostfixOnExpression(Expression term, TokenStream tokens) {
        return ifConsumeToken(tokens, TokenType.LSQUARE, () ->
                parseExpr(tokens).map(arg -> consumeToken(tokens, TokenType.RSQUARE).then(() -> parsePostfixOnExpression(new IndexExpression(term, arg), tokens)))
        ).or(() -> ifConsumeToken(tokens, TokenType.LPARENTH, () ->
                parseArgList(tokens).map(args -> consumeToken(tokens, TokenType.RPARENTH).then(() -> parsePostfixOnExpression(new FunctionInvocationExpression(term, args), tokens)))
        )).or(() -> ifConsumeToken(tokens, TokenType.DOT, () ->
                parseName(tokens).map(field -> parsePostfixOnExpression(new FieldExpression(term, field), tokens))
        )).orElse(Messenger.success(term));
    }

    /**
     * An optional comma may be used after the last argument.
     */
    private ListMessenger<Expression> parseArgList(TokenStream tokens) {
        ListMessenger<Expression> result = new ListMessenger<>();
        AtomicBoolean expectingMoreArgs = new AtomicBoolean(true);
        while (expectingMoreArgs.get() && tokens.peek().isPresent() && tokens.peek().get().type != TokenType.RPARENTH) {
            Messenger<Expression> arg = parseExpr(tokens).map(expr -> {
                if (tokens.peek().isPresent() && tokens.peek().get().type == TokenType.COMMA) {
                    consumeToken(tokens, TokenType.COMMA);
                } else {
                    expectingMoreArgs.set(false);
                }
                return Messenger.success(expr);
            });
            result.add(arg);
        }
        return result;
    }

    private Messenger<Expression> parseTerm(TokenStream tokens) {
        if (tokens.peek().isEmpty()) {
            return Messenger.fail(new ArrayList<>(List.of(new Message(
                    new Range(tokens.currentPosition()),
                    Message.MessageSeverity.ERROR,
                    "Expected term, got end of file"
            ))));
        }

        switch (tokens.peek().get().type) {
            case IDENTIFIER:
                return parseName(tokens).map(name -> Messenger.success(new Identifier(name)));
            case STRING_LITERAL:
                return Messenger.success(new StringLiteral(tokens.next()));
            case INTEGER_LITERAL:
                return Messenger.success(new IntegerLiteral(tokens.next()));
            case KW_TRUE:
            case KW_FALSE:
                return Messenger.success(new BooleanLiteral(tokens.next()));
            case LPARENTH: {
                tokens.next();  // consume opening parenthesis
                return parseExpr(tokens).map(expr -> consumeToken(tokens, TokenType.RPARENTH).then(() -> Messenger.success(expr)));
            }
            case KW_RESULT:
                return Messenger.success(new ResultExpression(tokens.next().getRange()));
        }

        return Messenger.fail(new ArrayList<>(List.of(new Message(
                new Range(tokens.currentPosition()),
                Message.MessageSeverity.ERROR,
                "Expected term, got " + tokens.next().type
        ))));
    }

    //#endregion

    //#region Utilities

    private Messenger<Type> parseType(TokenStream tokens) {
        Messenger<Type> result = null;
        if (tokens.peek().isPresent()) {
            switch (tokens.peek().get().type) {
                case KW_INT:
                case KW_BOOL:
                case KW_STRING:
                case KW_TEXT:
                case KW_ENTITY:
                case KW_RATIO:
                case KW_COL:
                case KW_POS:
                case KW_TEXTURE:
                case KW_PLAYER:
                    result = consumeToken(tokens, tokens.peek().get().type).map(tk -> Messenger.success(new Type.PrimitiveType(tk)));
                    break;
                case IDENTIFIER:
                    result = parseName(tokens).map(name -> Messenger.success(new Type.StructType(name.getRange(), name)));
                    break;
                case LSQUARE:
                    result = parseListType(tokens);
                    break;
                case LBRACE:
                    result = parseMapType(tokens);
                    break;
            }
        }

        if (result == null) {
            return Messenger.fail(new ArrayList<>(List.of(new Message(
                    new Range(tokens.currentPosition()),
                    Message.MessageSeverity.ERROR,
                    "Expected type"
            ))));
        }

        while (tokens.peek().isPresent() && tokens.peek().get().type == TokenType.TYPE_MAYBE) {
            Messenger<Type> finalResult = result;
            result = consumeToken(tokens, TokenType.TYPE_MAYBE).map(maybe -> finalResult.map(type -> Messenger.success(new Type.MaybeType(type, maybe))));
        }

        return result;
    }

    private Messenger<Type> parseListType(TokenStream tokens) {
        return consumeToken(tokens, TokenType.LSQUARE).map(startToken ->
                parseType(tokens).map(type ->
                        consumeToken(tokens, TokenType.RSQUARE).map(endToken ->
                                Messenger.success(new Type.ListType(type, startToken, endToken)))));
    }

    private Messenger<Type> parseMapType(TokenStream tokens) {
        return parseMulti(List.of(
                () -> consumeToken(tokens, TokenType.LBRACE),       // 0
                () -> parseType(tokens),                            // 1
                () -> consumeToken(tokens, TokenType.TYPE_MAPS_TO), // 2
                () -> parseType(tokens),                            // 3
                () -> consumeToken(tokens, TokenType.RBRACE)        // 4
        )).map(list -> {
            Token startToken = (Token) list.get(0);
            Type keyType = (Type) list.get(1);
            Type valueType = (Type) list.get(3);
            Token endToken = (Token) list.get(4);

            return Messenger.success(new Type.MapType(keyType, valueType, startToken, endToken));
        });
    }

    /**
     * Must start with an IDENTIFIER token.
     */
    private Messenger<NameLiteral> parseName(TokenStream tokens) {
        Position start = tokens.currentPosition();
        ListMessenger<Token> segments = new ListMessenger<>();
        segments.add(consumeToken(tokens, TokenType.IDENTIFIER));
        while (segments.getValue().isPresent()  // ensure we haven't already errored by consuming the wrong token
                && tokens.peek().isPresent()  // ensure we don't read past the end of the file
                && tokens.peek().get().type == TokenType.SCOPE_RESOLUTION) {
            tokens.next();  // consume the scope resolution token
            segments.add(consumeToken(tokens, TokenType.IDENTIFIER));
        }
        return segments.map(s -> Messenger.success(new NameLiteral(new Range(start, tokens.currentEndPosition()), s)));
    }

    /**
     * Executes the given parser as many times as possible.
     * The parser is expected to return <code>Optional.empty()</code> when the token stream no
     * longer represents a valid instance of T.
     *
     * The messages from each invocation of the parser will be collated in the result variable.
     *
     * @param parser The parser to repeatedly execute.
     */
    private <T> ListMessenger<T> parseGreedy(Supplier<Optional<Messenger<T>>> parser) {
        ListMessenger<T> values = new ListMessenger<>();
        while (true) {
            Optional<Messenger<T>> value = parser.get();
            if (value.isPresent()) {
                values.add(value.get());
            } else {
                break;
            }
        }
        return values;
    }

    /**
     * Executes the given parsers consecutively until one fails.
     */
    private ListMessenger<Object> parseMulti(List<Supplier<Messenger<?>>> values) {
        ListMessenger<Object> result = new ListMessenger<>(values.size());
        for (Supplier<Messenger<?>> value : values) {
            result.add(value.get());
        }
        return result;
    }

    private Messenger<Token> consumeToken(TokenStream tokens, TokenType type) {
        // Match SEMICOLON and IMPLICIT_SEMICOLON.
        if (type == TokenType.SEMICOLON)
            type = TokenType.IMPLICIT_SEMICOLON;
        Optional<Token> peek = tokens.peek();
        if (peek.isEmpty())
            return Messenger.fail(new ArrayList<>(List.of(new Message(
                    new Range(tokens.currentPosition()),
                    Message.MessageSeverity.ERROR,
                    "Expected " + type + ", got end of file"
            ))));
        if ((peek.get().type != type) && !(type == TokenType.IMPLICIT_SEMICOLON && peek.get().type == TokenType.SEMICOLON))
            return Messenger.fail(new ArrayList<>(List.of(new Message(
                    new Range(tokens.currentPosition()),
                    Message.MessageSeverity.ERROR,
                    "Expected " + type + ", got " + peek.get().type
            ))));
        return Messenger.success(tokens.next());
    }

    /**
     * Returns an empty Optional if the specified token did not exist.
     */
    private Optional<Messenger<Token>> peekConsumeToken(TokenStream tokens, TokenType type) {
        if (tokens.peek().isPresent() && tokens.peek().get().type == type) {
            return Optional.of(consumeToken(tokens, type));
        }
        return Optional.empty();
    }

    /**
     * If the given token type is next, consume it and run the func, returning the result. Else, return Optional.empty.
     */
    private <T> Optional<Messenger<T>> ifConsumeToken(TokenStream tokens, TokenType type, Supplier<Messenger<T>> func) {
        return peekConsumeToken(tokens, type).map(token -> token.then(func));
    }

    //#endregion
}
