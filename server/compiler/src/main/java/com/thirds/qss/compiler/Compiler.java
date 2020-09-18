package com.thirds.qss.compiler;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.thirds.qss.QualifiedName;
import com.thirds.qss.compiler.indexer.TypeIndex;
import com.thirds.qss.compiler.indexer.TypeNameIndex;
import com.thirds.qss.compiler.lexer.Lexer;
import com.thirds.qss.compiler.lexer.TokenStream;
import com.thirds.qss.compiler.parser.Parser;
import com.thirds.qss.compiler.tree.Script;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The Compiler class encapsulates the compilation process for a given bundle.
 * Create <i>one</i> instance of the Compiler class per resource bundle.
 *
 * This class caches file content so that it is not repeatedly read from disk.
 * You can overwrite the cached file content by using the {@link #overwriteCachedFileContent} method.
 */
public class Compiler {
    private final Path bundleRoot;
    private final Path indexRoot;

    /**
     * Maps file paths (relative to the bundle root) to the file contents.
     *
     * TODO consider moving this to a guava Cache - making sure not to evict entries whose file content has been overwritten by overwriteCachedFileContent
     */
    private final Map<Path, String> cachedFileContent = new HashMap<>();

    /**
     * Maps folder paths (relative to the bundle root) to the list of children files (also relative to the bundle root).
     * This does NOT include subdirectories, only files.
     */
    private final Multimap<Path, Path> folderChildren = MultimapBuilder.hashKeys().arrayListValues().build();

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

    /**
     * @return Null if the file could not be read.
     */
    public String getFileContent(Path filePath) {
        return cachedFileContent.computeIfAbsent(filePath, k -> {
            try {
                return Files.readString(bundleRoot.resolve(k));
            } catch (IOException e) {
                return null;
            }
        });
    }

    /**
     * @return The new, updated collection of child paths.
     */
    public Collection<Path> updateFolderChildren(Path folderPath) {
        Collection<Path> paths = folderChildren.get(folderPath);
        paths.clear();
        for (File file : folderPath.toFile().listFiles()) {
            if (file.isFile() && file.getName().endsWith(".qss"))
                paths.add(file.toPath());
        }
        return paths;
    }

    public Collection<Path> getFolderChildren(Path folderPath) {
        Collection<Path> paths = folderChildren.get(folderPath);
        if (paths.isEmpty()) {
            return updateFolderChildren(folderPath);
        }
        return paths;
    }

    public void overwriteCachedFileContent(Path filePath, String fileContents) {
        cachedFileContent.put(filePath, fileContents);
    }

    public Messenger<Script> compile(Path filePath) {
        String fileContents = getFileContent(filePath);
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
