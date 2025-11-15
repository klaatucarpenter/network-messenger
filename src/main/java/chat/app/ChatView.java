package chat.app;

import java.util.List;

/**
 * This interface contains no Swing-specific types, so it can be reused in other frontends.
 */
public interface ChatView {
    /** Called when the client successfully connected and logged in. */
    void onConnected(String nick);

    /** A public message broadcast to everyone. */
    void onPublicMessage(String from, String text);

    /** A private (direct) message. */
    void onPrivateMessage(String from, String to, String text);

    /** Called when the server sends the users list. */
    void onUsers(List<String> users);

    /** Informational/system message. */
    void onSystemMessage(String text);

    /** Error reported by the server or client. */
    void onError(String error);

    /** Connection closed or lost. */
    void onDisconnected();
}
