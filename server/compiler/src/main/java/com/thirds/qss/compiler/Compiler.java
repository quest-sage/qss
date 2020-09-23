package com.thirds.qss.compiler;

import com.github.jezza.Toml;
import com.github.jezza.TomlTable;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.thirds.qss.QssLogger;
import com.thirds.qss.QualifiedName;
import com.thirds.qss.compiler.indexer.Index;
import com.thirds.qss.compiler.indexer.Indices;
import com.thirds.qss.compiler.indexer.NameIndex;
import com.thirds.qss.compiler.indexer.NameIndices;
import com.thirds.qss.compiler.lexer.Lexer;
import com.thirds.qss.compiler.lexer.TokenStream;
import com.thirds.qss.compiler.parser.Parser;
import com.thirds.qss.compiler.tree.Documentable;
import com.thirds.qss.compiler.tree.Script;
import com.thirds.qss.compiler.tree.SymbolMap;
import com.thirds.qss.compiler.tree.script.Func;
import com.thirds.qss.compiler.type.TypeDeducer;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Consumer;

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
     * Maps bundles and package paths to their name indices.
     */
    private final NameIndices typeNameIndices = new NameIndices();

    /**
     * Maps bundles and package paths to their detailed indices.
     */
    private final Indices indices = new Indices();

    /**
     * @param bundleRoot The root directory of the bundle we're compiling. This should contain the bundle.toml file.
     *                   If this is null, no index files will be created or read, and the compiler will be unable
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
        File[] files = bundleRoot.resolve(folderPath.toPath()).toFile().listFiles();
        if (files == null)
            return paths;
        for (File file : files) {
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
            Messenger<TokenStream> tokens = new Lexer().process(fileContents);
            return tokens.map(t -> new Parser(filePath).parse(t)).getValue().orElse(null);
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
                try {
                    return new SymbolMap(script);
                } catch (Exception e) {
                    QssLogger.logger.atSevere().withCause(e).log("Symbol map could not be generated");
                    throw e;
                }
            });
        } catch (ExecutionException | UncheckedExecutionException e) {
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

    /**
     * Executes the given function on each of the scripts in this package.
     */
    private void forScriptsIn(ScriptPath thePackage, Consumer<Script> func) {
        for (ScriptPath folderChild : getFolderChildren(thePackage)) {
            Script parsed = getParsed(folderChild);
            if (parsed != null) {
                func.accept(parsed);
            }
        }
    }

    public NameIndices getNameIndices() {
        return typeNameIndices;
    }

    public Indices getIndices() {
        return indices;
    }

    public Messenger<Script> compile(ScriptPath filePath) {
        String fileContents = getFileContent(filePath);
        Messenger<TokenStream> tokens = new Lexer().process(fileContents);
        Messenger<Script> script = tokens.map(t -> new Parser(filePath).parse(t));

        if (script.getValue().isEmpty()) {
            return script;
        } else {
            ArrayList<Message> allMessages = new ArrayList<>(script.getMessages());

            Script scriptParsed = script.getValue().get();
            deleteCachedContent(filePath);
            parsedFiles.put(filePath, scriptParsed);

            // Now, parse the bundle.toml file at the bundle root.
            Messenger<TomlTable> bundleFile = getBundleFile();
            allMessages.addAll(bundleFile.getMessages());

            // This maps bundle names onto the path containing the bundle root.
            Map<String, ScriptPath> dependencies = new HashMap<>();
            bundleFile.getValue().ifPresent(table -> {
                // Scan the "dependencies" key in bundle.toml for dependencies.
                Object o = table.get("dependencies");
                if (o instanceof TomlTable) {
                    TomlTable dependenciesTable = (TomlTable) o;
                    dependenciesTable.forEach((bundleName, value) -> {
                        if (value instanceof TomlTable) {
                            TomlTable bundleInfo = (TomlTable) value;
                            if (bundleInfo.get("path") instanceof String) {
                                ScriptPath dependencyBundlePath = new ScriptPath((String) bundleInfo.get("path"));
                                if (bundleRoot.resolve(dependencyBundlePath.toPath()).resolve("bundle.toml").toFile().isFile()) {
                                    dependencies.put(bundleName, dependencyBundlePath);
                                } else {
                                    allMessages.add(new Message(
                                            new Range(new Position(0, 0)),
                                            Message.MessageSeverity.ERROR,
                                            "Bundle " + bundleName + " in bundle.toml had an invalid \"path\" key; " + dependencyBundlePath + " was not a directory containing a Quest Sage bundle"
                                    ));
                                }
                            } else {
                                allMessages.add(new Message(
                                        new Range(new Position(0, 0)),
                                        Message.MessageSeverity.ERROR,
                                        "Bundle " + bundleName + " in bundle.toml should have a \"path\" key that is the directory of the bundle root"
                                ));
                            }
                        } else {
                            allMessages.add(new Message(
                                    new Range(new Position(0, 0)),
                                    Message.MessageSeverity.ERROR,
                                    "Bundle " + bundleName + " in bundle.toml should be a table containing the \"path\" key"
                            ));
                        }
                    });
                }
            });

            // Fill the index with each script in the package, making sure to do this script last.
            // If it's last, any name collisions will be reported in this file's error messages.
            Messenger<NameIndex> typeNameIndex = forNeighbours(filePath, scriptParsed,
                    Messenger.success(new NameIndex("bundle", scriptParsed.getPackageName())),
                    (script2, index) -> index.addFrom(script2));
            typeNameIndex.getValue().ifPresent(idx -> typeNameIndices
                    .computeIfAbsent("bundle", new ScriptPath())
                    .put(scriptParsed.getPackageName(), idx));

            // If there were no errors up to this point, we're OK to generate the index for the package.
            if (typeNameIndex.hasErrors()) {
                allMessages.addAll(typeNameIndex.getMessages());
                return Messenger.success(scriptParsed, allMessages);
            }

            // First, let's make sure the index is filled with all the other packages in this bundle and other
            // dependency bundles.
            for (QualifiedName packageName : getPackagesInBundle(bundleRoot.resolve("src"))) {
                typeNameIndices
                        .computeIfAbsent("bundle", new ScriptPath())
                        .computeIfAbsent(packageName, k -> {
                            NameIndex index = new NameIndex("bundle", k);
                            forScriptsIn(new ScriptPath(Paths.get("src").resolve(k.toPath())), index::addFrom);
                            return index;
                        });
            }

            dependencies.forEach((dependencyBundle, dependencyBundlePath) -> {
                // Compute the indices for each dependency bundle.
                for (QualifiedName packageName : getPackagesInBundle(bundleRoot.resolve(dependencyBundlePath.toPath()).resolve("src"))) {
                    typeNameIndices
                            .computeIfAbsent(dependencyBundle, dependencyBundlePath)
                            .computeIfAbsent(packageName, k -> {
                                NameIndex index = new NameIndex(dependencyBundle, k);
                                forScriptsIn(new ScriptPath(dependencyBundlePath.toPath().resolve("src").resolve(k.toPath())), index::addFrom);
                                return index;
                            });
                }
            });

            QssLogger.logger.atInfo().log("Type Name Indices:\n%s", typeNameIndices);

            // We will go ahead and generate the index. There might be errors when we do this
            // (e.g. field of undeclared type) but we'll just generate the index anyway.
            // The addFrom method uses the typeNameIndices we just generated.
            // We'll do the same thing where we generate this script last.
            Messenger<Index> index = forNeighbours(filePath, scriptParsed,
                    typeNameIndex.map(idx -> Messenger.success(new Index(this, scriptParsed.getPackageName()))),
                    (script2, index2) -> index2.addFrom(script2));
            index.getValue().ifPresent(idx -> indices
                    .computeIfAbsent("bundle", new ScriptPath())
                    .put(scriptParsed.getPackageName(), idx));

            // Now, let's build the index for the whole bundle.
            for (QualifiedName packageName : getPackagesInBundle(bundleRoot.resolve("src"))) {
                indices
                        .computeIfAbsent("bundle", new ScriptPath())
                        .computeIfAbsent(packageName, k -> {
                            Index index2 = new Index(this, k);
                            forScriptsIn(new ScriptPath(Paths.get("src").resolve(k.toPath())), index2::addFrom);
                            return index2;
                        });
            }

            dependencies.forEach((dependencyBundle, dependencyBundlePath) -> {
                // Compute the indices for each dependency bundle.
                for (QualifiedName packageName : getPackagesInBundle(bundleRoot.resolve(dependencyBundlePath.toPath()).resolve("src"))) {
                    indices
                            .computeIfAbsent(dependencyBundle, dependencyBundlePath)
                            .computeIfAbsent(packageName, k -> {
                                Index index2 = new Index(this, k);
                                forScriptsIn(new ScriptPath(dependencyBundlePath.toPath().resolve("src").resolve(k.toPath())), index2::addFrom);
                                return index2;
                            });
                }
            });

            QssLogger.logger.atInfo().log("Indices:\n%s", indices);

            allMessages.addAll(index.getMessages());

            // Now that all the indices have been created, we can start deducing the types of everything inside
            // function bodies.
            TypeDeducer typeDeducer = new TypeDeducer(this, scriptParsed, filePath);
            for (Documentable<Func> func : scriptParsed.getFuncs()) {
                typeDeducer.computeTypesIn(func.getContent(), allMessages);
            }

            // Return the parsed script.
            return Messenger.success(scriptParsed, allMessages);
        }
    }

    /**
     * Recursively finds the names of all the packages in the bundle.
     * @param srcRoot The ABSOLUTE (not relative) root directory of the QSS source in the bundle.
     */
    private ArrayList<QualifiedName> getPackagesInBundle(Path srcRoot) {
        ArrayList<QualifiedName> packages = new ArrayList<>();
        File[] files = srcRoot.toFile().listFiles();
        if (files == null)
            return packages;
        for (File file : files) {
            if (file.isDirectory()) {
                String name = file.getName();
                if (name.startsWith("."))
                    continue;
                packages.add(new QualifiedName(name));
                for (QualifiedName qualifiedName : getPackagesInBundle(file.toPath())) {
                    packages.add(qualifiedName.prependSegment(name));
                }
            }
        }
        return packages;
    }

    /**
     * If null, it will be retrieved when getBundleFile is called.
     */
    private Messenger<TomlTable> bundleFile;

    private String bundleFileContents = null;

    /**
     * Call this when the bundle.toml file is changed.
     */
    public void overwriteBundleFileContents(String contents) {
        bundleFile = null;
        bundleFileContents = contents;
    }

    private Messenger<TomlTable> getBundleFile() {
        if (bundleFile != null)
            return bundleFile;

        if (bundleFileContents == null) {
            Path tomlFile = bundleRoot.resolve("bundle.toml");
            if (tomlFile.toFile().isFile()) {
                try {
                    bundleFileContents = Files.readString(tomlFile);
                } catch (IOException e) {
                    return Messenger.fail(new ArrayList<>(List.of(new Message(
                            new Range(new Position(0, 0)),
                            Message.MessageSeverity.ERROR,
                            "Cannot open bundle.toml file"
                    ).setSource("qss-bundle"))));
                }
            } else {
                return Messenger.fail(new ArrayList<>(List.of(new Message(
                        new Range(new Position(0, 0)),
                        Message.MessageSeverity.ERROR,
                        "Cannot find bundle.toml file; this file should be at the bundle root"
                ).setSource("qss-bundle"))));
            }
        }

        try {
            TomlTable tomlTable = Toml.from(new StringReader(bundleFileContents));
            // TODO maybe do some validation here?
            bundleFile = Messenger.success(tomlTable);
            return bundleFile;
        } catch (IOException e) {
            return Messenger.fail(new ArrayList<>(List.of(new Message(
                    new Range(new Position(0, 0)),
                    Message.MessageSeverity.ERROR,
                    "Cannot parse bundle.toml file"
            ).setSource("qss-bundle"))));
        }
    }
}
