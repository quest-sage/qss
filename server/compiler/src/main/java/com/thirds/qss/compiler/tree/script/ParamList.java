package com.thirds.qss.compiler.tree.script;

import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.tree.Node;

import java.util.ArrayList;
import java.util.function.Consumer;

public class ParamList extends Node {
    private final ArrayList<Param> params;

    public ParamList(Range range, ArrayList<Param> params) {
        super(range);
        this.params = params;
    }

    public ArrayList<Param> getParams() {
        return params;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        for (Param param : params) {
            consumer.accept(param);
        }
    }
}
