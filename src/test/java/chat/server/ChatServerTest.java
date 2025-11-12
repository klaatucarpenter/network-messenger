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
        server.awaitReady(2000);
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
            assertEquals(Protocol.WELCOME, inA.readLine());

            outB.println(Protocol.HANDSHAKE + "bob");
            assertEquals(Protocol.WELCOME, inB.readLine());

            outA.println(Protocol.MSG + "hello from alice");

            String lineB = inB.readLine();
            assertEquals(Protocol.FROM + "alice hello from alice", lineB);
            String lineA = inA.readLine();
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
            assertEquals(Protocol.WELCOME, inA.readLine());
            outB.println(Protocol.HANDSHAKE + "bob");
            assertEquals(Protocol.WELCOME, inB.readLine());
            outC.println(Protocol.HANDSHAKE + "charlie");
            assertEquals(Protocol.WELCOME, inC.readLine());

            outA.println(Protocol.PRIV + "bob hi bob");
            String lineB = inB.readLine();
            assertEquals(Protocol.PRIV_FROM + "alice hi bob", lineB);
            assertThrows(java.net.SocketTimeoutException.class, () -> {
                String unexpected = inC.readLine();
                System.out.println("Unexpected for C: " + unexpected);
            });
        }
    }
}
