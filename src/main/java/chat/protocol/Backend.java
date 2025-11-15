package chat.protocol;

/**
 * Abstraction over the server-side storage and delivery of chat messages.
 * <p>
 * A {@code Backend} is responsible for reserving and releasing nicknames and for delivering
 * public and private messages to connected clients. Implementations may choose different
 * storage strategies; a simple in-memory implementation is provided by
 * {@code chat.server.InMemoryBackend}.
 * </p>
 */
public interface Backend {
    /**
     * Attempts to reserve a nickname for a connecting client.
     *
     * @param nick the requested nickname
     * @return true if successful, false if the nickname is already taken
     */
    boolean reserveNick(String nick);

    /**
     * Releases a previously reserved nickname. Implementations should be idempotent.
     *
     * @param nick the nickname to release
     */
    void releaseNick(String nick);

    /**
     * Broadcasts a public message to all connected clients.
     *
     * @param fromNick sender nickname
     * @param text message text
     */
    void broadcast(String fromNick, String text);

    /**
     * Sends a private message to a specific recipient.
     *
     * @param fromNick sender nickname
     * @param toNick recipient nickname
     * @param text message text
     * @return true if the receiver exists and the message was queued for delivery; false otherwise
     */
    boolean sendPrivate(String fromNick, String toNick, String text);

    /**
     * Returns the list of reserved nicknames as a comma-separated string.
     *
     * @return e.g., {@code "alice,bob"}; empty string if there are no users
     */
    String usersCsv();

    /**
     * Broadcasts the current users list to all connected clients.
     */
    void broadcastUsersList();
}
