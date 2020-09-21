package com.thirds.qss.compiler.lexer;

public enum TokenType {
    LPARENTH, RPARENTH,  // ( )
    LSQUARE, RSQUARE,  // [ ]
    LBRACE, RBRACE,  // { }

    TYPE,  // :
    SCOPE_RESOLUTION,  // ::
    SEMICOLON,  // ;

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
}
