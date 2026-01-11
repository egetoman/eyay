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
    private transient CardCostPolicy cardCostPolicy;
    private boolean botEnabled = true;
    private MatchOutcome outcome;

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
        int baseCost = card.getElixirCost();
        int cost = cardCostPolicy != null ? cardCostPolicy.resolveCost(actingPlayer, card, baseCost) : baseCost;
        cost = Math.max(0, cost);
        if (!actingPlayer.hasEnoughElixir(cost)) {
            return Result.fail("Not enough elixir.");
        }
        if (!arena.isWithinBounds(position)) {
            return Result.fail("Position is outside the arena bounds.");
        }
        if (!arena.isTileFree(position)) {
            return Result.fail("Target tile is occupied.");
        }

        // Territory restriction: Only apply in single-player mode (botEnabled=true)
        // In Local PvP/Network modes, the view handles territory validation
        // Troops and buildings can only be placed on player's side
        // Spells can be cast anywhere (including on enemy towers)
        if (botEnabled) {
            TowerOwner unitOwner = resolveOwner(actingPlayer);
            CardType cardType = card.getType();

            if (cardType == CardType.TROOP || cardType == CardType.BUILDING) {
                boolean isPlayerUnit = (unitOwner == TowerOwner.PLAYER);
                boolean isOnPlayerSide = arena.isPlayerSide(position);
                boolean isOnOpponentSide = arena.isOpponentSide(position);

                if (isPlayerUnit && !isOnPlayerSide) {
                    return Result.fail("You can only deploy units on your side of the arena.");
                }
                if (!isPlayerUnit && !isOnOpponentSide) {
                    return Result.fail("Opponent can only deploy on their side.");
                }
            }
        }
        // Spells (CardType.SPELL) have no territory restriction

        actingPlayer.spendElixir(cost);
        int hp = card.getStats() != null ? card.getStats().getHp() : 0;
        TowerOwner unitOwner = resolveOwner(actingPlayer);
        Unit unit = new Unit(card, new Position(position.getX(), position.getY()), hp, unitOwner);
        Tower initialTarget = arena.findNearestEnemyTower(unitOwner, position);
        unit.setTargetTower(initialTarget);
        arena.addUnit(unit);
        return Result.ok(unit);
    }

    /**
     * Sets a cost policy to modify card elixir costs for special modes (e.g.,
     * challenges).
     * If null, the match uses {@link Card#getElixirCost()}.
     */
    public void setCardCostPolicy(CardCostPolicy cardCostPolicy) {
        this.cardCostPolicy = cardCostPolicy;
    }

    public CardCostPolicy getCardCostPolicy() {
        return cardCostPolicy;
    }

    public void advanceTime(double deltaSeconds) {
        if (deltaSeconds <= 0) {
            return;
        }
        if (isOver()) {
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
            updateOutcomeIfNeeded(false);
            if (isOver()) {
                break;
            }
        }
        elapsedSeconds = targetTime;
        updateOutcomeIfNeeded(true);
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
        return isOver();
    }

    public boolean isOver() {
        return outcome != null || elapsedSeconds >= TOTAL_DURATION_SECONDS;
    }

    public MatchOutcome getOutcome() {
        updateOutcomeIfNeeded(true);
        return outcome;
    }

    /**
     * Enables/disables built-in bot behavior. Local PvP / Network / Replay should
     * disable this.
     */
    public boolean isBotEnabled() {
        return botEnabled;
    }

    public void setBotEnabled(boolean botEnabled) {
        this.botEnabled = botEnabled;
    }

    /**
     * Used by synchronization systems to align clocks.
     */
    public void setElapsedSeconds(double elapsedSeconds) {
        this.elapsedSeconds = Math.max(0, Math.min(TOTAL_DURATION_SECONDS, elapsedSeconds));
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
        if (!botEnabled) {
            return;
        }
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

    private void updateOutcomeIfNeeded(boolean considerTimeOut) {
        if (outcome != null || arena == null) {
            return;
        }

        Tower playerKing = null;
        Tower opponentKing = null;
        int playerCrownTowersDestroyed = 0;   // crown towers owned by PLAYER that are destroyed (opponent earned)
        int opponentCrownTowersDestroyed = 0; // crown towers owned by OPPONENT that are destroyed (player earned)
        int playerTowerHpTotal = 0;
        int opponentTowerHpTotal = 0;

        for (Tower tower : arena.getTowers()) {
            if (tower == null) {
                continue;
            }
            // HP totals for time-out tie-break (include 0 for destroyed towers)
            if (tower.getOwner() == TowerOwner.PLAYER) {
                playerTowerHpTotal += Math.max(0, tower.getHp());
            } else if (tower.getOwner() == TowerOwner.OPPONENT) {
                opponentTowerHpTotal += Math.max(0, tower.getHp());
            }
            if (tower.getType() == TowerType.KING) {
                if (tower.getOwner() == TowerOwner.PLAYER) {
                    playerKing = tower;
                } else if (tower.getOwner() == TowerOwner.OPPONENT) {
                    opponentKing = tower;
                }
            } else if (tower.getType() == TowerType.CROWN) {
                if (tower.isDestroyed()) {
                    if (tower.getOwner() == TowerOwner.PLAYER) {
                        playerCrownTowersDestroyed++;
                    } else if (tower.getOwner() == TowerOwner.OPPONENT) {
                        opponentCrownTowersDestroyed++;
                    }
                }
            }
        }

        boolean playerKingDestroyed = playerKing != null && playerKing.isDestroyed();
        boolean opponentKingDestroyed = opponentKing != null && opponentKing.isDestroyed();

        if (playerKingDestroyed || opponentKingDestroyed) {
            TowerOwner winner = null;
            if (playerKingDestroyed && !opponentKingDestroyed) {
                winner = TowerOwner.OPPONENT;
            } else if (opponentKingDestroyed && !playerKingDestroyed) {
                winner = TowerOwner.PLAYER;
            }
            int playerCrowns = opponentKingDestroyed ? 3 : opponentCrownTowersDestroyed;
            int opponentCrowns = playerKingDestroyed ? 3 : playerCrownTowersDestroyed;
            outcome = new MatchOutcome(winner, playerCrowns, opponentCrowns, "KING_DESTROYED");
            return;
        }

        // Scenario 2: if one side has destroyed more crown towers than the other (max 2), end match.
        // We treat "2 crowns" (both crown towers destroyed) as an early victory condition.
        if (opponentCrownTowersDestroyed == 2 && playerCrownTowersDestroyed < 2) {
            outcome = new MatchOutcome(TowerOwner.PLAYER, 2, playerCrownTowersDestroyed, "CROWN_TOWERS_DESTROYED");
            return;
        }
        if (playerCrownTowersDestroyed == 2 && opponentCrownTowersDestroyed < 2) {
            outcome = new MatchOutcome(TowerOwner.OPPONENT, opponentCrownTowersDestroyed, 2, "CROWN_TOWERS_DESTROYED");
            return;
        }

        if (considerTimeOut && elapsedSeconds >= TOTAL_DURATION_SECONDS) {
            TowerOwner winner = null;
            // First tie-break: crowns (destroyed opponent crown towers)
            if (opponentCrownTowersDestroyed > playerCrownTowersDestroyed) {
                winner = TowerOwner.PLAYER;
                outcome = new MatchOutcome(winner, opponentCrownTowersDestroyed, playerCrownTowersDestroyed, "TIME_OUT_CROWNS");
                return;
            } else if (playerCrownTowersDestroyed > opponentCrownTowersDestroyed) {
                winner = TowerOwner.OPPONENT;
                outcome = new MatchOutcome(winner, opponentCrownTowersDestroyed, playerCrownTowersDestroyed, "TIME_OUT_CROWNS");
                return;
            }

            // Second tie-break: total remaining tower HP
            if (playerTowerHpTotal > opponentTowerHpTotal) {
                winner = TowerOwner.PLAYER;
                outcome = new MatchOutcome(winner, opponentCrownTowersDestroyed, playerCrownTowersDestroyed, "TIME_OUT_HP");
                return;
            } else if (opponentTowerHpTotal > playerTowerHpTotal) {
                winner = TowerOwner.OPPONENT;
                outcome = new MatchOutcome(winner, opponentCrownTowersDestroyed, playerCrownTowersDestroyed, "TIME_OUT_HP");
                return;
            }

            outcome = new MatchOutcome(null, opponentCrownTowersDestroyed, playerCrownTowersDestroyed, "TIME_OUT_DRAW");
        }
    }
}
