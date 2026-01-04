package application.network;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NetworkMessageTest {

    @Test
    void encodeParse_roundTrip() {
        NetworkMessage msg = new NetworkMessage(NetworkMessageType.HELLO, 1, "Alice", "123");
        String encoded = msg.encode();
        NetworkMessage parsed = NetworkMessage.parse(encoded);
        assertNotNull(parsed);
        assertEquals(msg, parsed);
    }

    @Test
    void parse_invalid_returnsNull() {
        assertNull(NetworkMessage.parse(null));
        assertNull(NetworkMessage.parse(""));
        assertNull(NetworkMessage.parse("BAD"));
        assertNull(NetworkMessage.parse("UNKNOWN|1|x|y"));
    }
}


