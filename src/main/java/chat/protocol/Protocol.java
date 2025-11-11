package chat.protocol;

public interface Protocol {
    String ERR_INVALID_NICK = "ERROR Invalid nick";
    String ERR_INVALID_MSG = "ERROR Invalid message";
    String ERR_NICK_TAKEN = "ERROR Nick taken";
    String ERR_NOT_LOGGED_IN = "ERROR Not logged in";
    String ERROR_UNKNOWN = "ERROR Unknown command";
    String ERR_USER_NOT_FOUND = "ERROR User not found";

    String HANDSHAKE = "HELLO ";
    String MSG = "MSG ";
    String PRIV = "PRIV ";
    String LIST_USERS = "USERS";
    String QUIT = "QUIT";
    String WELCOME = "WELCOME";

    int MAX_NICK_LENGTH = 20;

}
