package chat.protocol;

public interface Protocol {
    String ERROR_INVALID_HANDSHAKE = "ERROR Invalid handshake";
    String ERR_INVALID_NICK = "ERROR Invalid nick";
    String ERR_NICK_TAKEN = "ERROR Nick taken";
    String ERROR_UNKNOWN = "ERROR Unknown command";

    String HANDSHAKE = "HELLO ";
    String WELCOME = "WELCOME";

    int MAX_NICK_LENGTH = 20;

}
