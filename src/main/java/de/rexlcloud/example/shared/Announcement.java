package de.rexlcloud.example.shared;

import java.io.Serializable;

/**
 * A message for every player of the network, sent between this plugin and the
 * <a href="https://github.com/rexlCloud/Example-paper-plugin">Paper example</a> over a message channel.
 * <p>
 * Both plugins contain this record with the same package and name: the receiving side turns the bytes back into
 * its own copy of the class. Records are a good fit, Java serialization does not compare their
 * {@code serialVersionUID}. Only JDK value types (strings, numbers, UUIDs, collections, time types) and your own
 * classes can be sent.
 *
 * @param from    name of the server or proxy that sent it, e.g. {@code Lobby-1}
 * @param sender  name of the player or console that wrote it
 * @param message the text, in MiniMessage
 */
public record Announcement(String from, String sender, String message) implements Serializable {

    /**
     * The channel name; the same on every server and proxy that wants to receive it.
     */
    public static final String CHANNEL = "example:announcement";

}
