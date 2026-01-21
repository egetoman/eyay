package kuroyale.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import kuroyale.support.Result;

public class Match {

    private static final double NORMAL_DURATION_SECONDS = 3 * 60; // 3 minutes as per document
    private static final double OVERTIME_DURATION_SECONDS = 3 * 60; // 3 minutes overtime
    private static final double DOUBLE_ELIXIR_START_SECONDS = 2 * 60; // Double elixir starts at 2 minutes
    private static final double TRIPLE_ELIXIR_START_SECONDS = 5 * 60; // Triple elixir starts at 5 minutes
    private static final double OVERTIME_START_SECONDS = NORMAL_DURATION_SECONDS;
    private static final double OVERTIME_END_SECONDS = NORMAL_DURATION_SECONDS + OVERTIME_DURATION_SECONDS;
    private static final double SINGLE_ELIXIR_PER_SECOND = 1.0 / 2.8;
    private static final double SUDDEN_DEATH_DAMAGE_PER_SECOND = 120.0;

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
    private boolean suddenDeathActive;
    private double suddenDeathDamageRemainder;
    private final Set<String> suddenDeathAliveTowerKeys = new HashSet<>();
    private final Map<String, Integer> suddenDeathHpBefore = new HashMap<>();
    private boolean overtimeActive;
    private final Set<String> overtimeAliveTowerKeys = new HashSet<>();

    // Card play tracking for quests
    private int playerSpellsPlayed;
    private int playerTroopsDeployed;
    private int playerBuildingsPlayed;
    private int playerElixirSpent;
    private int playerCardsPlayed;
    private int playerSpellDamageDealt;

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

    public double getPlayerElixirFraction() {
        return playerElixirFraction;
    }

