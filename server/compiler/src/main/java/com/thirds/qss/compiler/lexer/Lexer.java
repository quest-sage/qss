package com.thirds.qss.compiler.lexer;

import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.Messenger;
import com.thirds.qss.compiler.Position;
import com.thirds.qss.compiler.Range;

import java.util.ArrayList;

public class Lexer {

    public Lexer() {
    }

    public Messenger<TokenStream> process(String input) {
        // Implicitly make a newline at the end of the file so that we can make implicit semicolons work right at
        // the end of the file.
        input = input + "\n";

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
                case '+':
                    oneCharacter(tokens, codePoints, codePoints.next(), TokenType.PLUS, position);
                    break;
                case '-': {
                    int firstCodePoint = codePoints.next();
                    if (codePoints.peek() == '>') {
                        twoCharacters(tokens, codePoints, firstCodePoint, codePoints.next(), TokenType.RETURNS, position);
                    } else {
                        oneCharacter(tokens, codePoints, firstCodePoint, TokenType.MINUS, position);
                    }
                    break;
                }
                case '.':
                    oneCharacter(tokens, codePoints, codePoints.next(), TokenType.DOT, position);
                    break;
                case ',':
                    oneCharacter(tokens, codePoints, codePoints.next(), TokenType.COMMA, position);
                    break;
                case '?':
                    oneCharacter(tokens, codePoints, codePoints.next(), TokenType.TYPE_MAYBE, position);
                    break;
                case '=': {
                    int firstCodePoint = codePoints.next();
                    if (codePoints.peek() == '=') {
                        twoCharacters(tokens, codePoints, firstCodePoint, codePoints.next(), TokenType.EQUAL, position);
                    } else if (codePoints.peek() == '>') {
                        twoCharacters(tokens, codePoints, firstCodePoint, codePoints.next(), TokenType.TYPE_MAPS_TO, position);
                    } else {
                        oneCharacter(tokens, codePoints, firstCodePoint, TokenType.ASSIGN, position);
                    }
                    break;
                }
                case '!': {
                    int firstCodePoint = codePoints.next();
                    if (codePoints.peek() == '=') {
                        twoCharacters(tokens, codePoints, firstCodePoint, codePoints.next(), TokenType.NOT_EQUAL, position);
                    } else {
                        oneCharacter(tokens, codePoints, firstCodePoint, TokenType.NOT, position);
                    }
                    break;
                }
                case '>': {
                    int firstCodePoint = codePoints.next();
                    if (codePoints.peek() == '=') {
                        twoCharacters(tokens, codePoints, firstCodePoint, codePoints.next(), TokenType.GREATER_EQUAL, position);
                    } else {
                        oneCharacter(tokens, codePoints, firstCodePoint, TokenType.GREATER, position);
                    }
                    break;
                }
                case '<': {
                    int firstCodePoint = codePoints.next();
                    if (codePoints.peek() == '=') {
                        twoCharacters(tokens, codePoints, firstCodePoint, codePoints.next(), TokenType.LESS_EQUAL, position);
                    } else {
                        oneCharacter(tokens, codePoints, firstCodePoint, TokenType.LESS, position);
                    }
                    break;
                }
                case '&': {
                    int firstCodePoint = codePoints.next();
                    if (codePoints.peek() == '&') {
                        twoCharacters(tokens, codePoints, firstCodePoint, codePoints.next(), TokenType.LOGICAL_AND, position);
                    } else {
                        oneCharacter(tokens, codePoints, firstCodePoint, TokenType.TYPE_AND, position);
                    }
                    break;
                }
                case '|': {
                    int firstCodePoint = codePoints.next();
                    if (codePoints.peek() == '|') {
                        twoCharacters(tokens, codePoints, firstCodePoint, codePoints.next(), TokenType.LOGICAL_OR, position);
                    } else {
                        oneCharacter(tokens, codePoints, firstCodePoint, TokenType.TYPE_OR, position);
                    }
                    break;
                }
                case ':': {
                    int firstCodePoint = codePoints.next();
                    if (codePoints.peek() == ':') {
                        twoCharacters(tokens, codePoints, firstCodePoint, codePoints.next(), TokenType.SCOPE_RESOLUTION, position);
                    } else {
                        oneCharacter(tokens, codePoints, firstCodePoint, TokenType.TYPE, position);
                    }
                    break;
                }
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
                case '*':
                {
                    int firstCodePoint = codePoints.next();
                    if (codePoints.peek() == '*') {
                        codePoints.next();

                        // This is a documentation comment.
                        Position start = position.copy();
                        position.character += 2;
                        codePoints.next();

                        StringBuilder content = new StringBuilder();
                        boolean failed = false;
                        int codePoint;
                        while (true) {
                            codePoint = codePoints.peek();

                            if (codePoint == -1) {
                                messages.add(new Message(
                                        new Range(start, position),
                                        Message.MessageSeverity.ERROR,
                                        "Unclosed documentation comment"
                                ));
                                failed = true;
                                break;
                            }

                            codePoints.next();
                            if (codePoint == '\n') {
                                position.character = 0;
                                position.line++;
                            } else {
                                position.character++;
                            }
                            if (codePoint == '*' && codePoints.peek() == '*') {
                                break;
                            } else {
                                content.append(Character.toString(codePoint));
                            }
                        }

                        if (!failed) {
                            codePoints.next();  // Consume the end star character.
                            position.character++;
                        }

                        tokens.add(new Token(TokenType.DOCUMENTATION_COMMENT, content.toString(), new Range(start, position)));
                    } else {
                        oneCharacter(tokens, codePoints, firstCodePoint, TokenType.STAR, position);
                    }
                    break;
                }
                case '/':
                {
                    int firstCodePoint = codePoints.next();
                    if (codePoints.peek() == '*') {
                        codePoints.next();

                        // This is a block comment.
                        Position start = position.copy();
                        position.character += 2;
                        codePoints.next();

                        boolean failed = false;
                        int codePoint;
                        while (true) {
                            codePoint = codePoints.peek();

                            if (codePoint == -1) {
                                messages.add(new Message(
                                        new Range(start, position),
                                        Message.MessageSeverity.ERROR,
                                        "Unclosed block comment"
                                ));
                                failed = true;
                                break;
                            }

                            codePoints.next();
                            if (codePoint == '\n') {
                                position.character = 0;
                                position.line++;
                            } else {
                                position.character++;
                            }
                            if (codePoint == '*' && codePoints.peek() == '/') {
                                break;
                            }
                        }

                        if (!failed) {
                            codePoints.next();  // Consume the end star character.
                            position.character++;
                        }
                    } else if (codePoints.peek() == '/') {
                        codePoints.next();

                        // This is a line comment.
                        Position start = position.copy();
                        position.character += 2;
                        codePoints.next();

                        int codePoint;
                        while (codePoints.peek() != '\n') {
                            codePoint = codePoints.next();

                            if (codePoint == -1) {
                                messages.add(new Message(
                                        new Range(start, position),
                                        Message.MessageSeverity.ERROR,
                                        "Unclosed block comment"
                                ));
                                break;
                            }

                            position.character++;
                        }
                    } else {
                        oneCharacter(tokens, codePoints, firstCodePoint, TokenType.SLASH, position);
                    }
                    break;
                }
                case '\n':
                    // Check to see if we need to input an implicit semicolon (using rules from https://golang.org/doc/effective_go.html#semicolons).
                    if (!tokens.isEmpty()) {
                        Token previousToken = tokens.get(tokens.size() - 1);
                        // Ensure the token we're checking is on the same line as this newline character.
                        // This stops multiple semicolons being added when multiple newlines are used after a token.
                        if (previousToken.getRange().end.line == position.line) {
                            if (implicitSemicolonAfter(previousToken.type)) {
                                // We should add an implicit semicolon.
                                tokens.add(new Token(
                                        TokenType.IMPLICIT_SEMICOLON,
                                        "<end of line>",
                                        new Range(position)
                                ));
                            }
                            if (previousToken.type == TokenType.SEMICOLON) {
                                // We should check if this semicolon is required or not.
                                // Then, if it is unnecessary, we can alert the programmer.
                                if (tokens.size() >= 2) {
                                    if (implicitSemicolonAfter(tokens.get(tokens.size() - 2).type)) {
                                        messages.add(new Message(
                                                previousToken.getRange(),
                                                Message.MessageSeverity.INFORMATION,
                                                "This semicolon is unnecessary"
                                        ));
                                    }
                                }
                            }
                        }
                    }
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
                                type = TokenType.KW_STRUCT;
                                break;
                            case "func":
                                type = TokenType.KW_FUNC;
                                break;
                            case "before":
                                type = TokenType.KW_BEFORE;
                                break;
                            case "after":
                                type = TokenType.KW_AFTER;
                                break;
                            case "native":
                                type = TokenType.KW_NATIVE;
                                break;
                            case "import":
                                type = TokenType.KW_IMPORT;
                                break;
                            case "let":
                                type = TokenType.KW_LET;
                                break;
                            case "return":
                                type = TokenType.KW_RETURN;
                                break;
                            case "result":
                                type = TokenType.KW_RESULT;
                                break;
                            case "Int":
                                type = TokenType.KW_INT;
                                break;
                            case "Bool":
                                type = TokenType.KW_BOOL;
                                break;
                            case "String":
                                type = TokenType.KW_STRING;
                                break;
                            case "Text":
                                type = TokenType.KW_TEXT;
                                break;
                            case "Entity":
                                type = TokenType.KW_ENTITY;
                                break;
                            case "Ratio":
                                type = TokenType.KW_RATIO;
                                break;
                            case "Col":
                                type = TokenType.KW_COL;
                                break;
                            case "Pos":
                                type = TokenType.KW_POS;
                                break;
                            case "Texture":
                                type = TokenType.KW_TEXTURE;
                                break;
                            case "Player":
                                type = TokenType.KW_PLAYER;
                                break;
                            case "true":
                                type = TokenType.KW_TRUE;
                                break;
                            case "false":
                                type = TokenType.KW_FALSE;
                                break;
                            case "if":
                                type = TokenType.KW_IF;
                                break;
                            case "else":
                                type = TokenType.KW_ELSE;
                                break;
                            case "for":
                                type = TokenType.KW_FOR;
                                break;
                            case "in":
                                type = TokenType.KW_IN;
                                break;
                            case "while":
                                type = TokenType.KW_WHILE;
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

    /**
     * Should we implicitly insert a semicolon after this token at the end of a line?
     * Adapted for QSS from https://golang.org/doc/effective_go.html#semicolons
     */
    private boolean implicitSemicolonAfter(TokenType tokenType) {
        switch (tokenType) {
            case IDENTIFIER:
            case INTEGER_LITERAL:
            case STRING_LITERAL:
            case RPARENTH:
            case RBRACE:
            case RSQUARE:
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
            case KW_TRUE:
            case KW_FALSE:
            case KW_RETURN:
            case KW_RESULT:
            case TYPE_MAYBE:
                return true;
            default:
                return false;
        }
    }

    private boolean isIdentifierStart(int codePoint) {
        return Character.isLetter(codePoint);
    }

    private boolean isIdentifierPart(int codePoint) {
        return Character.isLetter(codePoint) || Character.isDigit(codePoint) || codePoint == '_';
    }

    private void oneCharacter(ArrayList<Token> tokens, CodePointIterator codePoints, int codePoint, TokenType type, Position position) {
        tokens.add(new Token(type,
                Character.toString(codePoint),
                new Range(position)));
        position.character++;
    }

    private void twoCharacters(ArrayList<Token> tokens, CodePointIterator codePoints, int codePoint1, int codePoint2, TokenType type, Position position) {
        tokens.add(new Token(type,
                Character.toString(codePoint1) + Character.toString(codePoint2),
                new Range(position)));
        position.character += 2;
    }
}
