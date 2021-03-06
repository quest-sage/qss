package com.thirds.qss.compiler;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Frontend-agnostic equivalent of LSP's Diagnostic class.
 */
public class Message {
    public final ArrayList<MessageRelatedInformation> infos = new ArrayList<>();
    private String source = "qss";

    /**
     * Represents a message that will be shown to the user. This represents some kind of diagnostic information,
     * usually an error or warning.
     * @param range Where did the error come from? Keep this range as small as possible to avoid extraneous underlining
     *              in IDEs.
     * @param severity How severe is this message? Use {@link MessageSeverity#ERROR} for messages that mean it is
     *                 impossible to compile. Use {@link MessageSeverity#WARNING} for code that is probably
     *                 unintentional. Use {@link MessageSeverity#INFORMATION} for basic lint messages.
     * @param message The message to show to the user. It should start with a capital letter, and not have any
     *                trailing punctuation. If the message is comprised of multiple sentences, you may use punctuation
     *                to separate the sentences (e.g. full stops/periods) but do not put punctuation right at the end.
     *                If the message has an "expected" part and an "actual" part, write the message in the format
     *                "Expected ..., got ...". If the message has an "expected" type and an "actual" type, write
     *                the message in the format "Expected an expression of type ..., got ...".
     */
    public Message(Range range, MessageSeverity severity, String message) {
        this.range = range;
        this.severity = severity;
        this.message = message;
    }

    /**
     * @return <code>this</code> for chaining.
     */
    public Message addInfo(MessageRelatedInformation info) {
        infos.add(info);
        return this;
    }

    /**
     * @return <code>this</code> for chaining.
     */
    public Message setSource(String source) {
        this.source = source;
        return this;
    }

    /**
     * Represents a related location to a message. Useful for linking to places that may have caused an error, e.g.
     * name clashes.
     */
    public static class MessageRelatedInformation {
        public final Location location;
        public final String message;

        public MessageRelatedInformation(Location location, String message) {
            this.location = location;
            this.message = message;
        }
    }

    public enum MessageSeverity {
        HINT,
        INFORMATION,
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
