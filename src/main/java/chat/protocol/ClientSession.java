package chat.protocol;

/**
 * Stateful protocol handler for a single client connection.
 * <p>
 * Instances of this class are not thread-safe and are expected to be used from a single
 * connection-handling thread. The session tracks the authenticated nickname after a successful
 * {@link Protocol#HANDSHAKE} and routes subsequent commands to the provided {@link Backend}.
 * </p>
 */
public class ClientSession {
    private final Backend backend;
    private String nick;

    /**
     * Creates a new session bound to a backend implementation.
     *
     * @param backend the backend responsible for nickname reservation and message delivery
     */
    public ClientSession(Backend backend) {
        this.backend = backend;
    }

    /**
     * Processes a single incoming line according to the {@link Protocol}.
     *
     * @param line a non-null UTF-8 line received from the client (without line breaks)
     * @return a response to send back to the client or {@code null} if no immediate
     * response is required (e.g., for broadcasted messages). Never throws; unknown input
     * results in {@link Protocol#ERROR_UNKNOWN}.
     */
    public String process(String line) {
        if (line == null) return null;

        if (nick == null) {
            if (!line.startsWith(Protocol.HANDSHAKE)) return Protocol.ERR_NOT_LOGGED_IN;
            String candidate = line.substring(Protocol.HANDSHAKE.length()).trim();
            if (candidate.isEmpty() || candidate.contains(" ") || candidate.length() > Protocol.MAX_NICK_LENGTH) {
                return Protocol.ERR_INVALID_NICK;
            }
            if (backend.reserveNick(candidate)) {
                nick = candidate;
                return Protocol.WELCOME;
            } else {
                return Protocol.ERR_NICK_TAKEN;
            }
        }

        if (line.startsWith(Protocol.MSG)) {
            String text = line.substring(Protocol.MSG.length()).trim();
            backend.broadcast(nick, text);
            return null;
        }

        if (line.startsWith(Protocol.PRIV)) {
            String rest = line.substring(Protocol.PRIV.length()).trim();
            String[] parts = rest.split(" ", 2);
            if (parts.length < 2) {
                return Protocol.ERR_INVALID_MSG;
            }
            String target = parts[0].trim();
            String text = parts[1].trim();
            boolean ok = backend.sendPrivate(nick, target, text);
            if (ok) {
                return null;
            } else {
                return Protocol.ERR_USER_NOT_FOUND;
            }
        }

        if (line.startsWith(Protocol.LIST_USERS)) {
            return Protocol.LIST_USERS + backend.usersCsv();
        }

        if (line.startsWith(Protocol.QUIT)) {
            backend.releaseNick(nick);
            nick = null;
            return null;
        }

        return Protocol.ERROR_UNKNOWN;
    }

    /**
     * Returns the currently authenticated nickname for this session.
     *
     * @return the nickname, or {@code null} if the user has not logged in or has quit
     */
    public String nick() {
        return nick;
    }
}
