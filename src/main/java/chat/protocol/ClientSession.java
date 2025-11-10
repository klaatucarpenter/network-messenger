package chat.protocol;

public class ClientSession {
    private final Backend backend;
    private String nick;

    public ClientSession(Backend backend) {
        this.backend = backend;
    }

    public String process(String line) {
        if (line == null) return null;

        if (nick == null) {
            if (!line.startsWith(Protocol.HANDSHAKE)) return Protocol.ERR_NOT_LOGGED_IN;
            String trimmed = line.trim();
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

        return Protocol.ERROR_UNKNOWN;
    }

    public String nick() {
        return nick;
    }
}
