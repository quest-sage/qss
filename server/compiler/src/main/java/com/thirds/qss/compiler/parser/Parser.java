package com.thirds.qss.compiler.parser;

import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.*;
import com.thirds.qss.compiler.lexing.Token;
import com.thirds.qss.compiler.lexing.TokenStream;
import com.thirds.qss.compiler.lexing.TokenType;
import com.thirds.qss.compiler.tree.Identifier;
import com.thirds.qss.compiler.tree.Script;
import com.thirds.qss.compiler.tree.Struct;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Represents an LL(1) parser for QSS.
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
        ListMessenger<Struct> structs = new ListMessenger<>();
        Position start = tokens.currentPosition();

        while (true) {
            Optional<Messenger<Struct>> struct = parseStruct(tokens);
            if (struct.isPresent()) {
                structs.add(struct.get());
            } else {
                break;
            }
        }

        return structs.map(structs2 -> Messenger.success(new Script(
                new Range(start, tokens.currentPosition()),
                structs2
        )));
    }

    /**
     * <code>Struct := "struct" Identifier "{" Field* "}"</code>
     * @return Null if the token stream did not represent a struct.
     */
    public Optional<Messenger<Struct>> parseStruct(TokenStream tokens) {
        if (tokens.peek().isEmpty() || tokens.peek().get().type != TokenType.STRUCT)
            return Optional.empty();

        Position start = tokens.currentPosition();

        return Optional.of(parseMulti(List.of(
                () -> consumeToken(tokens, TokenType.STRUCT),       // 0
                () -> consumeToken(tokens, TokenType.IDENTIFIER),   // 1
                () -> consumeToken(tokens, TokenType.LBRACE),       // 2
                () -> consumeToken(tokens, TokenType.RBRACE)        // 3
        )).map(list -> {
            Struct struct = new Struct(
                    new Range(start, tokens.currentPosition())
            );

            return Messenger.success(struct);
        }));
    }

    /**
     * Executes the given parsers consecutively until one fails.
     */
    public ListMessenger<Object> parseMulti(List<Supplier<Messenger<?>>> values) {
        ListMessenger<Object> result = new ListMessenger<>();
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

    public Optional<Messenger<Identifier>> parseIdentifier(TokenStream tokens) {
        Optional<Token> peek = tokens.peek();
        if (peek.isEmpty() || peek.get().type != TokenType.IDENTIFIER)
            return Optional.empty();

        Token token = tokens.next();
        Identifier identifier = new Identifier(token.range, token.contents);
        return Optional.of(Messenger.success(identifier));
    }
}
