package application.network;

import java.util.ArrayList;
import java.util.List;
import kuroyale.domain.Arena;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.Card;
import kuroyale.domain.Deck;
import kuroyale.domain.Match;
import kuroyale.domain.Player;
import kuroyale.domain.Position;
import kuroyale.infrastructure.CardCatalogRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NetworkMatchControllerTest {

    @Test
    void requestDeploy_rejectsCardNotInAllowedDeck() {
        FakeAdapter adapter = new FakeAdapter();
        NetworkMatchController controller = new NetworkMatchController(adapter, NetworkConfig.defaults(), true, 1);

        Card knight = find("card_knight");
        assertNotNull(knight);

        // Only allow archers
        controller.setAllowedDeckIds(1, List.of("card_archers"));

        var result = controller.requestDeploy(knight, new Position(0, 0));
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    void host_appliesOwnDeployWhenAllowed() {
        FakeAdapter adapter = new FakeAdapter();
        NetworkMatchController controller = new NetworkMatchController(adapter, NetworkConfig.defaults(), true, 1);

        Card knight = find("card_knight");
        assertNotNull(knight);

        controller.setAllowedDeckIds(1, List.of("card_knight"));

        Deck d1 = new Deck(List.of(knight, knight, knight, knight, knight, knight, knight, knight));
        Deck d2 = new Deck(List.of(knight, knight, knight, knight, knight, knight, knight, knight));
        Player p1 = new Player("P1", d1, 0);
        Player p2 = new Player("P2", d2, 0);
        Arena arena = new Arena();
        arena.loadLayout(ArenaLayout.defaultLayout());
        Match match = new Match(p1, p2, arena);
        match.setBotEnabled(false);
        controller.attachHostMatch(match, ArenaLayout.defaultLayout());

        int before = match.getArena().getUnits().size();
        var result = controller.requestDeploy(knight, new Position(0, 0));
        assertTrue(result.isSuccess());
        int after = match.getArena().getUnits().size();
        assertEquals(before + 1, after);
    }

    private Card find(String id) {
        for (Card c : new CardCatalogRepository().findAll()) {
            if (c != null && id.equals(c.getId())) {
                return c;
            }
        }
        return null;
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


