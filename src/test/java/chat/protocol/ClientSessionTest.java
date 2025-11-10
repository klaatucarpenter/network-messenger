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
        String resp = s.process("HELLO alice");

        assertEquals("WELCOME", resp);
        assertEquals("alice", s.nick());
        verify(backend).reserveNick("alice");
        verifyNoMoreInteractions(backend);
    }
}
