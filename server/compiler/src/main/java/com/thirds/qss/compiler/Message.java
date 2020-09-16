package com.thirds.qss.compiler;

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
}
