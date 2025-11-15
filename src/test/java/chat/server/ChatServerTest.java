package chat.server;

import chat.protocol.Protocol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ChatServerTest {
    private ChatServer server;
    private Thread serverThread;

    private int startServerOnFreePort() throws IOException, InterruptedException {
        int port;
        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }
        server = new ChatServer(port);
        serverThread = server.startAsync();
        if (!server.awaitReady(2000)) {
            System.err.println("Server did not start within 5s.");
            System.exit(1);
        }
        return port;
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (server != null) server.stop();
        if (serverThread != null) serverThread.join(200);
    }

    private static BufferedReader reader(Socket s) throws IOException {
        return new BufferedReader(
                new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8)
        );
    }

    private static PrintWriter writer(Socket s) throws IOException {
        return new PrintWriter(
                new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8),
                true
        );
    }

    private static String readNonUsers(BufferedReader in) throws IOException {
        for (String line; (line = in.readLine()) != null; ) {
            if (!line.startsWith(Protocol.LIST_USERS)) return line;
        }
        return null;
    }

    private static String readUsers(BufferedReader in) throws IOException {
        for (String line; (line = in.readLine()) != null; ) {
            if (line.startsWith(Protocol.LIST_USERS)) return line;
        }
        return null;
    }

    @Test
    void twoClientsLoginAndBroadcast() throws Exception {
        int port = startServerOnFreePort();

        try (Socket a = new Socket("127.0.0.1", port);
             Socket b = new Socket("127.0.0.1", port)) {

            a.setSoTimeout(300);
            b.setSoTimeout(300);

            BufferedReader inA = reader(a);
            PrintWriter outA = writer(a);
            BufferedReader inB = reader(b);
            PrintWriter outB = writer(b);

            outA.println(Protocol.HANDSHAKE + "alice");
            assertEquals(Protocol.WELCOME, readNonUsers(inA));

            outB.println(Protocol.HANDSHAKE + "bob");
            assertEquals(Protocol.WELCOME, readNonUsers(inB));

            outA.println(Protocol.MSG + "hello from alice");

            String lineB = readNonUsers(inB);
            assertEquals(Protocol.FROM + "alice hello from alice", lineB);
            String lineA = readNonUsers(inA);
            assertEquals(Protocol.FROM + "alice hello from alice", lineA);
        }
    }

    @Test
    void privateMessageDeliveredToTarget() throws Exception {
        int port = startServerOnFreePort();

        try (Socket a = new Socket("127.0.0.1", port);
             Socket b = new Socket("127.0.0.1", port);
             Socket c = new Socket("127.0.0.1", port)) {

            a.setSoTimeout(300);
            b.setSoTimeout(300);
            c.setSoTimeout(300);

            BufferedReader inA = reader(a);
            PrintWriter outA = writer(a);
            BufferedReader inB = reader(b);
            PrintWriter outB = writer(b);
            BufferedReader inC = reader(c);
            PrintWriter outC = writer(c);

            outA.println(Protocol.HANDSHAKE + "alice");
            assertEquals(Protocol.WELCOME, readNonUsers(inA));
            outB.println(Protocol.HANDSHAKE + "bob");
            assertEquals(Protocol.WELCOME, readNonUsers(inB));
            outC.println(Protocol.HANDSHAKE + "charlie");
            assertEquals(Protocol.WELCOME, readNonUsers(inC));

            outA.println(Protocol.PRIV + "bob hi bob");
            String lineB = readNonUsers(inB);
            assertEquals(Protocol.PRIV_FROM + "alice" + Protocol.PRIV_TO + "bob hi bob", lineB);
            assertThrows(java.net.SocketTimeoutException.class, () -> {
                String unexpected = readNonUsers(inC);
                System.out.println("Unexpected for C: " + unexpected);
            });
        }
    }

    @Test
    void usersListBroadcastsOnLoginAndLogout() throws Exception {
        int port = startServerOnFreePort();

        try (Socket a = new Socket("127.0.0.1", port);
             Socket b = new Socket("127.0.0.1", port)) {

            a.setSoTimeout(300);
            b.setSoTimeout(300);

            BufferedReader inA = reader(a);
            PrintWriter outA = writer(a);
            BufferedReader inB = reader(b);
            PrintWriter outB = writer(b);

            // Alice logs in
            outA.println(Protocol.HANDSHAKE + "alice");
            String usersA1 = readUsers(inA);
            assertNotNull(usersA1);
            assertTrue(usersA1.startsWith(Protocol.LIST_USERS));
            assertTrue(usersA1.contains("alice"));

            // Bob logs in, both should receive USERS with both names
            outB.println(Protocol.HANDSHAKE + "bob");

            String usersA2 = readUsers(inA);
            String usersB1 = readUsers(inB);
            assertTrue(usersA2.startsWith(Protocol.LIST_USERS));
            assertTrue(usersB1.startsWith(Protocol.LIST_USERS));
            assertTrue(usersA2.contains("alice"));
            assertTrue(usersA2.contains("bob"));
            assertTrue(usersB1.contains("alice"));
            assertTrue(usersB1.contains("bob"));

            // Bob quits, Alice should receive updated USERS without bob
            outB.println(Protocol.QUIT);
            String usersA3 = readUsers(inA);
            assertTrue(usersA3.contains("alice"));
            assertFalse(usersA3.contains("bob"));
        }
    }
}
