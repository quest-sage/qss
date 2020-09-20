package com.thirds.qss.compiler;

import java.util.ArrayList;

public class ListMessenger<T> extends Messenger<ArrayList<T>> {
    public ListMessenger() {
        super(new ArrayList<>(), new ArrayList<>(0));
    }

    /**
     * @param initialCapacity The initial capacity for the list of results - not the list of messages!
     */
    public ListMessenger(int initialCapacity) {
        super(new ArrayList<>(initialCapacity), new ArrayList<>(0));
    }

    public void add(Messenger<? extends T> value) {
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
