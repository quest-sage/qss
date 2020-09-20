package com.thirds.qss.compiler.indexer;

import com.thirds.qss.QualifiedName;
import com.thirds.qss.compiler.ScriptPath;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents a cached version of the indices for all loaded packages.
 */
public class Indices {
    public Bundle computeIfAbsent(String bundle, ScriptPath pathToBundleRoot) {
        return bundles.computeIfAbsent(bundle, k -> new Bundle(pathToBundleRoot));
    }

    /**
     * Returns null if no named bundle exists in this index.
     */
    public Bundle get(String bundle) {
        return bundles.get(bundle);
    }

    public Map<String, Bundle> getBundles() {
        return bundles;
    }

    /**
     * The bundle of name "bundle" is the resource bundle we're currently compiling.
     * This is like the crate of name "crate" in Rust.
     */
    public static class Bundle {
        private final ScriptPath pathToBundleRoot;

        /**
         * Maps package names -> corresponding indices.
         */
        private final Map<QualifiedName, Index> packages = new HashMap<>();

        public Bundle(ScriptPath pathToBundleRoot) {
            this.pathToBundleRoot = pathToBundleRoot;
        }

        public void remove(QualifiedName packageName) {
            packages.remove(packageName);
        }

        public Index put(QualifiedName packageName, Index index) {
            return packages.put(packageName, index);
        }

        public Index computeIfAbsent(QualifiedName packageName, Function<QualifiedName, Index> func) {
            return packages.computeIfAbsent(packageName, func);
        }

        public Map<QualifiedName, Index> getPackages() {
            return packages;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            packages.forEach((p, i) -> sb.append("  ").append(p).append(": ").append(i).append("\n"));
            return sb.toString();
        }
    }

    /**
     * Maps bundle names -> bundle indices.
     */
    private final Map<String, Bundle> bundles = new HashMap<>();

    public void addBundle(String bundleName, Bundle bundle) {
        bundles.put(bundleName, bundle);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        bundles.forEach((s, b) -> sb.append(s).append(":\n").append(b).append("\n"));
        return sb.toString();
    }
}
