package com.thirds.qss.compiler.tree.script;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.tree.Node;

public class Action extends Node {
    private final Struct struct;
    private final Func func;

    public Action(Struct struct, Func func) {
        super(Range.combine(struct.getRange(), func.getRange()));
        this.struct = struct;
        this.func = func;
    }

    public Struct getStruct() {
        return struct;
    }

    public Func getFunc() {
        return func;
    }
}
