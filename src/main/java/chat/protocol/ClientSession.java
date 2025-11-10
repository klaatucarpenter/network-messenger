package chat.protocol;

public class ClientSession {
    private static final String HELLO_PREFIX = "HELLO ";
    private static final int MAX_NICK_LENGTH = 20;
    private final Backend backend;
    private String nick;

    public ClientSession(Backend backend) {
        this.backend = backend;
    }

    public String process(String line) {
        if (line == null) return "ERROR";

        if (nick == null) {
            if (!line.startsWith(HELLO_PREFIX)) return "ERROR missing HELLO";
            String trimmed = line.trim();
            String candidate = line.substring(HELLO_PREFIX.length()).trim();
            if (candidate.isEmpty() || candidate.contains(" ") || candidate.length() > MAX_NICK_LENGTH) {
                return "ERROR Bad nickname";
            }
            if (backend.reserveNick(candidate)) {
                nick = candidate;
                return "WELCOME";
            } else {
                return "ERROR Nickname is already taken";
            }
        }

    return "ERROR Unknown command";
    }

    public String nick() { return nick; }
}
