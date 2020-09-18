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
        Compiler compiler = new Compiler(null);
        Optional<TokenStream> tokens = new Lexer(compiler).process("struct struct struct A { }").getValue();
        assertTrue(tokens.isPresent());
        Optional<Script> script = new Parser(compiler).parse(new ScriptPath(Paths.get("unit_test.qss")), tokens.get()).getValue();
        assertTrue(script.isPresent());
    }
}