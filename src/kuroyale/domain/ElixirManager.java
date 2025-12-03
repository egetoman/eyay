package kuroyale.domain;

/**
 * Tracks elixir regeneration and spending for a player.
 */
public class ElixirManager {

    private static final int MAX_ELIXIR = 10;
    private static final double NORMAL_INTERVAL_SECONDS = 2.8;
    private static final double DOUBLE_INTERVAL_SECONDS = 1.4;

    private int current = 5;
    private double accumulatorSeconds = 0;
    private boolean doubleElixirActive = false;

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = Math.max(0, Math.min(current, MAX_ELIXIR));
    }

    public boolean isDoubleElixirActive() {
        return doubleElixirActive;
    }

    public void activateDoubleElixir() {
        this.doubleElixirActive = true;
    }

    public void deactivateDoubleElixir() {
        this.doubleElixirActive = false;
    }

    public boolean canSpend(int cost) {
        return current >= cost;
    }

    public boolean spend(int cost) {
        if (!canSpend(cost)) {
            return false;
        }
        current -= cost;
        return true;
    }

    public void tick(double deltaSeconds) {
        double interval = doubleElixirActive ? DOUBLE_INTERVAL_SECONDS : NORMAL_INTERVAL_SECONDS;
        accumulatorSeconds += deltaSeconds;
        while (accumulatorSeconds >= interval && current < MAX_ELIXIR) {
            current += 1;
            accumulatorSeconds -= interval;
        }
        if (current > MAX_ELIXIR) {
            current = MAX_ELIXIR;
        }
    }
}
