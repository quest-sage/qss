package com.thirds.qss.compiler;

import java.nio.file.Path;

public class Compiler {
    private final Path bundleRoot;
    private final Path indexRoot;

    public Compiler(Path bundleRoot) {
        this.bundleRoot = bundleRoot;

        // Create the index directory, if it does not exist.
        indexRoot = bundleRoot.resolve(".qss").resolve("index");
        indexRoot.toFile().mkdirs();
    }

    public void compile(Path path, String fileContents) {
        
    }
}
