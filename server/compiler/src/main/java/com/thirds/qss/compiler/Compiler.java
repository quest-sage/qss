package com.thirds.qss.compiler;

import com.thirds.qss.QualifiedName;
import com.thirds.qss.compiler.indexer.TypeIndex;
import com.thirds.qss.compiler.indexer.TypeNameIndex;
import com.thirds.qss.compiler.lexer.Lexer;
import com.thirds.qss.compiler.lexer.TokenStream;
import com.thirds.qss.compiler.parser.Parser;
import com.thirds.qss.compiler.tree.Script;

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

    public Messenger<Script> compile(Path filePath, String fileContents) {
        Messenger<TokenStream> tokens = new Lexer(this).process(fileContents);
        Messenger<Script> script = tokens.map(t -> new Parser(this).parse(filePath, t));
        if (script.getValue().isEmpty()) {
            return script;
        } else {
            Script scriptParsed = script.getValue().get();

            // Fill the index with each script in the package.
            Messenger<TypeNameIndex> typeNameIndex = Messenger.success(new TypeNameIndex(new QualifiedName()), script.getMessages());
            typeNameIndex = typeNameIndex.map(index -> index.addFrom(scriptParsed));

            // If there were no errors up to this point, we're OK to generate the index for the package.
            if (typeNameIndex.hasErrors()) {
                return typeNameIndex.then(() -> Messenger.success(scriptParsed));
            }

            // We will go ahead and generate the index. There might be errors (e.g. field of undeclared type)
            // but we'll just generate the index anyway.
            Messenger<TypeIndex> typeIndex = typeNameIndex.map(idx -> Messenger.success(new TypeIndex(idx)));
            typeIndex = typeIndex.map(index -> index.addFrom(scriptParsed));

            // Return the parsed script.
            return typeIndex.then(() -> Messenger.success(scriptParsed));
        }
    }
}
