package chat.protocol;

public interface Backend {
    boolean reserveNick(String nick);
    void releaseNick(String nick);
    void broadcast(String fromNick, String text);
    /**
     * @return true if the receiver exists
     */
    boolean sendPrivate(String fromNick, String toNick, String text);
    /**
    * ex. "alice,bob"
    */
    String usersCsv();
}
