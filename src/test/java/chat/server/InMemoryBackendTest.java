package chat.server;

import chat.protocol.Protocol;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryBackendTest {

    @Test
    void reservesAndListsUsers() {
        InMemoryBackend b = new InMemoryBackend();

        assertTrue(b.reserveNick("alice"));
        assertFalse(b.reserveNick("alice")); // already taken
        assertTrue(b.reserveNick("bob"));

        String list = b.usersCsv();
        assertTrue(list.contains("alice"));
        assertTrue(list.contains("bob"));
    }

    @Test
    void releasesNickRemovesFromList() {
        InMemoryBackend b = new InMemoryBackend();
        b.reserveNick("alice");
        b.reserveNick("bob");

        b.releaseNick("alice");
        String list = b.usersCsv();
        assertFalse(list.contains("alice"));
        assertTrue(list.contains("bob"));
    }

    @Test
    void broadcastWritesToAllAttachedWriters() {
        InMemoryBackend b = new InMemoryBackend();
        b.reserveNick("alice");
        b.reserveNick("bob");

        StringWriter wa = new StringWriter();
        StringWriter wb = new StringWriter();

        b.attachWriter("alice", new PrintWriter(wa, true));
        b.attachWriter("bob", new PrintWriter(wb, true));

        b.broadcast("alice", "hello everyone");

        assertTrue(wa.toString().contains(Protocol.FROM + "alice hello everyone"));
        assertTrue(wb.toString().contains(Protocol.FROM + "alice hello everyone"));
    }

    @Test
    void sendPrivateWritesOnlyToTargetAndOptionallyEchoesToSender() {
        InMemoryBackend b = new InMemoryBackend();
        b.reserveNick("alice");
        b.reserveNick("bob");
        StringWriter wa = new StringWriter();
        StringWriter wb = new StringWriter();
        b.attachWriter("alice", new PrintWriter(wa, true));
        b.attachWriter("bob", new PrintWriter(wb, true));

        boolean ok = b.sendPrivate("alice", "bob", "hi bob");
        assertTrue(ok);

        String outA = wa.toString();
        String outB = wb.toString();
        assertTrue(outB.contains(Protocol.PRIV_FROM + "alice" + Protocol.PRIV_TO + "bob hi bob")); // delivered to bob
        assertTrue(outA.contains(Protocol.PRIV_FROM + "alice" + Protocol.PRIV_TO + "bob hi bob")); // visible to sender too
    }

    @Test
    void sendPrivateReturnsFalseWhenUserUnknown() {
        InMemoryBackend b = new InMemoryBackend();
        b.reserveNick("alice");

        boolean ok = b.sendPrivate("alice", "missing", "hi");
        assertFalse(ok);
    }
}