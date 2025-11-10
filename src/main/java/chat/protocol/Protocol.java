package chat.protocol;

public interface Protocol {
    String ERR_INVALID_NICK = "ERROR Invalid nick";
    String ERR_NICK_TAKEN = "ERROR Nick taken";
    String ERR_NOT_LOGGED_IN = "ERROR Not logged in";
    String ERROR_UNKNOWN = "ERROR Unknown command";

    String HANDSHAKE = "HELLO ";
    String MSG = "MSG ";
    String WELCOME = "WELCOME";

    int MAX_NICK_LENGTH = 20;

}
