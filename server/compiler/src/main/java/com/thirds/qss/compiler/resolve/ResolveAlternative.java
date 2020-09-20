package com.thirds.qss.compiler.resolve;

import com.thirds.qss.BundleQualifiedName;
import com.thirds.qss.VariableType;

import java.util.List;

/**
 * Represents one possible resolve of a name.
 */
public class ResolveAlternative<T> {
    /**
     * The value that this alternative will resolve to.
     */
    public final T value;
    /**
     * What imports are required for this alternative to be resolved?
     */
    public final List<BundleQualifiedName> imports;

    public ResolveAlternative(T value, List<BundleQualifiedName> imports) {
        this.value = value;
        this.imports = imports;
    }
}
