package com.thirds.qss.compiler;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.thirds.qss.QssLogger;
import com.thirds.qss.QualifiedName;
import com.thirds.qss.compiler.indexer.TypeIndex;
import com.thirds.qss.compiler.indexer.TypeNameIndex;
import com.thirds.qss.compiler.lexer.Lexer;
import com.thirds.qss.compiler.lexer.TokenStream;
import com.thirds.qss.compiler.parser.Parser;
import com.thirds.qss.compiler.tree.Script;
import com.thirds.qss.compiler.tree.SymbolMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

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
    private final Map<ScriptPath, String> cachedFileContent = new HashMap<>();

    /**
     * Maps folder paths (relative to the bundle root) to the list of children files (also relative to the bundle root).
     * This does NOT include subdirectories, only files.
     */
    private final Multimap<ScriptPath, ScriptPath> folderChildren = MultimapBuilder.hashKeys().arrayListValues().build();

    /**
     * Maps script names to the parsed file content.
     */
    private final Map<ScriptPath, Script> parsedFiles = new HashMap<>();

    /**
     * Caches the locations of all the symbols in a given file so that we can do efficient hover and jump-to-definition.
     */
    private final Cache<ScriptPath, SymbolMap> symbolMaps = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(1))
            .maximumSize(25)
            .build();

    /**
     * Maps package paths to their type name indices.
     */
    private final Map<ScriptPath, TypeNameIndex> typeNameIndices = new HashMap<>();

    /**
     * Maps package paths to their type indices.
     */
    private final Map<ScriptPath, TypeIndex> typeIndices = new HashMap<>();

    /**
     * @param bundleRoot If this is null, no index files will be created or read, and the compiler will be unable
     *                   to access imported files.
     */
    public Compiler(Path bundleRoot) {
        this.bundleRoot = bundleRoot;

        if (bundleRoot != null) {
            QssLogger.initialise(bundleRoot.resolve(".qss").resolve("logs"));

            // Create the index directory, if it does not exist.
            indexRoot = bundleRoot.resolve(".qss").resolve("index");
            indexRoot.toFile().mkdirs();
        } else {
            QssLogger.initialise(null);
            indexRoot = null;
        }
    }

    /**
     * @return Null if the file could not be read.
     */
    public String getFileContent(ScriptPath filePath) {
        return cachedFileContent.computeIfAbsent(filePath, k -> {
            try {
                return Files.readString(bundleRoot.resolve(k.toPath()));
            } catch (IOException e) {
                return null;
            }
        });
    }

    /**
     * @return The new, updated collection of child paths.
     */
    public Collection<ScriptPath> updateFolderChildren(ScriptPath folderPath) {
        Collection<ScriptPath> paths = folderChildren.get(folderPath);
        paths.clear();
        for (File file : folderPath.toPath().toFile().listFiles()) {
            if (file.isFile() && file.getName().endsWith(".qss"))
                paths.add(folderPath.appendSegment(file.getName()));
        }
        return paths;
    }

    public Collection<ScriptPath> getFolderChildren(ScriptPath folderPath) {
        Collection<ScriptPath> paths = folderChildren.get(folderPath);
        if (paths.isEmpty()) {
            return updateFolderChildren(folderPath);
        }
        return paths;
    }

    public void overwriteCachedFileContent(ScriptPath filePath, String fileContents) {
        cachedFileContent.put(filePath, fileContents);
        // Reparse the file.
        deleteCachedContent(filePath);
    }

    private void deleteCachedContent(ScriptPath filePath) {
        parsedFiles.remove(filePath);
        refreshSymbolMap(filePath);
    }

    /**
     * Parses the file, if the parsed result is not yet cached.
     * This does not perform any validation checks or produce any index output.
     * This discards any messages emitted by the lexer and parser.
     */
    public Script getParsed(ScriptPath filePath) {
        return parsedFiles.computeIfAbsent(filePath, k -> {
            String fileContents = getFileContent(filePath);
            Messenger<TokenStream> tokens = new Lexer(this).process(fileContents);
            return tokens.map(t -> new Parser(this).parse(filePath, t)).getValue().orElse(null);
        });
    }

    /**
     * Computes (if not cached) the symbol map for the given script.
     */
    public SymbolMap getSymbolMap(ScriptPath filePath) {
        try {
            return symbolMaps.get(filePath, () -> {
                Script script = getParsed(filePath);
                if (script == null)
                    throw new UnsupportedOperationException();
                return new SymbolMap(script);
            });
        } catch (ExecutionException e) {
            return null;
        }
    }

    public void refreshSymbolMap(ScriptPath filePath) {
        symbolMaps.invalidate(filePath);
    }

    /**
     * Executes the given function on each of the script's neighbours in its package, then on the script itself.
     *
     * This discards messages that came from different source files.
     */
    private <T> Messenger<T> forNeighbours(ScriptPath filePath, Script scriptParsed, Messenger<T> initial, BiFunction<Script, T, Messenger<T>> func) {
        Messenger<T> result = initial;
        for (ScriptPath folderChild : getFolderChildren(filePath.trimLastSegment())) {
            if (folderChild.equals(filePath))
                continue;
            Script parsed = getParsed(folderChild);
            if (parsed != null) {
                // Ignore error messages from outside this file.
                result = result.map(value -> func.apply(parsed, value).getValue()
                        .map(Messenger::success).orElseGet(() -> Messenger.fail(new ArrayList<>(0))));
            }
        }
        result = result.map(value -> func.apply(scriptParsed, value));
        return result;
    }

    public Messenger<Script> compile(ScriptPath filePath) {
        ScriptPath packagePath = filePath.trimLastSegment();
        String fileContents = getFileContent(filePath);
        Messenger<TokenStream> tokens = new Lexer(this).process(fileContents);
        Messenger<Script> script = tokens.map(t -> new Parser(this).parse(filePath, t));

        if (script.getValue().isEmpty()) {
            return script;
        } else {
            Script scriptParsed = script.getValue().get();
            deleteCachedContent(filePath);
            parsedFiles.put(filePath, scriptParsed);

            // Fill the index with each script in the package, making sure to do this script last.
            // If it's last, any name collisions will be reported in this file's error messages.
            Messenger<TypeNameIndex> typeNameIndex = forNeighbours(filePath, scriptParsed,
                    Messenger.success(new TypeNameIndex(new QualifiedName()), script.getMessages()),
                    (script2, index) -> index.addFrom(script2));

            // If there were no errors up to this point, we're OK to generate the index for the package.
            if (typeNameIndex.hasErrors()) {
                return typeNameIndex.then(() -> Messenger.success(scriptParsed));
            }

            // We will go ahead and generate the index. There might be errors when we do this
            // (e.g. field of undeclared type) but we'll just generate the index anyway.
            // We'll do the same thing where we generate this script last.
            Messenger<TypeIndex> typeIndex = forNeighbours(filePath, scriptParsed,
                    typeNameIndex.map(idx -> Messenger.success(new TypeIndex(idx))),
                    (script2, index) -> index.addFrom(script2));

            typeIndex.getValue().map(idx -> typeIndices.put(packagePath, idx));

            // Return the parsed script.
            return typeIndex.then(() -> Messenger.success(scriptParsed));
        }
    }
}
