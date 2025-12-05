package kuroyale.domain;

import kuroyale.support.Result;

public class Match {

    private static final double TOTAL_DURATION_SECONDS = 6 * 60;
    private static final double TRIPLE_ELIXIR_START_SECONDS = 5 * 60;
    private static final double SINGLE_ELIXIR_PER_SECOND = 1.0 / 2.8;

    private Player player;
    private Player opponent;
    private Arena arena;
    private double elapsedSeconds;
    private double playerElixirFraction;
    private double opponentElixirFraction;

    public Match() {
        this.arena = new Arena();
    }

    public Match(Player player, Player opponent, Arena arena) {
        this.player = player;
        this.opponent = opponent;
        this.arena = arena != null ? arena : new Arena();
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

    public Result<Unit> deployCard(Player actingPlayer, Card card, Position position) {
        if (actingPlayer == null || card == null || position == null) {
            return Result.fail("Deployment data is incomplete.");
        }
        if (!isParticipant(actingPlayer)) {
            return Result.fail("Player is not part of this match.");
        }
        if (!actingPlayer.hasEnoughElixir(card.getElixirCost())) {
            return Result.fail("Not enough elixir.");
        }
        if (!arena.isWithinBounds(position)) {
            return Result.fail("Position is outside the arena bounds.");
        }
        if (!arena.isTileFree(position)) {
            return Result.fail("Target tile is occupied.");
        }

        actingPlayer.spendElixir(card.getElixirCost());
        int hp = card.getStats() != null ? card.getStats().getHp() : 0;
        Unit unit = new Unit(card, new Position(position.getX(), position.getY()), hp);
        arena.addUnit(unit);
        return Result.ok(unit);
    }

    public void advanceTime(double deltaSeconds) {
        if (deltaSeconds <= 0) {
            return;
        }
        double targetTime = Math.min(TOTAL_DURATION_SECONDS, elapsedSeconds + deltaSeconds);
        double cursor = elapsedSeconds;
        while (cursor < targetTime) {
            double chunkBoundary = nextPhaseBoundary(cursor);
            double chunkEnd = Math.min(targetTime, chunkBoundary);
            double chunkDelta = chunkEnd - cursor;
            double multiplier = multiplierFor(cursor);
            applyRegen(player, chunkDelta, multiplier, true);
            applyRegen(opponent, chunkDelta, multiplier, false);
            cursor = chunkEnd;
        }
        elapsedSeconds = targetTime;
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public double getRemainingSeconds() {
        return Math.max(0, TOTAL_DURATION_SECONDS - elapsedSeconds);
    }

    public double getTotalDurationSeconds() {
        return TOTAL_DURATION_SECONDS;
    }

    public boolean isFinished() {
        return elapsedSeconds >= TOTAL_DURATION_SECONDS;
    }

    public ElixirPhase getCurrentElixirPhase() {
        return elapsedSeconds >= TRIPLE_ELIXIR_START_SECONDS
            ? ElixirPhase.TRIPLE
            : ElixirPhase.DOUBLE;
    }

    private boolean isParticipant(Player potential) {
        return potential != null && (potential == player || potential == opponent);
    }

    private void applyRegen(Player target, double seconds, double multiplier, boolean isPlayerBucket) {
        if (target == null || seconds <= 0 || multiplier <= 0 || target.getCurrentElixir() >= target.getMaxElixir()) {
            return;
        }
        double addition = seconds * SINGLE_ELIXIR_PER_SECOND * multiplier;
        if (isPlayerBucket) {
            playerElixirFraction += addition;
            int whole = (int) playerElixirFraction;
            if (whole > 0) {
                target.regenerateElixir(whole);
                playerElixirFraction -= whole;
            }
        } else {
            opponentElixirFraction += addition;
            int whole = (int) opponentElixirFraction;
            if (whole > 0) {
                target.regenerateElixir(whole);
                opponentElixirFraction -= whole;
            }
        }
    }

    private double nextPhaseBoundary(double currentSeconds) {
        if (currentSeconds < TRIPLE_ELIXIR_START_SECONDS) {
            return TRIPLE_ELIXIR_START_SECONDS;
        }
        return TOTAL_DURATION_SECONDS;
    }

    private double multiplierFor(double currentSeconds) {
        return currentSeconds >= TRIPLE_ELIXIR_START_SECONDS ? 3.0 : 2.0;
    }
}




