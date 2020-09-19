package com.thirds.qss.compiler.lexer;

public enum TokenType {
    LPARENTH, RPARENTH,  // ( )
    LSQUARE, RSQUARE,  // [ ]
    LBRACE, RBRACE,  // { }

    TYPE,  // :
    SCOPE_RESOLUTION,  // ::
    SEMICOLON,  // ;

    KW_STRUCT,
    IDENTIFIER,

    KW_INT, KW_BOOL, KW_STRING, KW_TEXT, KW_ENTITY, KW_RATIO, KW_COL, KW_POS, KW_TEXTURE, KW_PLAYER,

    KW_PACKAGE,

    STRING_LITERAL, INTEGER_LITERAL,

    PLUS, MINUS, STAR, SLASH,  // + - * /

    DOCUMENTATION_COMMENT,  // ** ... **
}
