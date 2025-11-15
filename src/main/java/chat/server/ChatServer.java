package chat.server;

import chat.protocol.ClientSession;
import chat.protocol.Protocol;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Minimal multi-client TCP chat server.
 * <p>
 * The server accepts plain-text connections, delegates per-connection protocol parsing
 * to {@link ClientSession}, and uses an in-memory backend for message routing.
 * Each client is handled on a dedicated thread. This implementation is intended for
 * demos and tests and is not optimized for production use.
 * </p>
 */
public class ChatServer {
    private final int port;
    private final InMemoryBackend backend = new InMemoryBackend();
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    private final CountDownLatch ready = new CountDownLatch(1);

    /**
     * Creates a server that will listen on the given TCP port.
     *
     * @param port TCP port to bind to
     */
    public ChatServer(int port) {
        this.port = port;
    }

    /**
     * Starts the server on a background daemon thread.
     *
     * @return the thread that runs the accept loop
     */
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

    /**
     * Waits until the server has successfully bound the port.
     *
     * @param ms maximum time to wait in milliseconds
     * @return true if the server became ready within the given time, false otherwise
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public boolean isReady(long ms) throws InterruptedException {
        return !ready.await(ms, TimeUnit.MILLISECONDS);
    }

    /**
     * Requests the server to stop and closes the server socket if open.
     * The accept loop thread will exit shortly after.
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * Handles a single connected client until the socket is closed.
     */
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

    /**
     * Starts the server from the command line.
     *
     * @param args first argument may specify the port (default 5000)
     * @throws Exception if the server thread is interrupted
     */
    public static void main(String[] args) throws Exception {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : 5000;
        ChatServer server = new ChatServer(port);
        Thread t = server.startAsync();
        if (server.isReady(5000)) {
            System.err.println("Server did not start within 5s.");
            System.exit(1);
        }
        System.out.println("Server started on port " + port + ". Press Ctrl+C to stop.");
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "shutdown"));
        t.join();
    }
}
