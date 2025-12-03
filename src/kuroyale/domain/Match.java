package kuroyale.domain;

public class Match {

    private Player player;
    private Player opponent;
    private Arena arena;
    private GameTimer timer;
    private ElixirManager playerElixir;
    private ElixirManager opponentElixir;
    private Hand playerHand;
    private Hand opponentHand;
    private boolean paused;

    public Match() {
    }

    public Match(Player player, Player opponent, Arena arena) {
        this(player, opponent, arena, new GameTimer(), null, null);
    }

    public Match(Player player, Player opponent, Arena arena, GameTimer timer, Hand playerHand, Hand opponentHand) {
        this.player = player;
        this.opponent = opponent;
        this.arena = arena;
        this.timer = timer != null ? timer : new GameTimer();
        this.playerElixir = new ElixirManager();
        this.opponentElixir = new ElixirManager();
        this.playerHand = playerHand;
        this.opponentHand = opponentHand;
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

    public GameTimer getTimer() {
        return timer;
    }

    public void setTimer(GameTimer timer) {
        this.timer = timer;
    }

    public ElixirManager getPlayerElixir() {
        return playerElixir;
    }

    public ElixirManager getOpponentElixir() {
        return opponentElixir;
    }

    public Hand getPlayerHand() {
        return playerHand;
    }

    public void setPlayerHand(Hand playerHand) {
        this.playerHand = playerHand;
    }

    public Hand getOpponentHand() {
        return opponentHand;
    }

    public void setOpponentHand(Hand opponentHand) {
        this.opponentHand = opponentHand;
    }

    public boolean isPaused() {
        return paused;
    }

    public void pause() {
        paused = true;
        timer.pause();
    }

    public void resume() {
        paused = false;
        timer.resume();
    }

    public void start() {
        paused = false;
        if (timer != null) {
            timer.reset();
            timer.start();
        }
    }

    public void end() {
        // TODO: implement match ending behavior
    }
}
