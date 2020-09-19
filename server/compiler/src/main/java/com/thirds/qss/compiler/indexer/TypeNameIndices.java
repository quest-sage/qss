package com.thirds.qss.compiler.indexer;

import com.thirds.qss.QualifiedName;
import com.thirds.qss.compiler.ScriptPath;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents a cached version of the type name indices for all loaded packages.
 */
public class TypeNameIndices {
    public Bundle computeIfAbsent(String bundle, ScriptPath pathToBundleRoot) {
        return bundles.computeIfAbsent(bundle, k -> new Bundle(pathToBundleRoot));
    }

    /**
     * Returns null if no named bundle exists in this index.
     */
    public Bundle get(String bundle) {
        return bundles.get(bundle);
    }

    /**
     * The bundle of name "bundle" is the resource bundle we're currently compiling.
     * This is like the crate of name "crate" in Rust.
     */
    public static class Bundle {
        private final ScriptPath pathToBundleRoot;

        /**
         * Maps package names -> corresponding type name indices.
         */
        private final Map<QualifiedName, TypeNameIndex> packages = new HashMap<>();

        public Bundle(ScriptPath pathToBundleRoot) {
            this.pathToBundleRoot = pathToBundleRoot;
        }

        public void addPackage(QualifiedName bundle, TypeNameIndex index) {
            packages.put(bundle, index);
        }

        public void remove(QualifiedName packageName) {
            packages.remove(packageName);
        }

        public TypeNameIndex put(QualifiedName packageName, TypeNameIndex index) {
            return packages.put(packageName, index);
        }

        public TypeNameIndex computeIfAbsent(QualifiedName packageName, Function<QualifiedName, TypeNameIndex> func) {
            return packages.computeIfAbsent(packageName, func);
        }
    }

    /**
     * Maps bundle names -> bundle indices.
     */
    private final Map<String, Bundle> bundles = new HashMap<>();

    public void addBundle(String bundleName, Bundle bundle) {
        bundles.put(bundleName, bundle);
    }
}
