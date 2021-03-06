package com.thirds.qss.compiler.parser;

import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.ScriptPath;
import com.thirds.qss.compiler.lexer.Lexer;
import com.thirds.qss.compiler.lexer.TokenStream;
import com.thirds.qss.compiler.tree.Script;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    @Test
    void parseStruct() {
        Optional<TokenStream> tokens = new Lexer().process("struct struct struct A { }").getValue();
        assertTrue(tokens.isPresent());
        Optional<Script> script = new Parser(new ScriptPath(Paths.get("unit_test.qss"))).parse(tokens.get()).getValue();
        assertTrue(script.isPresent());
    }
}