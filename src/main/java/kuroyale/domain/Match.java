package kuroyale.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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
    private double botDecisionTimer;
    private final Random random = new Random();

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
        TowerOwner unitOwner = resolveOwner(actingPlayer);
        Unit unit = new Unit(card, new Position(position.getX(), position.getY()), hp, unitOwner);
        Tower initialTarget = arena.findNearestEnemyTower(unitOwner, position);
        unit.setTargetTower(initialTarget);
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
            arena.tickUnits(chunkDelta);
            handleBotBehavior(chunkDelta);
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

    private TowerOwner resolveOwner(Player actingPlayer) {
        return actingPlayer == player ? TowerOwner.PLAYER : TowerOwner.OPPONENT;
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

    private void handleBotBehavior(double deltaSeconds) {
        if (opponent == null || opponent.getDeck() == null || arena == null) {
            return;
        }
        botDecisionTimer += deltaSeconds;
        if (botDecisionTimer < 4.0) {
            return;
        }
        botDecisionTimer = 0;

        Card cardToPlay = pickBotCard(opponent);
        if (cardToPlay == null) {
            return;
        }
        Position spawn = findBotSpawnPosition();
        if (spawn == null) {
            return;
        }
        deployCard(opponent, cardToPlay, spawn);
    }

    private Card pickBotCard(Player acting) {
        Deck deck = acting.getDeck();
        if (deck == null) {
            return null;
        }
        List<Card> options = new ArrayList<>(deck.getCards());
        Collections.shuffle(options, random);
        for (Card candidate : options) {
            if (candidate == null) {
                continue;
            }
            if (candidate.getElixirCost() > acting.getCurrentElixir()) {
                continue;
            }
            if (candidate.getType() != CardType.TROOP) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private Position findBotSpawnPosition() {
        int width = arena.getWidth();
        int height = arena.getHeight();
        int minY = height / 2;
        int maxY = Math.max(minY + 1, height - 2);
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = random.nextInt(Math.max(1, width - 2)) + 1;
            int y = random.nextInt(Math.max(1, maxY - minY)) + minY;
            Position candidate = new Position(x, y);
            if (arena.isWithinBounds(candidate) && arena.isTileFree(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}




