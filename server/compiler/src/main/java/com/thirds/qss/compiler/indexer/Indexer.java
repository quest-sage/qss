package com.thirds.qss.compiler.indexer;

import com.thirds.qss.QualifiedName;
import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.Messenger;
import com.thirds.qss.compiler.tree.Script;

public class Indexer {
    private final Compiler compiler;

    public Indexer(Compiler compiler) {
        this.compiler = compiler;
    }

    public Messenger<Object> addFrom(Script script) {
        return new TypeNameIndex(new QualifiedName()).addFrom(script);
    }
}
