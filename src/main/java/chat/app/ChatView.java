package chat.app;

import java.util.List;

/**
 * UI/frontend contract for receiving events from the chat client.
 * <p>
 * This interface contains no Swing-specific types, so it can be reused across different
 * frontends (e.g., CLI, web, or desktop UIs). All callbacks are expected to be invoked
 * on the UI thread of the respective frontend (Swing EDT for the default desktop app).
 * </p>
 */
public interface ChatView {
    /**
     * Notifies that the client has successfully connected and logged in.
     *
     * @param nick the nickname reserved for this session
     */
    void onConnected(String nick);

    /**
     * Delivers a public message that was broadcast to everyone.
     *
     * @param from sender nickname
     * @param text message text
     */
    void onPublicMessage(String from, String text);

    /**
     * Delivers a private (direct) message.
     *
     * @param from sender nickname
     * @param to recipient nickname
     * @param text message text
     */
    void onPrivateMessage(String from, String to, String text);

    /**
     * Provides the current list of users connected to the server.
     *
     * @param users list of nicknames (may be empty but never null)
     */
    void onUsers(List<String> users);

    /**
     * Shows an informational/system message that does not originate from a user.
     *
     * @param text human-readable message
     */
    void onSystemMessage(String text);

    /**
     * Reports an error message emitted by the server or client.
     *
     * @param error human-readable error description
     */
    void onError(String error);

    /**
     * Notifies that the underlying connection has been closed or lost.
     */
    void onDisconnected();
}
