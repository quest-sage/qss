package com.thirds.qss.compiler.parser;

import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.Messenger;
import com.thirds.qss.compiler.lexing.Lexer;
import com.thirds.qss.compiler.lexing.TokenStream;
import com.thirds.qss.compiler.tree.Script;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    @Test
    void parseStruct() {
        Compiler compiler = new Compiler(null);
        Optional<TokenStream> tokens = new Lexer(compiler).process("struct struct struct A { }").getValue();
        assertTrue(tokens.isPresent());
        Optional<Script> script = new Parser(compiler).parse(tokens.get()).getValue();
        assertTrue(script.isPresent());
    }
}