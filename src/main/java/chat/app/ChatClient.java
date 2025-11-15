package chat.app;

import chat.protocol.Protocol;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Chat client logic: networking, protocol parsing, and callbacks to {@link ChatView}.
 * <p>
 * This class contains no UI state and can be reused by different frontends.
 * Network I/O happens on a background thread; all {@link ChatView} callbacks are
 * dispatched onto the Swing Event Dispatch Thread via {@link SwingUtilities#invokeLater(Runnable)}.
 * </p>
 */
public class ChatClient {
    private final ChatView view;

    private Thread listenerThread;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private volatile String nick;

    /**
     * Creates a new client bound to a {@link ChatView} implementation.
     *
     * @param view callback sink for UI/frontends; must not be {@code null}
     */
    public ChatClient(ChatView view) {
        this.view = Objects.requireNonNull(view, "view");
    }

    /**
     * Returns whether the underlying socket is connected and open.
     *
     * @return {@code true} if connected, otherwise {@code false}
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Connects to the server but does not log in.
     *
     * @param host server hostname or IP
     * @param port server TCP port
     * @throws IOException if the socket cannot be opened
     */
    public void connect(String host, int port) throws IOException {
        if (isConnected()) return;
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    /**
     * Attempts to log in with the given nickname.
     *
     * @param nick nickname to reserve; leading/trailing spaces are trimmed
     * @return {@code null} on success, or a human-readable error message to display
     */
    public String login(String nick) {
        if (out == null || in == null) return "Not connected";
        nick = nick == null ? "" : nick.trim();
        out.println(Protocol.HANDSHAKE + nick);
        String resp;
        try {
            resp = in.readLine();
        } catch (IOException e) {
            return "Connection error during login: " + e.getMessage();
        }
        if (resp == null) return "Server closed the connection.";
        if (Protocol.WELCOME.equals(resp)) {
            this.nick = nick;
            SwingUtilities.invokeLater(() -> view.onConnected(this.nick));
            startListener();
            return null;
        }
        return resp;
    }

    /**
     * Sends a public message to all connected users.
     *
     * @param text message text; blank values are ignored
     */
    public void sendPublic(String text) {
        if (!isConnected() || out == null) {
            SwingUtilities.invokeLater(() -> view.onSystemMessage("Not connected"));
            return;
        }
        if (text == null) return;
        text = text.trim();
        if (text.isEmpty()) return;
        out.println(Protocol.MSG + text);
    }

    /**
     * Sends a direct message to a single recipient.
     *
     * @param to recipient nickname; must not be blank
     * @param text message text; blank values are ignored
     */
    public void sendPrivate(String to, String text) {
        if (!isConnected() || out == null) {
            SwingUtilities.invokeLater(() -> view.onSystemMessage("Not connected"));
            return;
        }
        if (to == null || to.isBlank()) {
            SwingUtilities.invokeLater(() -> view.onSystemMessage("Usage: DM nick message"));
            return;
        }
        if (text == null || text.isBlank()) return;
        out.println(Protocol.PRIV + to + " " + text);
    }

    /**
     * Sends a quit command and closes the connection if open.
     * Always triggers {@link ChatView#onDisconnected()} on the EDT.
     */
    public void disconnect() {
        try {
            if (out != null) {
                out.println(Protocol.QUIT);
                out.flush();
            }
        } catch (Exception ignored) {
        }
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
        out = null;
        in = null;
        socket = null;
        if (listenerThread != null) listenerThread.interrupt();
        SwingUtilities.invokeLater(view::onDisconnected);
    }

    /**
     * Starts a background listener thread that reads server lines and dispatches
     * callbacks to the {@link ChatView} on the EDT.
     */
    private void startListener() {
        listenerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    final String ln = line;
                    SwingUtilities.invokeLater(() -> handleIncoming(ln));
                }
            } catch (IOException ignored) {
            } finally {
                SwingUtilities.invokeLater(() -> view.onSystemMessage("Disconnected."));
            }
        }, "chat/listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Parses a single server line and forwards it to the appropriate view callback.
     */
    private void handleIncoming(String line) {
        if (line.startsWith(Protocol.FROM)) {
            String rest = line.substring(Protocol.FROM.length());
            int sp = rest.indexOf(' ');
            if (sp > 0) {
                String from = rest.substring(0, sp);
                String msg = rest.substring(sp + 1);
                view.onPublicMessage(from, msg);
            }
        } else if (line.startsWith(Protocol.PRIV_FROM)) {
            String rest = line.substring(Protocol.PRIV_FROM.length()); // "alice TO: bob <text>"
            int toIdx = rest.indexOf(Protocol.PRIV_TO);
            if (toIdx > 0) {
                String from = rest.substring(0, toIdx).trim();
                String afterTo = rest.substring(toIdx + Protocol.PRIV_TO.length()); // "bob <text>"
                int sp2 = afterTo.indexOf(' ');
                if (sp2 > 0) {
                    String to = afterTo.substring(0, sp2).trim();
                    String msg = afterTo.substring(sp2 + 1);
                    view.onPrivateMessage(from, to, msg);
                }
            }
        } else if (line.startsWith(Protocol.LIST_USERS)) {
            String csv = line.substring(Protocol.LIST_USERS.length());
            List<String> users = new ArrayList<>();
            if (!csv.isEmpty()) {
                users.addAll(Arrays.stream(csv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList());
            }
            view.onUsers(users);
        } else if (line.startsWith("ERROR")) {
            view.onError(line);
        } else {
            view.onSystemMessage("? " + line);
        }
    }
}
