package com.thirds.qss.compiler;

import java.util.Objects;

public class Message {
    public Message(Range range, MessageSeverity severity, String message) {
        this.range = range;
        this.severity = severity;
        this.message = message;
    }

    public enum MessageSeverity {
        WARNING,
        ERROR
    }

    /**
     * Where does the message originate from?
     */
    public final Range range;

    public final MessageSeverity severity;

    public final String message;

    @Override
    public String toString() {
        return severity + " (" + range + "): " + message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message1 = (Message) o;
        return range.equals(message1.range) &&
                severity == message1.severity &&
                message.equals(message1.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(range, severity, message);
    }
}
