package chat.protocol;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientSessionTest {

    @Test
    void hello_acceptsFreeNick() {
        Backend backend = mock(Backend.class);
        when(backend.reserveNick("alice")).thenReturn(true);

        ClientSession s = new ClientSession(backend);
        String resp = s.process(Protocol.HANDSHAKE + "alice");

        assertEquals(Protocol.WELCOME, resp);
        assertEquals("alice", s.nick());
        verify(backend).reserveNick("alice");
        verifyNoMoreInteractions(backend);
    }

    @Test
    void hello_rejectsEmptyNick() {
        Backend backend = mock(Backend.class);
        ClientSession s = new ClientSession(backend);

        String resp = s.process(Protocol.HANDSHAKE);
        assertEquals(Protocol.ERR_INVALID_NICK, resp);
        verifyNoInteractions(backend);
    }

    @Test
    void hello_rejectsNickWithSpaces() {
        Backend backend = mock(Backend.class);
        ClientSession s = new ClientSession(backend);

        String resp = s.process(Protocol.HANDSHAKE + "alice bob");
        assertEquals(Protocol.ERR_INVALID_NICK, resp);
        verifyNoInteractions(backend);
    }

    @Test
    void hello_rejectsTooLongNick() {
        Backend backend = mock(Backend.class);
        ClientSession s = new ClientSession(backend);
        String longNick = "a".repeat(Protocol.MAX_NICK_LENGTH + 1);

        String resp = s.process(Protocol.HANDSHAKE + longNick);
        assertEquals(Protocol.ERR_INVALID_NICK, resp);
        verifyNoInteractions(backend);
    }

    @Test
    void hello_rejectsTakenNick() {
        Backend backend = mock(Backend.class);
        when(backend.reserveNick("alice")).thenReturn(false);

        ClientSession s = new ClientSession(backend);
        String resp = s.process(Protocol.HANDSHAKE + "alice");

        assertEquals(Protocol.ERR_NICK_TAKEN, resp);
        verify(backend).reserveNick("alice");
    }

    @Test
    void msg_broadcastsWhenLoggedIn() {
        Backend backend = mock(Backend.class);
        when(backend.reserveNick("alice")).thenReturn(true);
        ClientSession s = new ClientSession(backend);

        assertEquals(Protocol.WELCOME, s.process(Protocol.HANDSHAKE + "alice"));

        String resp = s.process(Protocol.MSG + "foo bar baz");
        assertNull(resp);
        verify(backend).broadcast("alice", "foo bar baz");
    }

    @Test
    void msg_beforeLoginIsRejected() {
        Backend backend = mock(Backend.class);
        ClientSession s = new ClientSession(backend);

        String resp = s.process(Protocol.MSG + "foo");
        assertEquals(Protocol.ERR_NOT_LOGGED_IN, resp);
    }

    @Test
    void priv_beforeLoginIsRejected() {
        Backend backend = mock(Backend.class);
        ClientSession s = new ClientSession(backend);

        String resp = s.process(Protocol.PRIV + "bob hi");
        assertEquals(Protocol.ERR_NOT_LOGGED_IN, resp);
    }

    @Test
    void priv_sendsPrivateMessageWhenLoggedIn() {
        Backend backend = mock(Backend.class);
        when(backend.reserveNick("alice")).thenReturn(true);
        when(backend.sendPrivate("alice", "bob", "hi")).thenReturn(true);

        ClientSession s = new ClientSession(backend);
        s.process(Protocol.HANDSHAKE + "alice");

        String resp = s.process(Protocol.PRIV + "bob hi");
        assertNull(resp);
        verify(backend).sendPrivate("alice", "bob", "hi");
        verify(backend).reserveNick("alice");
    }

    @Test
    void priv_reportsUnknownUser() {
        Backend backend = mock(Backend.class);
        when(backend.reserveNick("alice")).thenReturn(true);
        when(backend.sendPrivate("alice", "bob", "hi")).thenReturn(false);

        ClientSession s = new ClientSession(backend);
        s.process(Protocol.HANDSHAKE + "alice");

        String resp = s.process(Protocol.PRIV + "bob hi");
        assertEquals(Protocol.ERR_USER_NOT_FOUND, resp);
        verify(backend).sendPrivate("alice", "bob", "hi");
    }

    @Test
    void priv_reportsInvalidMessage() {
        Backend backend = mock(Backend.class);
        when(backend.reserveNick("alice")).thenReturn(true);

        ClientSession s = new ClientSession(backend);
        s.process(Protocol.HANDSHAKE + "alice");

        String resp = s.process(Protocol.PRIV + "bob ");
        assertEquals(Protocol.ERR_INVALID_MSG, resp);
    }

    @Test
    void users_beforeLoginIsRejected() {
        Backend backend = mock(Backend.class);
        ClientSession s = new ClientSession(backend);

        String resp = s.process(Protocol.LIST_USERS);
        assertEquals(Protocol.ERR_NOT_LOGGED_IN, resp);
    }

    @Test
    void users_returnsListWhenLoggedIn() {
        Backend backend = mock(Backend.class);
        when(backend.reserveNick("alice")).thenReturn(true);
        when(backend.usersCsv()).thenReturn("alice,bob,charlie");

        ClientSession s = new ClientSession(backend);
        s.process(Protocol.HANDSHAKE + "alice");

        String resp = s.process(Protocol.LIST_USERS);
        assertEquals(Protocol.LIST_USERS + "alice,bob,charlie", resp);
        verify(backend).usersCsv();
    }


}
