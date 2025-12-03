package kuroyale.domain;

/**
 * Simple match timer that can be paused/resumed.
 */
public class GameTimer {

    private final double totalSeconds;
    private double remainingSeconds;
    private boolean running;

    public GameTimer() {
        this(180); // 3 minutes default
    }

    public GameTimer(double totalSeconds) {
        this.totalSeconds = totalSeconds;
        this.remainingSeconds = totalSeconds;
    }

    public double getRemainingSeconds() {
        return remainingSeconds;
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        running = true;
    }

    public void pause() {
        running = false;
    }

    public void resume() {
        running = true;
    }

    public void reset() {
        remainingSeconds = totalSeconds;
        running = false;
    }

    public void tick(double deltaSeconds) {
        if (!running) {
            return;
        }
        remainingSeconds -= deltaSeconds;
        if (remainingSeconds < 0) {
            remainingSeconds = 0;
            running = false;
        }
    }

    public boolean isDoubleElixirPhase() {
        // Final minute is double elixir: when elapsed >= (total - 60)
        double elapsed = totalSeconds - remainingSeconds;
        return elapsed >= (totalSeconds - 60);
    }

    public boolean isExpired() {
        return remainingSeconds <= 0;
    }
}
