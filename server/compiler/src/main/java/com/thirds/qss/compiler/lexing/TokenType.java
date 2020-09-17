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

    STRING_LITERAL, INTEGER_LITERAL,
}
