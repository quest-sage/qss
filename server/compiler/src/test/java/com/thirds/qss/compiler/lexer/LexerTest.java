package com.thirds.qss.compiler.lexer;

import com.thirds.qss.compiler.*;
import com.thirds.qss.compiler.Compiler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

class LexerTest {

    @Test
    void unclosedStringLiteral() {
        ArrayList<Message> messages = new Lexer().process("\"").getMessages();
        assertThat(messages, hasItem(new Message(
                new Range(new Position(0, 0), new Position(0, 1)),
                Message.MessageSeverity.ERROR,
                "Unclosed string literal"
        )));
    }

    @Test
    void normal() {
        ArrayList<Message> messages = new Lexer().process("test").getMessages();
        assertThat(messages, empty());
    }
}