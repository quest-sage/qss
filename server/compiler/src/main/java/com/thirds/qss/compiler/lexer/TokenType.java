package com.thirds.qss.compiler.lexer;

public enum TokenType {
    LPARENTH, RPARENTH,  // ( )
    LSQUARE, RSQUARE,  // [ ]
    LBRACE, RBRACE,  // { }

    TYPE,  // :
    SCOPE_RESOLUTION,  // ::
    SEMICOLON,  // ;
    IMPLICIT_SEMICOLON,  // inserted after newlines in certain situations (see lexer)

    KW_STRUCT, KW_FUNC, KW_BEFORE, KW_AFTER, KW_NATIVE,
    KW_IMPORT,
    IDENTIFIER,

    KW_INT, KW_BOOL, KW_STRING, KW_TEXT, KW_ENTITY, KW_RATIO, KW_COL, KW_POS, KW_TEXTURE, KW_PLAYER,
    KW_LET,

    STRING_LITERAL, INTEGER_LITERAL,

    PLUS, MINUS, STAR, SLASH,  // + - * /
    DOT, COMMA,  // . ,
    ASSIGN,  // =
    RETURNS,  // ->

    DOCUMENTATION_COMMENT,  // ** ... **
    ;

    @Override
    public String toString() {
        switch (this) {
            case LPARENTH:
                return "opening parenthesis '('";
            case RPARENTH:
                return "closing parenthesis ')'";
            case LSQUARE:
                return "opening square bracket '['";
            case RSQUARE:
                return "closing square bracket ']'";
            case LBRACE:
                return "opening brace bracket '{'";
            case RBRACE:
                return "closing brace bracket '}'";
            case TYPE:
                return "type indicator ':'";
            case SCOPE_RESOLUTION:
                return "path separator '::'";
            case SEMICOLON:
                return "semicolon ';'";
            case IMPLICIT_SEMICOLON:
                return "end of line";
            case KW_STRUCT:
                return "keyword 'struct'";
            case KW_FUNC:
                return "keyword 'func'";
            case KW_BEFORE:
                return "keyword 'before'";
            case KW_AFTER:
                return "keyword 'after'";
            case KW_NATIVE:
                return "keyword 'native'";
            case KW_IMPORT:
                return "keyword 'import'";
            case IDENTIFIER:
                return "identifier";
            case KW_INT:
                return "keyword 'Int'";
            case KW_BOOL:
                return "keyword 'Bool'";
            case KW_STRING:
                return "keyword 'String'";
            case KW_TEXT:
                return "keyword 'Text'";
            case KW_ENTITY:
                return "keyword 'Entity'";
            case KW_RATIO:
                return "keyword 'Ratio'";
            case KW_COL:
                return "keyword 'Col'";
            case KW_POS:
                return "keyword 'Pos'";
            case KW_TEXTURE:
                return "keyword 'Texture'";
            case KW_PLAYER:
                return "keyword 'Player'";
            case KW_LET:
                return "keyword 'let'";
            case STRING_LITERAL:
                return "string literal";
            case INTEGER_LITERAL:
                return "integer literal";
            case PLUS:
                return "plus sign '+'";
            case MINUS:
                return "minus sign '-'";
            case STAR:
                return "star '*'";
            case SLASH:
                return "slash '/'";
            case DOT:
                return "dot '.'";
            case COMMA:
                return "comma ','";
            case ASSIGN:
                return "assign symbol '='";
            case RETURNS:
                return "returns symbol '->'";
            case DOCUMENTATION_COMMENT:
                return "documentation comment";
        }
        return "UNKNOWN TOKEN " + name();
    }
}
