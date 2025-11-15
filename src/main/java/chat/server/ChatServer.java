package chat.server;

import chat.protocol.ClientSession;
import chat.protocol.Protocol;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ChatServer {
    private final int port;
    private final InMemoryBackend backend = new InMemoryBackend();
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    private final CountDownLatch ready = new CountDownLatch(1);

    public ChatServer(int port) {
        this.port = port;
    }

    public Thread startAsync() {
        running = true;
        Thread t = new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(port)) {
                serverSocket = ss;
                ready.countDown();
                while (running) {
                    Socket s = ss.accept();
                    new Thread(() -> handle(s), "client-" + s.getPort()).start();
                }
            } catch (IOException e) {
                ready.countDown();
                if (running) e.printStackTrace();
            }
        }, "chat/server");
        t.setDaemon(true);
        t.start();
        return t;
    }

    public boolean awaitReady(long ms) throws InterruptedException {
        return ready.await(ms, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
    }

    private void handle(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
             );
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                     true)
        ) {
            ClientSession session = new ClientSession(backend);
            String line;
            while ((line = in.readLine()) != null) {
                String resp = session.process(line);
                if (resp != null) {
                    out.println(resp);
                    if (Protocol.WELCOME.equals(resp) && session.nick() != null) {
                        backend.attachWriter(session.nick(), out);
                        backend.broadcastUsersList();
                    }
                } else {
                    if (session.nick() != null) {
                        backend.attachWriter(session.nick(), out);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    public static void main(String[] args) throws Exception {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : 5000;
        ChatServer server = new ChatServer(port);
        Thread t = server.startAsync();
        if (!server.awaitReady(5000)) {
            System.err.println("Server did not start within 5s.");
            System.exit(1);
        }
        System.out.println("Server started on port " + port + ". Press Ctrl+C to stop.");
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "shutdown"));
        t.join();
    }
}
