package application.network;

import java.util.ArrayList;
import java.util.List;

/**
 * Host-authoritative snapshot of match state sent from Host -> Client.
 * <p>
 * Pattern note: this is a simple DTO used for serialization (Snapshot sync approach).
 */
public class NetworkSnapshot {
    public double elapsedSeconds;
    public double remainingSeconds;
    public String phase; // "DOUBLE" or "TRIPLE"
    public int player1Elixir;
    public int player2Elixir;
    public List<TowerHp> towers = new ArrayList<>();
    public List<UnitState> units = new ArrayList<>();
    public boolean finished;
    public Integer winnerPlayerId; // null => draw/unknown

    public static class TowerHp {
        public int owner; // 1 or 2
        public String type; // KING/CROWN
        public int x;
        public int y;
        public int hp;
    }

    public static class UnitState {
        public String cardId;
        public String cardName;
        public int owner; // 1 or 2
        public double x;
        public double y;
        public int hp;
    }
}


