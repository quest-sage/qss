package com.thirds.qss.compiler.lexing;

public enum TokenType {
    LPARENTH, RPARENTH,  // ( )
    LSQUARE, RSQUARE,  // [ ]
    LBRACE, RBRACE,  // { }

    TYPE,  // :
    SCOPE_RESOLUTION,  // ::
    SEMICOLON,  // ;

    STRUCT,
    IDENTIFIER,

    INT, BOOL, STRING, TEXT, ENTITY, RATIO, COL, POS, TEXTURE, PLAYER,

    STRING_LITERAL, INTEGER_LITERAL,
}
