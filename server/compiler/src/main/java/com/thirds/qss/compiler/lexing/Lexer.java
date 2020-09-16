package com.thirds.qss.compiler.lexing;

import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.*;

import java.util.ArrayList;

public class Lexer {
    private final Compiler compiler;

    public Lexer(Compiler compiler) {
        this.compiler = compiler;
    }

    public Messenger<TokenStream> process(String input) {
        ArrayList<Token> tokens = new ArrayList<>();
        ArrayList<Message> messages = new ArrayList<>();

        CodePointIterator codePoints = new CodePointIterator(input);

        Position position = new Position(0, 0);

        while (codePoints.hasNext()) {
            int peek = codePoints.peek();

            switch (peek) {
                case '(':
                    oneCharacter(tokens, codePoints, codePoints.next(), TokenType.LPARENTH, position);
                    break;
                case '\n':
                    position.line++;
                    position.character = 0;
                    codePoints.next();
                    break;
                default:
                    messages.add(new Message(
                            new Range(position),
                            Message.MessageSeverity.ERROR,
                            "Character '" + Character.toString(peek) + "' (U+" + String.format("%04x", peek) + ") not recognised"
                    ));
                    position.character++;
                    codePoints.next();
            }
        }

        return Messenger.success(new TokenStream(tokens), messages);
    }

    private void oneCharacter(ArrayList<Token> tokens, CodePointIterator codePoints, int codePoint, TokenType type, Position position) {
        tokens.add(new Token(type,
                Character.toString(codePoint),
                new Range(position)));
        position.character++;
    }
}
