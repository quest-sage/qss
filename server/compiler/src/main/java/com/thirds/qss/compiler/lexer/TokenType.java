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
    KW_TRAIT, KW_IMPL,
    KW_IMPORT,
    IDENTIFIER,

    // T prefix = type name (distinguishes e.g. T_FUNC 'Func' from lowercase keywords like KW_FUNC 'func')
    T_INT, T_BOOL, T_STRING, T_TEXT, T_ENTITY, T_RATIO, T_COL, T_POS, T_TEXTURE, T_PLAYER, T_THIS, T_FUNC,
    KW_TRUE, KW_FALSE, KW_JUST, KW_NULL,
    KW_LET, KW_RETURN, KW_NEW, KW_BREAK, KW_CONTINUE,
    KW_IF, KW_ELSE, KW_FOR, KW_IN, KW_WHILE,
    KW_RESULT, KW_THIS,

    STRING_LITERAL, INTEGER_LITERAL,

    PLUS, MINUS, STAR, SLASH,  // + - * /
    LOGICAL_AND, LOGICAL_OR, NOT,  // && || !
    DOT, COMMA,  // . ,
    ASSIGN,  // =
    RETURNS,  // ->
    EQUAL, NOT_EQUAL,  // == !=
    LESS, GREATER,  // < >
    LESS_EQUAL, GREATER_EQUAL,  // <= >=

    TYPE_AND, TYPE_OR, TYPE_MAYBE,  // & | ?
    TYPE_MAPS_TO,  // =>

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
            case KW_TRAIT:
                return "keyword 'trait'";
            case KW_IMPL:
                return "keyword 'impl'";
            case KW_IMPORT:
                return "keyword 'import'";
            case IDENTIFIER:
                return "identifier";
            case T_INT:
                return "type 'Int'";
            case T_BOOL:
                return "type 'Bool'";
            case T_STRING:
                return "type 'String'";
            case T_TEXT:
                return "type 'Text'";
            case T_ENTITY:
                return "type 'Entity'";
            case T_RATIO:
                return "type 'Ratio'";
            case T_COL:
                return "type 'Col'";
            case T_POS:
                return "type 'Pos'";
            case T_TEXTURE:
                return "type 'Texture'";
            case T_PLAYER:
                return "type 'Player'";
            case T_THIS:
                return "type 'This'";
            case T_FUNC:
                return "type 'Func'";
            case KW_TRUE:
                return "boolean 'true'";
            case KW_FALSE:
                return "boolean 'false'";
            case KW_JUST:
                return "keyword 'just'";
            case KW_NULL:
                return "keyword 'null'";
            case KW_LET:
                return "keyword 'let'";
            case KW_RETURN:
                return "keyword 'return'";
            case KW_NEW:
                return "keyword 'new'";
            case KW_BREAK:
                return "keyword 'break'";
            case KW_CONTINUE:
                return "keyword 'continue'";
            case KW_IF:
                return "keyword 'if'";
            case KW_ELSE:
                return "keyword 'else'";
            case KW_FOR:
                return "keyword 'for'";
            case KW_IN:
                return "keyword 'in'";
            case KW_WHILE:
                return "keyword 'while'";
            case KW_RESULT:
                return "keyword 'result'";
            case KW_THIS:
                return "keyword 'this'";
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
            case LOGICAL_AND:
                return "logical AND operator '&&'";
            case LOGICAL_OR:
                return "logical OR operator '||'";
            case NOT:
                return "logical NOT operator '!'";
            case DOT:
                return "dot '.'";
            case COMMA:
                return "comma ','";
            case ASSIGN:
                return "assign symbol '='";
            case RETURNS:
                return "returns symbol '->'";
            case EQUAL:
                return "equals symbol '='";
            case NOT_EQUAL:
                return "not-equals symbol '!='";
            case LESS:
                return "less-than symbol '<'";
            case GREATER:
                return "greater-than symbol '>'";
            case LESS_EQUAL:
                return "less-than-or-equal symbol '<='";
            case GREATER_EQUAL:
                return "greater-than-or-equal symbol '>='";
            case TYPE_AND:
                return "typewise 'and' operator '&'";
            case TYPE_OR:
                return "typewise 'or' operator '|'";
            case TYPE_MAYBE:
                return "typewise 'maybe' operator '?'";
            case TYPE_MAPS_TO:
                return "maps-to operator '=>'";
            case DOCUMENTATION_COMMENT:
                return "documentation comment";
        }
        return "UNKNOWN TOKEN " + name();
    }
}
