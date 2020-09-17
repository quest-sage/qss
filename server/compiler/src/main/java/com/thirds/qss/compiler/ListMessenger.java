package com.thirds.qss.compiler;

import java.util.ArrayList;

public class ListMessenger<T> extends Messenger<ArrayList<T>> {
    public ListMessenger() {
        super(new ArrayList<>(), new ArrayList<>());
    }

    public void add(Messenger<T> value) {
        if (getValue().isPresent()) {
            getMessages().addAll(value.getMessages());
            if (value.getValue().isPresent()) {
                getValue().get().add(value.getValue().get());
            } else {
                this.value = null;
            }
        }
    }
}
