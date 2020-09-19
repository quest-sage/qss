package com.thirds.qss.compiler.parser;

import com.thirds.qss.QualifiedName;
import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.*;
import com.thirds.qss.compiler.lexer.Token;
import com.thirds.qss.compiler.lexer.TokenStream;
import com.thirds.qss.compiler.lexer.TokenType;
import com.thirds.qss.compiler.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Represents an LL(1) recursive descent parser for QSS.
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
        ListMessenger<Documentable<?>> items = parseGreedy(() -> parseItem(tokens));

        return items.map(items2 -> {
            ArrayList<Documentable<Struct>> structs = new ArrayList<>();
            for (Documentable<?> documentable : items2) {
                Node content = documentable.getContent();
                if (content instanceof Struct)
                    structs.add((Documentable<Struct>) documentable);
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
                    filePath, new Range(start, tokens.currentPosition()),
                    packageName,
                    packagePath,
                    structs
            ), messages);
        });
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

        if (docs != null)
            tokens.rewind();
        return Optional.empty();
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
                () -> consumeToken(tokens, TokenType.KW_STRUCT),       // 0
                () -> consumeToken(tokens, TokenType.IDENTIFIER),   // 1
                () -> consumeToken(tokens, TokenType.LBRACE),       // 2
                () -> parseFields(tokens),                          // 3
                () -> consumeToken(tokens, TokenType.RBRACE)        // 4
        )).map(list -> {
            Token identifier = (Token) list.get(1);
            ArrayList<Field> fields = (ArrayList<Field>) list.get(3);

            Struct struct = new Struct(
                    new Range(start, tokens.currentPosition()),
                    identifier, fields
            );

            return Messenger.success(struct);
        }));
    }

    public ListMessenger<Field> parseFields(TokenStream tokens) {
        return parseGreedy(() -> parseField(tokens));
    }

    /**
     * <code>Field := Identifier ":" Type</code>
     */
    public Optional<Messenger<Field>> parseField(TokenStream tokens) {
        if (tokens.peek().isEmpty() || tokens.peek().get().type != TokenType.IDENTIFIER)
            return Optional.empty();

        return Optional.of(
        consumeToken(tokens, TokenType.IDENTIFIER).map(identifier ->
        consumeToken(tokens, TokenType.TYPE).then(() ->
        parseType(tokens).map(type -> Messenger.success(new Field(
                Range.combine(identifier.getRange(), type.getRange()),
                identifier, type
        ))))));
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
        return segments.map(s -> Messenger.success(new NameLiteral(new Range(start, tokens.currentPosition()), s)));
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
    @SuppressWarnings("unchecked")
    public ListMessenger<Object> parseMulti(List<Supplier<Messenger<?>>> values) {
        ListMessenger<Object> result = new ListMessenger<>(values.size());
        for (Supplier<Messenger<?>> value : values) {
            result.add((Messenger<Object>) value.get());
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
