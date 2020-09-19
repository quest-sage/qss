package com.thirds.qss;

/**
 * Qualified name with a bundle.
 */
public class BundleQualifiedName {
    public final String bundle;
    public final QualifiedName name;

    public BundleQualifiedName(String bundle, QualifiedName name) {
        this.bundle = bundle;
        this.name = name;
    }

    public String getBundle() {
        return bundle;
    }

    public QualifiedName getName() {
        return name;
    }

    @Override
    public String toString() {
        return bundle + "$" + name;
    }
}