    public double getOpponentElixirFraction() {
        return opponentElixirFraction;
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
        // Spells can be cast on occupied tiles (they're area effects)
        if (card.getType() != CardType.SPELL && !arena.isTileFree(position)) {
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

        // Track card plays for quests (only for human player, not bot/opponent)
        boolean isHumanPlayer = (actingPlayer == player);
        if (isHumanPlayer) {
            playerElixirSpent += cost;
            playerCardsPlayed++;
        }
        
        // Handle spells differently - they deal area damage immediately
        if (card.getType() == CardType.SPELL) {
            if (isHumanPlayer) {
                playerSpellsPlayed++;
            }
            castSpell(card, position, actingPlayer);
            // Return a dummy unit for compatibility, but spells don't create units
            return Result.ok(null);
        }
        
        // Track troop/building deployment for quests
        if (isHumanPlayer) {
            if (card.getType() == CardType.TROOP) {
                playerTroopsDeployed++;
            } else if (card.getType() == CardType.BUILDING) {
                playerBuildingsPlayed++;
            }
        }

        // Handle troops and buildings
        int hp = card.getStats() != null ? card.getStats().getHp() : 0;
        int spawnCount = card.getStats() != null ? card.getStats().getSpawnCount() : 1;
        TowerOwner unitOwner = resolveOwner(actingPlayer);
        
        // Spawn multiple units for swarm troops
        Unit firstUnit = null;
        List<Position> spawnPositions = calculateSpawnPositions(position, spawnCount);
        
        for (int i = 0; i < spawnCount && i < spawnPositions.size(); i++) {
            Position spawnPos = spawnPositions.get(i);
            Unit unit = new Unit(card, spawnPos, hp, unitOwner);
            Tower initialTarget = arena.findNearestEnemyTower(unitOwner, spawnPos);
        unit.setTargetTower(initialTarget);
        arena.addUnit(unit);
            if (firstUnit == null) {
                firstUnit = unit;
            }
        }
        
        return Result.ok(firstUnit);
    }

    /**
     * Calculates spawn positions for multiple units in a formation pattern.
     * Units are spread out around the center position to avoid overlapping.
     */
    private List<Position> calculateSpawnPositions(Position center, int count) {
        List<Position> positions = new ArrayList<>();
        if (center == null || count <= 0) {
            return positions;
        }
        
        int cx = center.getX();
        int cy = center.getY();
        
        if (count == 1) {
            positions.add(new Position(cx, cy));
        } else if (count == 2) {
            // Side by side
            positions.add(new Position(cx - 1, cy));
            positions.add(new Position(cx + 1, cy));
        } else if (count == 3) {
            // Triangle formation
            positions.add(new Position(cx, cy));
            positions.add(new Position(cx - 1, cy + 1));
            positions.add(new Position(cx + 1, cy + 1));
        } else if (count == 4) {
            // Square formation
            positions.add(new Position(cx - 1, cy));
            positions.add(new Position(cx + 1, cy));
            positions.add(new Position(cx - 1, cy + 1));
            positions.add(new Position(cx + 1, cy + 1));
        } else if (count == 5) {
            // Pentagon-like: center + 4 corners
            positions.add(new Position(cx, cy));
            positions.add(new Position(cx - 1, cy - 1));
            positions.add(new Position(cx + 1, cy - 1));
            positions.add(new Position(cx - 1, cy + 1));
            positions.add(new Position(cx + 1, cy + 1));
        } else {
            // For 6+: two rows
            int perRow = (count + 1) / 2;
            int startX = cx - (perRow - 1) / 2;
            for (int i = 0; i < perRow && positions.size() < count; i++) {
                positions.add(new Position(startX + i, cy));
            }
            startX = cx - (count - perRow - 1) / 2;
            for (int i = 0; i < count - perRow && positions.size() < count; i++) {
                positions.add(new Position(startX + i, cy + 1));
            }
        }
        
        return positions;
    }

    /**
     * Casts a spell at the target position, dealing area damage.
     * Spells deal reduced damage to towers (40% of unit damage).
     * Zap also stuns units for 0.5 seconds.
     */
    private void castSpell(Card spell, Position targetPosition, Player caster) {
        if (spell == null || targetPosition == null || spell.getStats() == null) {
            return;
        }

        int areaDamage = spell.getStats().getDamage();
        // Spell ranges are already in tiles (e.g., Rocket=2.0, Zap=2.5, Arrows=4.0)
        // Unlike unit ranges which are in internal units and need /10 conversion
        double radiusTiles = spell.getStats().getRange();
        String spellId = spell.getId();
        boolean isZap = "card_zap".equals(spellId);
        double stunDuration = isZap ? 0.5 : 0.0;
        TowerOwner casterOwner = resolveOwner(caster);
        boolean isHumanPlayer = (caster == player);
        int totalDamageDealt = 0;

        // Damage units
        List<Unit> unitsInRadius = arena.findUnitsInRadius(targetPosition, radiusTiles);
        for (Unit unit : unitsInRadius) {
            if (unit != null && !unit.isDefeated() && unit.getOwner() != casterOwner) {
                // Track actual damage dealt (capped by remaining HP)
                int damageDealt = Math.min(areaDamage, unit.getCurrentHP());
                unit.takeDamage(areaDamage);
                if (isHumanPlayer) {
                    totalDamageDealt += damageDealt;
                }
                if (isZap && stunDuration > 0) {
                    unit.applyStun(stunDuration);
                }
            }
        }

        // Damage towers (40% of unit damage)
        int towerDamage = (int) Math.round(areaDamage * 0.4);
        List<Tower> towersInRadius = arena.findTowersInRadius(targetPosition, radiusTiles);
        for (Tower tower : towersInRadius) {
            if (tower != null && !tower.isDestroyed() && tower.getOwner() != casterOwner) {
                // Track actual damage dealt (capped by remaining HP)
                int damageDealt = Math.min(towerDamage, tower.getHp());
                tower.takeDamage(towerDamage);
                if (isHumanPlayer) {
                    totalDamageDealt += damageDealt;
                }
            }
        }

        // Track spell damage for quests
        if (isHumanPlayer) {
            playerSpellDamageDealt += totalDamageDealt;
        }
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
        double targetTime = elapsedSeconds + deltaSeconds;
        if (!suddenDeathActive) {
            double cap = overtimeActive ? OVERTIME_END_SECONDS : NORMAL_DURATION_SECONDS;
            targetTime = Math.min(cap, targetTime);
        }
        double cursor = elapsedSeconds;
        while (cursor < targetTime) {
            double chunkBoundary = suddenDeathActive ? targetTime : nextPhaseBoundary(cursor);
            double chunkEnd = Math.min(targetTime, chunkBoundary);
            double chunkDelta = chunkEnd - cursor;
            double multiplier = multiplierFor(cursor);
            applyRegen(player, chunkDelta, multiplier, true);
            applyRegen(opponent, chunkDelta, multiplier, false);
            handleElixirCollectors(chunkDelta);
            arena.tick(chunkDelta);
            handleBotBehavior(chunkDelta);
            cursor = chunkEnd;
            if (suddenDeathActive) {
                applySuddenDeathDamage(chunkDelta);
                if (resolveSuddenDeathOutcomeIfNeeded()) {
                    break;
                }
            } else {
            updateOutcomeIfNeeded(false);
            if (isOver()) {
                break;
            }
        }
        }
        elapsedSeconds = cursor;
        if (suddenDeathActive) {
            resolveSuddenDeathOutcomeIfNeeded();
        } else {
        updateOutcomeIfNeeded(true);
        }
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public double getRemainingSeconds() {
        if (suddenDeathActive) {
            return 0;
        }
        if (overtimeActive) {
            return Math.max(0, OVERTIME_END_SECONDS - elapsedSeconds);
        }
        return Math.max(0, NORMAL_DURATION_SECONDS - elapsedSeconds);
    }

    public double getTotalDurationSeconds() {
        return NORMAL_DURATION_SECONDS + OVERTIME_DURATION_SECONDS;
    }

    public boolean isFinished() {
        return isOver();
    }

    public boolean isOver() {
        return outcome != null;
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

    // Quest tracking getters
    public int getPlayerSpellsPlayed() {
        return playerSpellsPlayed;
    }

    public int getPlayerTroopsDeployed() {
        return playerTroopsDeployed;
    }

    public int getPlayerBuildingsPlayed() {
        return playerBuildingsPlayed;
    }

    public int getPlayerElixirSpent() {
        return playerElixirSpent;
    }

    public int getPlayerCardsPlayed() {
        return playerCardsPlayed;
    }

    public int getPlayerSpellDamageDealt() {
        return playerSpellDamageDealt;
    }

    /**
     * Checks if the player's deck contains only common rarity cards.
     * 
     * @return true if all cards in the player's deck are common rarity
     */
    public boolean isPlayerDeckAllCommon() {
        if (player == null || player.getDeck() == null) {
            return false;
        }
        List<Card> cards = player.getDeck().getCards();
        if (cards == null || cards.isEmpty()) {
            return false;
        }
        for (Card card : cards) {
            if (card == null) {
                continue;
            }
            Rarity rarity = CardRarityCatalog.rarityForCardId(card.getId());
            if (rarity != Rarity.COMMON) {
                return false;
            }
        }
        return true;
    }

    /**
     * Used by synchronization systems to align clocks.
     */
    public void setElapsedSeconds(double elapsedSeconds) {
        this.elapsedSeconds = Math.max(0, elapsedSeconds);
    }

    public ElixirPhase getCurrentElixirPhase() {
        if (elapsedSeconds >= TRIPLE_ELIXIR_START_SECONDS) {
            return ElixirPhase.TRIPLE; // 5-6 min
        }
        if (elapsedSeconds >= DOUBLE_ELIXIR_START_SECONDS) {
            return ElixirPhase.DOUBLE; // 2-5 min (includes overtime)
        }
        return ElixirPhase.NORMAL; // 0-2 min
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
        if (currentSeconds < DOUBLE_ELIXIR_START_SECONDS) {
            return DOUBLE_ELIXIR_START_SECONDS;
        }
        if (currentSeconds < OVERTIME_START_SECONDS) {
            return OVERTIME_START_SECONDS;
        }
        return OVERTIME_END_SECONDS;
    }

    private double multiplierFor(double currentSeconds) {
        if (currentSeconds >= TRIPLE_ELIXIR_START_SECONDS) {
            return 3.0; // Triple elixir in last minute of overtime (5-6 min)
        }
        if (currentSeconds >= DOUBLE_ELIXIR_START_SECONDS) {
            return 2.0; // Double elixir from 2-5 min (includes overtime)
        }
        return 1.0; // Normal elixir 0-2 min
    }

    private void activateOvertime() {
        if (arena == null) {
            return;
        }
        overtimeActive = true;
        overtimeAliveTowerKeys.clear();
        for (Tower tower : arena.getTowers()) {
            if (tower == null || tower.isDestroyed() || tower.getPosition() == null || tower.getOwner() == null
                    || tower.getType() == null) {
                continue;
            }
            overtimeAliveTowerKeys.add(buildTowerKey(tower));
        }
    }

    private boolean resolveOvertimeTowerDestroyed() {
        if (!overtimeActive || arena == null || arena.getTowers() == null) {
            return false;
        }
        Tower destroyed = null;
        for (Tower tower : arena.getTowers()) {
            if (tower == null || tower.getPosition() == null || tower.getOwner() == null || tower.getType() == null) {
                continue;
            }
            String key = buildTowerKey(tower);
            if (overtimeAliveTowerKeys.contains(key) && tower.isDestroyed()) {
                overtimeAliveTowerKeys.remove(key);
                if (destroyed == null) {
                    destroyed = tower;
                }
            }
        }
        if (destroyed == null) {
            return false;
        }
        int playerCrowns = countDestroyedCrowns(TowerOwner.OPPONENT);
        int opponentCrowns = countDestroyedCrowns(TowerOwner.PLAYER);
        TowerOwner winner = destroyed.getOwner() == TowerOwner.PLAYER ? TowerOwner.OPPONENT : TowerOwner.PLAYER;
        outcome = new MatchOutcome(winner, playerCrowns, opponentCrowns, "OVERTIME_TOWER_DESTROYED");
        return true;
    }

    private void activateSuddenDeath() {
        if (arena == null) {
            return;
        }
        suddenDeathActive = true;
        suddenDeathDamageRemainder = 0;
        suddenDeathAliveTowerKeys.clear();
        suddenDeathHpBefore.clear();
        for (Tower tower : arena.getTowers()) {
            if (tower == null || tower.isDestroyed() || tower.getPosition() == null || tower.getOwner() == null
                    || tower.getType() == null) {
                continue;
            }
            suddenDeathAliveTowerKeys.add(buildTowerKey(tower));
        }
    }

    private void applySuddenDeathDamage(double deltaSeconds) {
        if (!suddenDeathActive || arena == null || deltaSeconds <= 0) {
            return;
        }
        double totalDamage = deltaSeconds * SUDDEN_DEATH_DAMAGE_PER_SECOND + suddenDeathDamageRemainder;
        int damage = (int) Math.floor(totalDamage);
        suddenDeathDamageRemainder = totalDamage - damage;
        if (damage <= 0) {
            return;
        }
        suddenDeathHpBefore.clear();
        for (Tower tower : arena.getTowers()) {
            if (tower == null || tower.isDestroyed() || tower.getPosition() == null || tower.getOwner() == null
                    || tower.getType() == null) {
                continue;
            }
            suddenDeathHpBefore.put(buildTowerKey(tower), tower.getHp());
            tower.takeDamage(damage);
        }
    }

    private boolean resolveSuddenDeathOutcomeIfNeeded() {
        if (!suddenDeathActive || arena == null || arena.getTowers() == null) {
            return false;
        }
        Set<TowerOwner> destroyedOwners = new HashSet<>();
        TowerOwner candidateOwner = null;
        int candidateHpBefore = Integer.MAX_VALUE;
        int playerMinBefore = Integer.MAX_VALUE;
        int opponentMinBefore = Integer.MAX_VALUE;
        int playerTotalBefore = 0;
        int opponentTotalBefore = 0;
        for (Tower tower : arena.getTowers()) {
            if (tower == null || tower.getPosition() == null || tower.getOwner() == null || tower.getType() == null) {
                continue;
            }
            String key = buildTowerKey(tower);
            Integer hpBefore = suddenDeathHpBefore.get(key);
            if (hpBefore != null) {
                if (tower.getOwner() == TowerOwner.PLAYER) {
                    playerTotalBefore += hpBefore;
                    playerMinBefore = Math.min(playerMinBefore, hpBefore);
                } else {
                    opponentTotalBefore += hpBefore;
                    opponentMinBefore = Math.min(opponentMinBefore, hpBefore);
                }
            }
            if (suddenDeathAliveTowerKeys.contains(key) && tower.isDestroyed()) {
                destroyedOwners.add(tower.getOwner());
                suddenDeathAliveTowerKeys.remove(key);
                if (hpBefore != null && hpBefore < candidateHpBefore) {
                    candidateHpBefore = hpBefore;
                    candidateOwner = tower.getOwner();
                }
            }
        }
        if (destroyedOwners.isEmpty()) {
            return false;
        }
        int playerCrowns = countDestroyedCrowns(TowerOwner.OPPONENT);
        int opponentCrowns = countDestroyedCrowns(TowerOwner.PLAYER);
        TowerOwner destroyedOwner;
        if (destroyedOwners.size() == 1) {
            destroyedOwner = destroyedOwners.iterator().next();
        } else {
            if (candidateOwner != null) {
                destroyedOwner = candidateOwner;
            } else {
                if (playerMinBefore < opponentMinBefore) {
                    destroyedOwner = TowerOwner.PLAYER;
                } else if (opponentMinBefore < playerMinBefore) {
                    destroyedOwner = TowerOwner.OPPONENT;
                } else if (playerTotalBefore < opponentTotalBefore) {
                    destroyedOwner = TowerOwner.PLAYER;
                } else if (opponentTotalBefore < playerTotalBefore) {
                    destroyedOwner = TowerOwner.OPPONENT;
                } else {
                    destroyedOwner = TowerOwner.PLAYER;
                }
            }
        }
        TowerOwner winner = destroyedOwner == TowerOwner.PLAYER ? TowerOwner.OPPONENT : TowerOwner.PLAYER;
        outcome = new MatchOutcome(winner, playerCrowns, opponentCrowns, "SUDDEN_DEATH");
        return true;
    }

    private int countDestroyedCrowns(TowerOwner owner) {
        if (arena == null || arena.getTowers() == null) {
            return 0;
        }
        int count = 0;
        for (Tower tower : arena.getTowers()) {
            if (tower == null || tower.getOwner() != owner || tower.getType() != TowerType.CROWN) {
                continue;
            }
            if (tower.isDestroyed()) {
                count++;
            }
        }
        return count;
    }

    private String buildTowerKey(Tower tower) {
        return tower.getOwner() + "|" + tower.getType() + "|" + tower.getPosition().getX() + "|"
                + tower.getPosition().getY();
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
        int playerCrownTowersDestroyed = 0; // crown towers owned by PLAYER that are destroyed (opponent earned)
        int opponentCrownTowersDestroyed = 0; // crown towers owned by OPPONENT that are destroyed (player earned)
        for (Tower tower : arena.getTowers()) {
            if (tower == null) {
                continue;
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

        if (overtimeActive) {
            if (resolveOvertimeTowerDestroyed()) {
            return;
        }
        }

        if (considerTimeOut && elapsedSeconds >= NORMAL_DURATION_SECONDS && !overtimeActive) {
            if (suddenDeathActive) {
                return;
            }
            TowerOwner winner = null;
            // First tie-break: crowns (destroyed opponent crown towers)
            if (opponentCrownTowersDestroyed > playerCrownTowersDestroyed) {
                winner = TowerOwner.PLAYER;
                outcome = new MatchOutcome(winner, opponentCrownTowersDestroyed, playerCrownTowersDestroyed,
                        "TIME_OUT_CROWNS");
                return;
            } else if (playerCrownTowersDestroyed > opponentCrownTowersDestroyed) {
                winner = TowerOwner.OPPONENT;
                outcome = new MatchOutcome(winner, opponentCrownTowersDestroyed, playerCrownTowersDestroyed,
                        "TIME_OUT_CROWNS");
                return;
            }
            activateOvertime();
                return;
        }

        if (considerTimeOut && overtimeActive && elapsedSeconds >= OVERTIME_END_SECONDS) {
            if (suddenDeathActive) {
                return;
            }
            activateSuddenDeath();
        }
    }

    // Track elixir generation for each collector to avoid double-generation
    private Map<Unit, Integer> elixirCollectorGenerations = new HashMap<>();

    /**
     * Handles elixir generation from Elixir Collector buildings.
     * Elixir Collectors generate 1 elixir every 10 seconds, up to 7 total over 70
     * seconds.
     */
    private void handleElixirCollectors(double deltaSeconds) {
        if (arena == null || deltaSeconds <= 0) {
            return;
        }
        List<Unit> unitsToRemove = new ArrayList<>();
        for (Unit unit : arena.getUnits()) {
            if (unit == null || unit.isDefeated() || unit.getCard() == null) {
                continue;
            }
            if ("card_elixir_collector".equals(unit.getCard().getId())) {
                TowerOwner owner = unit.getOwner();
                if (owner == null) {
                    continue;
                }
                Player ownerPlayer = (owner == TowerOwner.PLAYER) ? player : opponent;
                if (ownerPlayer == null) {
                    continue;
                }
                double lifetime = unit.getLifetimeSeconds();
                int expectedGenerations = (int) Math.floor(lifetime / 10.0);
                // Limit to 7 total generations over 70 seconds
                if (expectedGenerations > 7) {
                    expectedGenerations = 7;
                }
                int currentGenerations = elixirCollectorGenerations.getOrDefault(unit, 0);
                // Generate elixir if we've crossed a new 10-second boundary
                while (currentGenerations < expectedGenerations && currentGenerations < 7 && lifetime <= 70.0) {
                    ownerPlayer.regenerateElixir(1);
                    currentGenerations++;
                    elixirCollectorGenerations.put(unit, currentGenerations);
                }
                // Remove from map if collector is destroyed or expired
                if (unit.isDefeated() || lifetime >= 70.0) {
                    unitsToRemove.add(unit);
                }
            }
        }
        // Clean up destroyed collectors
        for (Unit unit : unitsToRemove) {
            elixirCollectorGenerations.remove(unit);
        }
    }
}
