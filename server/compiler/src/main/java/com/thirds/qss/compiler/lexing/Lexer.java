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
                case ')':
                    oneCharacter(tokens, codePoints, codePoints.next(), TokenType.RPARENTH, position);
                    break;
                case '{':
                    oneCharacter(tokens, codePoints, codePoints.next(), TokenType.LBRACE, position);
                    break;
                case '}':
                    oneCharacter(tokens, codePoints, codePoints.next(), TokenType.RBRACE, position);
                    break;
                case '[':
                    oneCharacter(tokens, codePoints, codePoints.next(), TokenType.LSQUARE, position);
                    break;
                case ']':
                    oneCharacter(tokens, codePoints, codePoints.next(), TokenType.RSQUARE, position);
                    break;
                case ':':
                    oneCharacter(tokens, codePoints, codePoints.next(), TokenType.TYPE, position);
                    break;
                case ';':
                    oneCharacter(tokens, codePoints, codePoints.next(), TokenType.SEMICOLON, position);
                    break;
                case '"':
                {
                    // This is a string literal.
                    Position start = position.copy();
                    codePoints.next();
                    position.character++;

                    StringBuilder content = new StringBuilder();
                    boolean failed = false;
                    int codePoint;
                    while ((codePoint = codePoints.peek()) != '"') {
                        if (codePoint == -1 || codePoint == '\n') {
                            messages.add(new Message(
                                    new Range(start, position),
                                    Message.MessageSeverity.ERROR,
                                    "Unclosed string literal"
                            ));
                            failed = true;
                            break;
                        }

                        codePoints.next();
                        position.character++;
                        if (codePoint == '\\') {
                            // This is an escape sequence.
                            // TODO parse this
                        } else {
                            content.append(Character.toString(codePoint));
                        }
                    }

                    if (!failed) {
                        codePoints.next();  // Consume the end quote character.
                        position.character++;
                    }

                    tokens.add(new Token(TokenType.STRING_LITERAL, content.toString(), new Range(start, position)));
                    break;
                }
                case '\n':
                    position.line++;
                    position.character = 0;
                    codePoints.next();
                    break;
                default:
                {
                    // The character must be a keyword, identifier, whitespace character or number.
                    if (Character.isWhitespace(peek)) {
                        codePoints.next();
                        position.character++;
                    } else if (isIdentifierStart(peek)) {
                        // This is an identifier.
                        StringBuilder identifier = new StringBuilder();
                        Position start = position.copy();
                        do {
                            identifier.append(Character.toString(codePoints.next()));
                            position.character++;
                        } while (codePoints.peek() != -1 && isIdentifierPart(codePoints.peek()));
                        TokenType type;
                        switch (identifier.toString()) {
                            case "struct":
                                type = TokenType.STRUCT;
                                break;
                            default:
                                type = TokenType.IDENTIFIER;
                        }
                        tokens.add(new Token(type, identifier.toString(), new Range(start, position)));
                    } else if (Character.isDigit(peek)) {
                        // This is a number.
                        StringBuilder identifier = new StringBuilder();
                        Position start = position.copy();
                        do {
                            identifier.append(Character.toString(codePoints.next()));
                            position.character++;
                        } while (codePoints.peek() != -1 && Character.isDigit(codePoints.peek()));
                        tokens.add(new Token(TokenType.INTEGER_LITERAL, identifier.toString(), new Range(start, position)));
                    } else {
                        messages.add(new Message(
                                new Range(position),
                                Message.MessageSeverity.ERROR,
                                "Character '" + Character.toString(peek) + "' (U+" + String.format("%04x", peek) + ") not recognised"
                        ));
                        position.character++;
                        codePoints.next();
                    }
                }
            }
        }

        return Messenger.success(new TokenStream(tokens), messages);
    }

    private boolean isIdentifierStart(int codePoint) {
        return Character.isLetter(codePoint);
    }

    private boolean isIdentifierPart(int codePoint) {
        return Character.isLetter(codePoint) || Character.isDigit(codePoint);
    }

    private void oneCharacter(ArrayList<Token> tokens, CodePointIterator codePoints, int codePoint, TokenType type, Position position) {
        tokens.add(new Token(type,
                Character.toString(codePoint),
                new Range(position)));
        position.character++;
    }
}
