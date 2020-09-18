package com.thirds.qss.compiler.tree;

import com.thirds.qss.compiler.Range;

import java.util.ArrayList;

public class Script extends Node {
    private final ArrayList<Struct> structs;

    public Script(Range range, ArrayList<Struct> structs) {
        super(range);
        this.structs = structs;
    }

    @Override
    public String toString() {
        return "Script{" +
                "structs=" + structs +
                '}';
    }

    public ArrayList<Struct> getStructs() {
        return structs;
    }
}
