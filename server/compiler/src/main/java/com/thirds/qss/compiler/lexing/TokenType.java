package com.thirds.qss.compiler.lexing;

public enum TokenType {
    LPARENTH, RPARENTH,  // ( )
    LSQUARE, RSQUARE,  // [ ]
    LBRACE, RBRACE,  // { }

    TYPE,  // :
    SEMICOLON,  // ;

    STRUCT,
    IDENTIFIER,

    STRING_LITERAL, INTEGER_LITERAL,
}
