package com.thirds.qss.compiler;

import com.thirds.qss.compiler.lexing.Lexer;
import com.thirds.qss.compiler.lexing.TokenStream;

import java.nio.file.Path;

public class Compiler {
    private final Path bundleRoot;
    private final Path indexRoot;

    /**
     * @param bundleRoot If this is null, no index files will be created or read, and the compiler will be unable
     *                   to access imported files.
     */
    public Compiler(Path bundleRoot) {
        this.bundleRoot = bundleRoot;

        if (bundleRoot != null) {
            // Create the index directory, if it does not exist.
            indexRoot = bundleRoot.resolve(".qss").resolve("index");
            indexRoot.toFile().mkdirs();
        } else {
            indexRoot = null;
        }
    }

    public Messenger<TokenStream> compile(Path path, String fileContents) {
        Messenger<TokenStream> tokens = new Lexer(this).process(fileContents);
        return tokens;
    }
}
