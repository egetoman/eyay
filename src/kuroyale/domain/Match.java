package kuroyale.domain;

public class Match {

    private Player player;
    private Player opponent;
    private Arena arena;

    public Match() {
    }

    public Match(Player player, Player opponent, Arena arena) {
        this.player = player;
        this.opponent = opponent;
        this.arena = arena;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getOpponent() {
        return opponent;
    }

    public void setOpponent(Player opponent) {
        this.opponent = opponent;
    }

    public Arena getArena() {
        return arena;
    }

    public void setArena(Arena arena) {
        this.arena = arena;
    }

    public void start() {
        // TODO: implement match starting behavior
    }

    public void end() {
        // TODO: implement match ending behavior
    }
}




