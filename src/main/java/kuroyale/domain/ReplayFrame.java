package kuroyale.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * One snapshot in a replay timeline.
 */
public class ReplayFrame {
    private double elapsedSeconds;
    private double remainingSeconds;
    private String phase;
    private int playerElixir;
    private int opponentElixir;
    private boolean finished;
    private Integer winnerPlayerId; // 1 or 2
    private List<TowerSnapshot> towers = new ArrayList<>();
    private List<UnitSnapshot> units = new ArrayList<>();

    public ReplayFrame() {
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void setElapsedSeconds(double elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }

    public double getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(double remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public int getPlayerElixir() {
        return playerElixir;
    }

    public void setPlayerElixir(int playerElixir) {
        this.playerElixir = playerElixir;
    }

    public int getOpponentElixir() {
        return opponentElixir;
    }

    public void setOpponentElixir(int opponentElixir) {
        this.opponentElixir = opponentElixir;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public Integer getWinnerPlayerId() {
        return winnerPlayerId;
    }

    public void setWinnerPlayerId(Integer winnerPlayerId) {
        this.winnerPlayerId = winnerPlayerId;
    }

    public List<TowerSnapshot> getTowers() {
        return towers != null ? new ArrayList<>(towers) : new ArrayList<>();
    }

    public void setTowers(List<TowerSnapshot> towers) {
        this.towers = towers != null ? new ArrayList<>(towers) : new ArrayList<>();
    }

    public List<UnitSnapshot> getUnits() {
        return units != null ? new ArrayList<>(units) : new ArrayList<>();
    }

    public void setUnits(List<UnitSnapshot> units) {
        this.units = units != null ? new ArrayList<>(units) : new ArrayList<>();
    }

    public static class TowerSnapshot {
        public int owner; // 1 (player) or 2 (opponent)
        public String type;
        public int x;
        public int y;
        public int hp;
    }

    public static class UnitSnapshot {
        public String cardId;
        public String cardName;
        public int owner; // 1 or 2
        public double x;
        public double y;
        public int hp;
    }
}


