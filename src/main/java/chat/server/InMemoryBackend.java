package chat.server;

import chat.protocol.Backend;
import chat.protocol.Protocol;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBackend implements Backend {
    private static final class Session {
        volatile PrintWriter out;
    }

    private final Map<String, Session> clients = new ConcurrentHashMap<>();

    @Override
    public boolean reserveNick(String nick) {
        return clients.putIfAbsent(nick, new Session()) == null;
    }

    @Override
    public void releaseNick(String nick) {
        clients.remove(nick);
        broadcastUsersList();
    }

    public void attachWriter(String nick, PrintWriter out) {
        Session s = clients.get(nick);
        if (s != null) s.out = out;
    }

    @Override
    public void broadcast(String fromNick, String text) {
        String line = Protocol.FROM + fromNick + " " + text;
        clients.values().forEach(s -> {
            PrintWriter w = s.out;
            if (w != null) w.println(line);
        });
    }

    @Override
    public boolean sendPrivate(String fromNick, String toNick, String text) {
        Session dst = clients.get(toNick);
        if (dst == null || dst.out == null) return false;
        String line = Protocol.PRIV_FROM + fromNick + " " + text;
        dst.out.println(line);

        Session src = clients.get(fromNick);
        if (src != null && src.out != null) src.out.println(line);
        return true;
    }

    @Override
    public String usersCsv() {
        return String.join(",", clients.keySet());
    }

    @Override
    public void broadcastUsersList() {
        String line = Protocol.LIST_USERS + usersCsv();
        clients.values().forEach(s -> {
            PrintWriter w = s.out;
            if (w != null) w.println(line);
        });
    }
}
