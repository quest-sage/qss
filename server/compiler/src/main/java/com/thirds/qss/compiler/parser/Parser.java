package com.thirds.qss.compiler.parser;

import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.*;
import com.thirds.qss.compiler.lexing.Token;
import com.thirds.qss.compiler.lexing.TokenStream;
import com.thirds.qss.compiler.lexing.TokenType;
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

    public Messenger<Script> parse(TokenStream tokens) {
        Messenger<Script> script = parseScript(tokens);

        tokens.peek().ifPresent(token -> script.getMessages().add(new Message(
                token.range,
                Message.MessageSeverity.ERROR,
                "Unexpected extra data at end of file"
        )));

        return script;
    }

    public Messenger<Script> parseScript(TokenStream tokens) {
        Position start = tokens.currentPosition();

        ListMessenger<Struct> structs = parseGreedy(() -> parseStruct(tokens));

        return structs.map(structs2 -> Messenger.success(new Script(
                new Range(start, tokens.currentPosition()),
                structs2
        )));
    }

    /**
     * <code>Struct := "struct" Identifier "{" Field* "}"</code>
     * @return Null if the token stream did not represent a struct.
     */
    @SuppressWarnings("unchecked")
    public Optional<Messenger<Struct>> parseStruct(TokenStream tokens) {
        if (tokens.peek().isEmpty() || tokens.peek().get().type != TokenType.STRUCT)
            return Optional.empty();

        Position start = tokens.currentPosition();

        return Optional.of(parseMulti(List.of(
                () -> consumeToken(tokens, TokenType.STRUCT),       // 0
                () -> consumeToken(tokens, TokenType.IDENTIFIER),   // 1
                () -> consumeToken(tokens, TokenType.LBRACE),       // 2
                () -> parseFields(tokens),                          // 3
                () -> consumeToken(tokens, TokenType.RBRACE)        // 4
        )).map(list -> {
            Token identifier = (Token) list.get(1);
            ArrayList<Field> fields = (ArrayList<Field>) list.get(3);

            Struct struct = new Struct(
                    new Range(start, tokens.currentPosition()),
                    identifier
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
                Range.combine(identifier.range, type.getRange()),
                identifier, type
        ))))));
    }

    public Messenger<Type> parseType(TokenStream tokens) {
        if (tokens.peek().isPresent()) {
            switch (tokens.peek().get().type) {
                case INT:
                case BOOL:
                case STRING:
                case TEXT:
                case ENTITY:
                case RATIO:
                case COL:
                case POS:
                case TEXTURE:
                case PLAYER:
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
