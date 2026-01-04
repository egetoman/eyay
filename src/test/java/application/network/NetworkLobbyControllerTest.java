package application.network;

import java.util.ArrayList;
import java.util.List;
import kuroyale.domain.Card;
import kuroyale.domain.Deck;
import kuroyale.infrastructure.CardCatalogRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NetworkLobbyControllerTest {

    @Test
    void onConnected_sendsHelloAndDeckIds() {
        FakeAdapter adapter = new FakeAdapter();
        Deck deck = buildDeck();
        NetworkLobbyController lobby = new NetworkLobbyController(adapter, NetworkConfig.defaults(), true, "Host", deck);

        lobby.onStatus(ConnectionStatus.CONNECTED, "Connected");

        assertTrue(adapter.sent.stream().anyMatch(m -> m.getType() == NetworkMessageType.HELLO));
        NetworkMessage deckMsg = adapter.sent.stream().filter(m -> m.getType() == NetworkMessageType.DECK).findFirst().orElse(null);
        assertNotNull(deckMsg);
        // DECK message should contain card ids (comma separated)
        assertTrue(deckMsg.getData().contains("card_"));
    }

    @Test
    void receivesDeckIds_mapsToNamesForUi() {
        FakeAdapter adapter = new FakeAdapter();
        Deck deck = buildDeck();
        NetworkLobbyController lobby = new NetworkLobbyController(adapter, NetworkConfig.defaults(), false, "Client", deck);

        lobby.onMessage(new NetworkMessage(NetworkMessageType.DECK, 1, "card_knight,card_archers", "t"));
        List<String> names = lobby.getState().getRemoteDeckNames();
        assertFalse(names.isEmpty());
        // We expect names, not raw ids
        assertTrue(names.stream().anyMatch(n -> n.equalsIgnoreCase("Knight")));
    }

    private Deck buildDeck() {
        List<Card> all = new CardCatalogRepository().findAll();
        List<Card> picked = new ArrayList<>();
        for (Card c : all) {
            if (picked.size() >= Deck.MAX_CARDS) break;
            picked.add(c);
        }
        return new Deck(picked);
    }

    private static final class FakeAdapter implements NetworkAdapter {
        final List<NetworkMessage> sent = new ArrayList<>();
        @Override public void host(int port, Listener listener) { }
        @Override public void connect(String host, int port, Listener listener) { }
        @Override public void setListener(Listener listener) { }
        @Override public void send(NetworkMessage message) { sent.add(message); }
        @Override public void close() { }
    }
}


