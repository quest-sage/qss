package com.thirds.qss.compiler;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Encapsulates a variable, but may also carry errors and other messages.
 * This is often returned by functions that may emit errors.
 *
 * The method {@link #map} allows you to use the variable inside the messenger if the prior computation succeeded.
 */
public class Messenger<T> {
    T value;
    private final ArrayList<Message> messages;

    Messenger(T value, ArrayList<Message> messages) {
        this.value = value;
        this.messages = messages;
    }

    public static <T> Messenger<T> success(T value) {
        return success(value, new ArrayList<>(0));
    }

    public static <T> Messenger<T> success(T value, ArrayList<Message> messages) {
        if (value == null)
            throw new NullPointerException();
        return new Messenger<>(value, messages);
    }

    public static <T> Messenger<T> fail(ArrayList<Message> messages) {
        return new Messenger<>(null, messages);
    }

    /**
     * If this messenger was a <code>success</code>, the given function is applied to the result.
     * The messages from this messenger and from the resultant messenger are combined to form the returned message.
     */
    public <U> Messenger<U> map(Function<T, Messenger<U>> func) {
        if (value != null) {
            Messenger<U> otherMessenger = func.apply(value);

            ArrayList<Message> newMessages = new ArrayList<>();
            newMessages.addAll(messages);
            newMessages.addAll(otherMessenger.messages);

            if (otherMessenger.value != null) {
                return success(otherMessenger.value, newMessages);
            } else {
                return fail(newMessages);
            }
        } else {
            return fail(messages);
        }
    }

    /**
     * If this messenger was a <code>success</code>, the given supplier is evaluated.
     * The messages from this messenger and from the resultant messenger are combined to form the returned message.
     */
    public <U> Messenger<U> then(Supplier<Messenger<U>> func) {
        return map(m -> func.get());
    }

    /**
     * Use {@link #map} if possible; this is a less useful method.
     *
     * @return If the computation represented by this messenger failed, this will return an empty optional.
     */
    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    @Override
    public String toString() {
        if (messages.isEmpty())
            return value.toString();
        Stream<Message> stream = messages.stream().skip(1);
        return value + " (messages: " + messages.get(0) + stream.map(m -> "\n" + m.toString()).collect(Collectors.joining()) + ")";
    }

    public boolean hasErrors() {
        for (Message message : messages) {
            if (message.severity == Message.MessageSeverity.ERROR)
                return true;
        }
        return false;
    }
}
