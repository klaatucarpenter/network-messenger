package chat.server;

import chat.protocol.Backend;
import chat.protocol.Protocol;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory {@link Backend} implementation backed by concurrent maps.
 * <p>
 * This backend stores the set of reserved nicknames and the latest {@link PrintWriter}
 * associated with each connected client. It is intended for testing and local demos.
 * </p>
 */
public class InMemoryBackend implements Backend {
    private static final class Session {
        volatile PrintWriter out;
    }

    private final Map<String, Session> clients = new ConcurrentHashMap<>();

    /** {@inheritDoc} */
    @Override
    public boolean reserveNick(String nick) {
        return clients.putIfAbsent(nick, new Session()) == null;
    }

    /** {@inheritDoc} */
    @Override
    public void releaseNick(String nick) {
        clients.remove(nick);
        broadcastUsersList();
    }

    /**
     * Associates the given writer with the specified nickname so future messages can be delivered.
     *
     * @param nick the nickname for which to attach the writer
     * @param out a live {@link PrintWriter} connected to the client's socket
     */
    public void attachWriter(String nick, PrintWriter out) {
        Session s = clients.get(nick);
        if (s != null) s.out = out;
    }

    /** {@inheritDoc} */
    @Override
    public void broadcast(String fromNick, String text) {
        String line = Protocol.FROM + fromNick + " " + text;
        clients.values().forEach(s -> {
            PrintWriter w = s.out;
            if (w != null) w.println(line);
        });
    }

    /** {@inheritDoc} */
    @Override
    public boolean sendPrivate(String fromNick, String toNick, String text) {
        Session dst = clients.get(toNick);
        if (dst == null || dst.out == null) return false;
        String line = Protocol.PRIV_FROM + fromNick + Protocol.PRIV_TO + toNick + " " + text;
        dst.out.println(line);

        Session src = clients.get(fromNick);
        if (src != null && src.out != null) src.out.println(line);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String usersCsv() {
        return String.join(",", clients.keySet());
    }

    /** {@inheritDoc} */
    @Override
    public void broadcastUsersList() {
        String line = Protocol.LIST_USERS + usersCsv();
        clients.values().forEach(s -> {
            PrintWriter w = s.out;
            if (w != null) w.println(line);
        });
    }
}
