package com.thirds.qss.compiler.parser;

import com.thirds.qss.QualifiedName;
import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.*;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.lexer.TokenStream;
import com.thirds.qss.compiler.lexer.TokenType;
import com.thirds.qss.compiler.tree.*;
import com.thirds.qss.compiler.tree.expr.Expression;
import com.thirds.qss.compiler.tree.expr.Identifier;
import com.thirds.qss.compiler.tree.expr.IntegerLiteral;
import com.thirds.qss.compiler.tree.expr.StringLiteral;
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
    private final Compiler compiler;

    public Parser(Compiler compiler) {
        this.compiler = compiler;
    }

    public Messenger<Script> parse(ScriptPath filePath, TokenStream tokens) {
        Messenger<Script> script = parseScript(filePath, tokens);

        tokens.peek().ifPresent(token -> script.getMessages().add(new Message(
                token.getRange(),
                Message.MessageSeverity.ERROR,
                "Unexpected extra data at end of file, got " + token.type
        )));

        return script;
    }

    @SuppressWarnings("unchecked")
    public Messenger<Script> parseScript(ScriptPath filePath, TokenStream tokens) {
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

    public Optional<Messenger<Import>> parseImport(TokenStream tokens) {
        if (tokens.peek().isPresent() && tokens.peek().get().type == TokenType.KW_IMPORT) {
            tokens.next();  // consume the KW_IMPORT token
            return Optional.of(parseName(tokens).map(name -> Messenger.success(new Import(name))));
        }
        return Optional.empty();
    }

    public Optional<Messenger<Documentable<?>>> parseItem(TokenStream tokens) {
        Token docs;
        if (tokens.peek().isPresent() && tokens.peek().get().type == TokenType.DOCUMENTATION_COMMENT) {
            docs = tokens.next();
        } else {
            docs = null;
        }

        Optional<Messenger<Struct>> struct = parseStruct(tokens);
        if (struct.isPresent()) {
            return Optional.of(struct.get().map(s -> Messenger.success(new Documentable<>(docs, s))));
        }

        Optional<Messenger<Func>> func = parseFunc(tokens);
        if (func.isPresent()) {
            return Optional.of(func.get().map(s -> Messenger.success(new Documentable<>(docs, s))));
        }

        Optional<Messenger<FuncHook>> funcHook = parseHook(tokens);
        if (funcHook.isPresent()) {
            return Optional.of(funcHook.get().map(s -> Messenger.success(new Documentable<>(docs, s))));
        }

        if (docs != null)
            tokens.rewind();
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public Optional<Messenger<Func>> parseFunc(TokenStream tokens) {
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

    public Optional<Messenger<FuncHook>> parseHook(TokenStream tokens) {
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
                    () -> parseFuncBlock(tokens)                        // 2
            )).map(list -> {
                NameLiteral name = (NameLiteral) list.get(0);
                ParamList paramList = (ParamList) list.get(1);
                FuncBlock funcBlock = (FuncBlock) list.get(2);

                FuncHook hook = new FuncHook(
                        new Range(start, tokens.currentEndPosition()),
                        time, name, paramList, funcBlock
                );

                return Messenger.success(hook);
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
    public Messenger<ParamList> parseParamList(TokenStream tokens) {
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

    public Messenger<Optional<Type>> parseReturnType(TokenStream tokens) {
        if (tokens.peek().isPresent() && tokens.peek().get().type == TokenType.RETURNS) {
            tokens.next();  // consume the RETURNS token
            return parseType(tokens).map(t -> Messenger.success(Optional.of(t)));
        }
        return Messenger.success(Optional.empty());
    }

    public Messenger<FuncBlock> parseFuncBlock(TokenStream tokens) {
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
            case IDENTIFIER:
            case INTEGER_LITERAL:
            case STRING_LITERAL:
                result = parseExprStmt(tokens);
                break;
            default:
                return Optional.empty();
        }
        // All statements end in a semicolon.
        return Optional.of(consumeToken(tokens, TokenType.SEMICOLON).then(() -> result));
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

    private Messenger<Expression> parseExpr(TokenStream tokens) {
        return parseTerm(tokens);
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
        }

        return Messenger.fail(new ArrayList<>(List.of(new Message(
                new Range(tokens.currentPosition()),
                Message.MessageSeverity.ERROR,
                "Expected term, got " + tokens.next().type
        ))));
    }

    /**
     * <code>Struct := "struct" Identifier "{" Field* "}"</code>
     * @return Null if the token stream did not represent a struct.
     */
    @SuppressWarnings("unchecked")
    public Optional<Messenger<Struct>> parseStruct(TokenStream tokens) {
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

    public ListMessenger<Documentable<Field>> parseFields(TokenStream tokens) {
        return parseGreedy(() -> parseField(tokens));
    }

    /**
     * <code>Field := Documentation? Identifier ":" Type</code>
     */
    public Optional<Messenger<Documentable<Field>>> parseField(TokenStream tokens) {
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
        parseType(tokens).map(type -> Messenger.success(new Documentable<>(docs, new Field(
                Range.combine(identifier.getRange(), type.getRange()),
                identifier, type
        )))))));
    }

    public Messenger<Type> parseType(TokenStream tokens) {
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
                    return consumeToken(tokens, tokens.peek().get().type).map(tk -> Messenger.success(new Type.PrimitiveType(tk)));
            }
        }
        return parseName(tokens).map(name -> Messenger.success(new Type.StructType(name.getRange(), name)));
    }

    /**
     * Must start with an IDENTIFIER token.
     */
    public Messenger<NameLiteral> parseName(TokenStream tokens) {
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
    public <T> ListMessenger<T> parseGreedy(Supplier<Optional<Messenger<T>>> parser) {
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
    public ListMessenger<Object> parseMulti(List<Supplier<Messenger<?>>> values) {
        ListMessenger<Object> result = new ListMessenger<>(values.size());
        for (Supplier<Messenger<?>> value : values) {
            result.add(value.get());
        }
        return result;
    }

    public Messenger<Token> consumeToken(TokenStream tokens, TokenType type) {
        Optional<Token> peek = tokens.peek();
        if (peek.isEmpty())
            return Messenger.fail(new ArrayList<>(List.of(new Message(
                    new Range(tokens.currentPosition()),
                    Message.MessageSeverity.ERROR,
                    "Expected " + type + ", got end of file"
            ))));
        if (peek.get().type != type)
            return Messenger.fail(new ArrayList<>(List.of(new Message(
                    new Range(tokens.currentPosition()),
                    Message.MessageSeverity.ERROR,
                    "Expected " + type + ", got " + peek.get().type
            ))));
        return Messenger.success(tokens.next());
    }
}
