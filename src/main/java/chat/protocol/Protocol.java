package chat.protocol;

/**
 * Text-based wire protocol used by the chat application.
 * <p>
 * Each command is a single UTF-8 line without line breaks. Client and server exchange
 * commands defined by the constants in this interface. Implementations should treat
 * values as case-sensitive.
 * </p>
 * Typical flow:
 * <ol>
 *   <li>Client connects and sends {@link #HANDSHAKE} + nick.</li>
 *   <li>Server responds with {@link #WELCOME} or one of the {@code ERROR} constants.</li>
 *   <li>After welcome, messages can be sent using {@link #MSG} or {@link #PRIV}.</li>
 *   <li>Server broadcasts messages with {@link #FROM} and direct messages with {@link #PRIV_FROM}.</li>
 *   <li>Users list can be requested via {@link #LIST_USERS}.</li>
 *   <li>Either side closes session by sending {@link #QUIT} or closing the socket.</li>
 * </ol>
 */
public interface Protocol {
    /** Error: provided nickname is invalid (empty, contains spaces, or exceeds {@link #MAX_NICK_LENGTH}). */
    String ERR_INVALID_NICK = "ERROR Invalid nick";
    /** Error: message format is invalid. */
    String ERR_INVALID_MSG = "ERROR Invalid message";
    /** Error: chosen nickname is already reserved by another client. */
    String ERR_NICK_TAKEN = "ERROR Nick taken";
    /** Error: client attempted an action before completing the handshake. */
    String ERR_NOT_LOGGED_IN = "ERROR Not logged in";
    /** Error: command was not recognized by the server. */
    String ERROR_UNKNOWN = "ERROR Unknown command";
    /** Error: target user for a private message was not found. */
    String ERR_USER_NOT_FOUND = "ERROR User not found";

    /** Server broadcast line prefix: {@code FROM: <nick> <text>} */
    String FROM = "FROM: ";
    /** Client handshake prefix: {@code HELLO <nick>} */
    String HANDSHAKE = "HELLO ";
    /** Public message prefix: {@code MSG <text>} */
    String MSG = "MSG ";
    /** Direct message prefix: {@code PRIV <nick> <text>} */
    String PRIV = "PRIV ";
    /** Direct message server broadcast prefix: {@code PRIV FROM: <from> TO: <to> <text>} */
    String PRIV_FROM = "PRIV FROM: ";
    /** Part of {@link #PRIV_FROM} line separating recipient: {@code TO: } */
    String PRIV_TO = " TO: ";
    /** Users list command and response prefix: {@code USERS<csv>} */
    String LIST_USERS = "USERS";
    /** Quit command: {@code QUIT} */
    String QUIT = "QUIT";
    /** Server welcome response confirming successful login: {@code WELCOME} */
    String WELCOME = "WELCOME";

    /** Maximum allowed nickname length in characters. */
    int MAX_NICK_LENGTH = 20;

}
