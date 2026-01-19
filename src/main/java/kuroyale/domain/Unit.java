package kuroyale.domain;

import java.util.List;
import kuroyale.infrastructure.CardCatalogRepository;
import java.io.FileWriter;
import java.io.PrintWriter;

public class Unit {

    private static final double DEFAULT_SPEED_TILES_PER_SECOND = 1.0;

    private Card card;
    private Position position;
    private int currentHP;
    private TowerOwner owner;
    private Tower targetTower;
    private Unit targetEnemyUnit;
    private double preciseX;
    private double preciseY;
    private double speedTilesPerSecond;
    private double attackRangeTiles;
    private int attackDamage;
    private double attackIntervalSeconds;
    private double attackCooldownSeconds;
    private double stunRemainingSeconds;
    private double spawnCooldownSeconds;
    private double lifetimeSeconds;
    private double elixirGenerationCooldownSeconds;

    // --- Combo modifiers (permanent until unit dies) ---
    private double damageMultiplier = 1.0;
    private double speedMultiplier = 1.0;
    private int hpBonus = 0;
    private double rangeBonus = 0.0;

    public Unit() {
        this.speedTilesPerSecond = DEFAULT_SPEED_TILES_PER_SECOND;
        this.attackRangeTiles = 1.5;
        this.attackDamage = 50;
        this.attackIntervalSeconds = 1.0;
        this.attackCooldownSeconds = 0;
        this.stunRemainingSeconds = 0;
        this.spawnCooldownSeconds = 0;
        this.lifetimeSeconds = 0;
        this.elixirGenerationCooldownSeconds = 0;
        this.damageMultiplier = 1.0;
        this.speedMultiplier = 1.0;
        this.hpBonus = 0;
        this.rangeBonus = 0.0;
    }

    public Unit(Card card, Position position, int currentHP, TowerOwner owner) {
        this.card = card;
        this.position = position != null ? new Position(position.getX(), position.getY()) : new Position(0, 0);
        this.currentHP = currentHP;
        this.owner = owner;
        this.preciseX = this.position.getX();
        this.preciseY = this.position.getY();
        this.speedTilesPerSecond = resolveSpeed(card);
        this.attackRangeTiles = resolveRange(card);
        this.attackDamage = resolveDamage(card);
        this.attackIntervalSeconds = resolveAttackInterval(card);
        this.attackCooldownSeconds = 0;
        this.stunRemainingSeconds = 0;
        this.spawnCooldownSeconds = 0;
        this.lifetimeSeconds = 0;
        this.elixirGenerationCooldownSeconds = 0;
        this.damageMultiplier = 1.0;
        this.speedMultiplier = 1.0;
        this.hpBonus = 0;
        this.rangeBonus = 0.0;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
        this.speedTilesPerSecond = resolveSpeed(card);
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        if (position == null) {
            return;
        }
        this.position = position;
        this.preciseX = position.getX();
        this.preciseY = position.getY();
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public void setCurrentHP(int currentHP) {
        this.currentHP = currentHP;
    }

    public int getMaxHP() {
        if (card == null || card.getStats() == null) {
            return currentHP;
        }
        return Math.max(1, card.getStats().getHp() + hpBonus);
    }

    public void applyHPBonus(int bonus) {
        if (bonus <= 0) {
            return;
        }
        // Increase both max and current HP by the same amount (simple + deterministic).
        hpBonus += bonus;
        currentHP += bonus;
    }

    public int getEffectiveDamage() {
        return (int) Math.round(attackDamage * damageMultiplier);
    }

    public double getEffectiveSpeed() {
        return speedTilesPerSecond * speedMultiplier;
    }

    public double getEffectiveRange() {
        return attackRangeTiles + rangeBonus;
    }

    public void setDamageMultiplier(double multiplier) {
        this.damageMultiplier = Math.max(1.0, multiplier);
    }

    public void setSpeedMultiplier(double multiplier) {
        this.speedMultiplier = Math.max(1.0, multiplier);
    }

    public void setRangeBonus(double bonus) {
        this.rangeBonus = Math.max(0.0, bonus);
    }

    public TowerOwner getOwner() {
        return owner;
    }

    public void setOwner(TowerOwner owner) {
        this.owner = owner;
    }

    public double getLifetimeSeconds() {
        return lifetimeSeconds;
    }

    public double getPreciseX() {
        return preciseX;
    }

    public double getPreciseY() {
        return preciseY;
    }

    /**
     * Allows networking/replay systems to set exact positions from authoritative
     * snapshots.
     * <p>
     * Domain note: this does not change any gameplay behavior; it only updates
     * render-relevant state.
     */
    public void setPrecisePosition(double x, double y) {
        this.preciseX = x;
        this.preciseY = y;
        int roundedX = (int) Math.round(x);
        int roundedY = (int) Math.round(y);
        if (position == null) {
            position = new Position(roundedX, roundedY);
        } else {
            position.setX(roundedX);
            position.setY(roundedY);
        }
    }

    public Tower getTargetTower() {
        return targetTower;
    }

    public void setTargetTower(Tower targetTower) {
        this.targetTower = targetTower;
    }

    public void moveTo(Position target) {
        if (target == null) {
            return;
        }
        this.preciseX = target.getX();
        this.preciseY = target.getY();
        this.position = new Position(target.getX(), target.getY());
    }

    public void takeDamage(int amount) {
        if (amount <= 0) {
            return;
        }
        currentHP = Math.max(0, currentHP - amount);
    }

    public void applyStun(double durationSeconds) {
        if (durationSeconds > 0) {
            stunRemainingSeconds = Math.max(stunRemainingSeconds, durationSeconds);
        }
    }

    public boolean isStunned() {
        return stunRemainingSeconds > 0;
    }

    public void tick(Arena arena, double deltaSeconds) {
        // Update lifetime and cooldowns
        lifetimeSeconds += deltaSeconds;
        stunRemainingSeconds = Math.max(0, stunRemainingSeconds - deltaSeconds);
        
        // Buildings should never move or acquire targets - they are stationary
        if (card != null && card.getType() == CardType.BUILDING) {
            // Handle spawner buildings
            if (isSpawnerBuilding()) {
                spawnCooldownSeconds = Math.max(0, spawnCooldownSeconds - deltaSeconds);
                if (spawnCooldownSeconds <= 0 && !isDefeated()) {
                    spawnUnits(arena);
                }
            }
            
            // Handle elixir collector
            if (isElixirCollector()) {
                elixirGenerationCooldownSeconds = Math.max(0, elixirGenerationCooldownSeconds - deltaSeconds);
                if (elixirGenerationCooldownSeconds <= 0 && !isDefeated()) {
                    generateElixir(arena);
                    elixirGenerationCooldownSeconds = 10.0; // Generate every 10 seconds
                }
                // Elixir collector lifetime is 70 seconds
                if (lifetimeSeconds >= 70.0) {
                    currentHP = 0; // Destroy after lifetime
                }
            }
            
            // Buildings can still attack if they have attack capabilities
            attackCooldownSeconds = Math.max(0, attackCooldownSeconds - deltaSeconds);
            double effectiveRange = getEffectiveRange();
            if (attackDamage > 0 && attackCooldownSeconds <= 0 && effectiveRange > 0) {
                // Find nearest enemy in range (respecting ground/flying restrictions)
                Unit nearestEnemy = arena.findNearestEnemyUnit(owner, position, effectiveRange, this);
                if (nearestEnemy != null) {
                    int effectiveDamage = getEffectiveDamage();
                    // Check if this building has area of effect attacks
                    double splashRadius = getSplashRadius();
                    if (splashRadius > 0) {
                        // AoE attack - damage all enemies in splash radius around the target
                        performAoEAttack(arena, nearestEnemy.getPosition(), splashRadius, effectiveDamage);
                    } else {
                        // Single target attack
                        nearestEnemy.takeDamage(effectiveDamage);
                    }
                    attackCooldownSeconds = attackIntervalSeconds;
                }
            }
            return; // Buildings don't move, so exit early
        }

        if (arena == null || deltaSeconds <= 0 || speedTilesPerSecond <= 0 || isDefeated()) {
            return;
        }
        
        // Stunned units cannot move or attack
        if (isStunned()) {
            return;
        }
        
        attackCooldownSeconds = Math.max(0, attackCooldownSeconds - deltaSeconds);

        // Check if we're currently locked onto a tower (in range and attacking it)
        boolean lockedOntoTower = false;
        if (targetTower != null && !targetTower.isDestroyed() && targetTower.getPosition() != null) {
            double dx = targetTower.getPosition().getX() - preciseX;
            double dy = targetTower.getPosition().getY() - preciseY;
            double distanceToTower = Math.sqrt(dx * dx + dy * dy);
            lockedOntoTower = (distanceToTower <= getEffectiveRange());
        }

        // Only acquire new targets if NOT locked onto a tower
        if (!lockedOntoTower) {
            acquireTargets(arena);
        }

        Position targetPosition = null;
        boolean attackingTower = false;

        // Determine target (enemy unit or tower)
        if (targetEnemyUnit != null && !targetEnemyUnit.isDefeated()) {
            targetPosition = targetEnemyUnit.getPosition();
        }
        if (targetPosition == null) {
            targetEnemyUnit = null;
            if (targetTower == null || targetTower.isDestroyed()) {
                targetTower = arena.findNearestEnemyTower(owner, position);
            }
            if (targetTower == null || targetTower.getPosition() == null) {
                return;
            }
            targetPosition = targetTower.getPosition();
            attackingTower = true;
        }

        // Calculate path and distance - use movement type for bridge/flying logic
        UnitMovementType moveType = getMovementType();
        Position travelTarget = arena.resolvePathTarget(position, targetPosition, moveType);
        double dx = travelTarget.getX() - preciseX;
        double dy = travelTarget.getY() - preciseY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        boolean headingToFinalTarget = travelTarget == targetPosition;
        // #region agent log
        boolean headingToFinalTargetEquals = travelTarget != null && targetPosition != null && travelTarget.equals(targetPosition);
        if (headingToFinalTarget != headingToFinalTargetEquals) { try (PrintWriter pw = new PrintWriter(new FileWriter("/Users/ozanozak/eyay/.cursor/debug.log", true))) { pw.println("{\"hypothesisId\":\"B\",\"location\":\"Unit.java:261\",\"message\":\"Reference vs equals mismatch\",\"data\":{\"refEqual\":" + headingToFinalTarget + ",\"valueEqual\":" + headingToFinalTargetEquals + ",\"travelTargetX\":" + (travelTarget != null ? travelTarget.getX() : -1) + ",\"travelTargetY\":" + (travelTarget != null ? travelTarget.getY() : -1) + ",\"targetPosX\":" + (targetPosition != null ? targetPosition.getX() : -1) + ",\"targetPosY\":" + (targetPosition != null ? targetPosition.getY() : -1) + "},\"timestamp\":" + System.currentTimeMillis() + "}"); } catch (Exception e) {} }
        // #endregion
        boolean inRange = headingToFinalTargetEquals && distance <= getEffectiveRange();

        // MOVEMENT: Move if not in attack range
        if (!inRange) {
            double step = getEffectiveSpeed() * deltaSeconds;
            double newX = preciseX;
            double newY = preciseY;
            
            // For ground units approaching river, use horizontal-first movement to reach bridge
            boolean useHorizontalFirst = false;
            if (moveType == UnitMovementType.GROUND) {
                // Check if we're approaching river (not already in it) and target is across
                boolean currentlyInRiver = arena.isRiverTile(preciseY);
                boolean targetInOrAcrossRiver = arena.isRiverTile(travelTarget.getY()) || 
                    (travelTarget.getY() > preciseY && arena.isRiverTile(preciseY + 1)) ||
                    (travelTarget.getY() < preciseY && arena.isRiverTile(preciseY - 1));
                
                // If we would enter river and not be on bridge, move horizontally first
                if (!currentlyInRiver && targetInOrAcrossRiver) {
                    double testY = preciseY + (dy / distance) * step;
                    if (arena.isRiverTile(testY) && !arena.isOnBridge(preciseX, testY)) {
                        useHorizontalFirst = true;
                    }
                }
            }
            
            if (useHorizontalFirst) {
                // Move only horizontally toward bridge X position first
                double targetX = travelTarget.getX();
                double horizontalDist = Math.abs(targetX - preciseX);
                if (horizontalDist > 0.01) {
                    double horizontalDir = (targetX - preciseX) / horizontalDist;
                    newX = preciseX + horizontalDir * step;
                    // #region agent log
                    try (PrintWriter pw = new PrintWriter(new FileWriter("/Users/ozanozak/eyay/.cursor/debug.log", true))) { pw.println("{\"hypothesisId\":\"F\",\"location\":\"Unit.java:285\",\"message\":\"Horizontal-first movement to bridge\",\"data\":{\"preciseX\":" + preciseX + ",\"preciseY\":" + preciseY + ",\"targetX\":" + targetX + ",\"newX\":" + newX + "},\"timestamp\":" + System.currentTimeMillis() + "}"); } catch (Exception e) {}
                    // #endregion
                } else {
                    // Already at bridge X, can move vertically
                    newY = preciseY + (dy / distance) * step;
                }
            } else if (distance <= step) {
                // Smoothly arrive at target
                newX = travelTarget.getX();
                newY = travelTarget.getY();
            } else {
                // Move toward target (diagonal OK)
                newX = preciseX + (dx / distance) * step;
                newY = preciseY + (dy / distance) * step;
            }
            
            // Ground units cannot enter river tiles (unless on a bridge)
            if (moveType == UnitMovementType.GROUND && arena.isRiverTile(newY)) {
                if (!arena.isOnBridge(newX, newY)) {
                    // #region agent log
                    double originalNewY = newY;
                    try (PrintWriter pw = new PrintWriter(new FileWriter("/Users/ozanozak/eyay/.cursor/debug.log", true))) { pw.println("{\"hypothesisId\":\"A,D\",\"location\":\"Unit.java:305\",\"message\":\"River blocked movement\",\"data\":{\"preciseX\":" + preciseX + ",\"preciseY\":" + preciseY + ",\"attemptedNewX\":" + newX + ",\"attemptedNewY\":" + originalNewY + ",\"isRiverTile\":true,\"isOnBridge\":false},\"timestamp\":" + System.currentTimeMillis() + "}"); } catch (Exception e) {}
                    // #endregion
                    // Cannot enter river - stay at safe Y position
                    newY = preciseY;
                    
                    // Move horizontally toward bridge instead
                    double targetX = travelTarget.getX();
                    double horizontalDist = Math.abs(targetX - preciseX);
                    if (horizontalDist > 0.01) {
                        double horizontalDir = (targetX - preciseX) / horizontalDist;
                        newX = preciseX + horizontalDir * step;
                        // #region agent log
                        try (PrintWriter pw = new PrintWriter(new FileWriter("/Users/ozanozak/eyay/.cursor/debug.log", true))) { pw.println("{\"hypothesisId\":\"A\",\"location\":\"Unit.java:317\",\"message\":\"Horizontal redirect to bridge\",\"data\":{\"targetX\":" + targetX + ",\"newX\":" + newX + ",\"horizontalDir\":" + horizontalDir + "},\"timestamp\":" + System.currentTimeMillis() + "}"); } catch (Exception e) {}
                        // #endregion
                    }
                }
            }
            
            preciseX = newX;
            preciseY = newY;
        }

        // ATTACK: Attack if in range and cooldown ready (independent of movement)
        if (inRange && attackCooldownSeconds <= 0 && attackDamage > 0) {
            int effectiveDamage = getEffectiveDamage();
            // Check if this unit has area of effect attacks
            double splashRadius = getSplashRadius();
            if (splashRadius > 0) {
                // AoE attack - damage all enemies in splash radius
                Position attackPosition = attackingTower && targetTower != null ? targetTower.getPosition() 
                    : (targetEnemyUnit != null ? targetEnemyUnit.getPosition() : position);
                if (attackPosition != null) {
                    performAoEAttack(arena, attackPosition, splashRadius, effectiveDamage);
                }
            } else {
                // Single target attack
                if (attackingTower) {
                    targetTower.takeDamage(effectiveDamage);
                } else if (targetEnemyUnit != null && canAttackUnit(targetEnemyUnit)) {
                    // Re-check canAttackUnit to ensure ground units don't attack flying targets
                    targetEnemyUnit.takeDamage(effectiveDamage);
                    // Immediately check if target was defeated and try to find new one
                    if (targetEnemyUnit.isDefeated()) {
                        targetEnemyUnit = null;
                        // Try to immediately acquire a new enemy unit (respecting ground/flying restrictions)
                        double detectionRadius = Math.max(getEffectiveRange() * 1.5, 2.5);
                        Unit newTarget = arena.findNearestEnemyUnit(owner, position, detectionRadius, this);
                        if (newTarget != null) {
                            targetEnemyUnit = newTarget;
                        }
                    }
                } else if (targetEnemyUnit != null && !canAttackUnit(targetEnemyUnit)) {
                    // Can't attack this target (e.g., ground unit vs flying target) - clear and find new target
                    targetEnemyUnit = null;
                }
            }
            attackCooldownSeconds = attackIntervalSeconds;
        }

        // Update position
        int roundedX = (int) Math.round(preciseX);
        int roundedY = (int) Math.round(preciseY);
        if (position == null) {
            position = new Position(roundedX, roundedY);
        } else {
            position.setX(roundedX);
            position.setY(roundedY);
        }
    }

    public boolean isDefeated() {
        return currentHP <= 0;
    }

    /**
     * Checks if this unit can attack the target unit based on movement types and attack target capability.
     * Ground units with GROUND target cannot attack flying units.
     * Units with AIR_AND_GROUND target can attack both ground and flying units.
     * 
     * @param target The unit to check if it can be attacked
     * @return true if this unit can attack the target unit, false otherwise
     */
    public boolean canAttackUnit(Unit target) {
        if (target == null || target.isDefeated()) {
            return false;
        }
        if (card == null || card.getTarget() == null) {
            return true; // Default behavior if card info is missing
        }
        
        CardTarget attackTarget = card.getTarget();
        UnitMovementType targetMovementType = target.getMovementType();
        
        // BUILDINGS target means this unit only attacks buildings, not other units
        if (attackTarget == CardTarget.BUILDINGS || attackTarget == CardTarget.NONE) {
            return false;
        }
        
        // If target is flying, check if attacker can hit air
        if (targetMovementType == UnitMovementType.FLYING) {
            // Only units with AIR_AND_GROUND can attack flying units
            return attackTarget == CardTarget.AIR_AND_GROUND;
        }
        
        // Ground targets can be attacked by both GROUND and AIR_AND_GROUND
        return attackTarget == CardTarget.GROUND || attackTarget == CardTarget.AIR_AND_GROUND;
    }

    /**
     * Gets the movement type of this unit (ground or flying).
     * Defaults to GROUND if card information is not available.
     */
    public UnitMovementType getMovementType() {
        if (card == null) {
            return UnitMovementType.GROUND;
        }
        return card.getMovementType();
    }

    private void acquireTargets(Arena arena) {
        // Building-only units (Giant, Hog Rider) should never target enemy troops
        if (card != null && card.getTarget() == CardTarget.BUILDINGS) {
            targetEnemyUnit = null; // Always null for building-only units
            return;
        }

        // Normal targeting logic for units that can attack troops
        // Reduced detection radius for more natural engagement (1.5x instead of 2x)
        double detectionRadius = Math.max(getEffectiveRange() * 1.5, 2.5);
        if (targetEnemyUnit == null || targetEnemyUnit.isDefeated()) {
            Unit candidate = arena.findNearestEnemyUnit(owner, position, detectionRadius, this);
            if (candidate != null) {
                targetEnemyUnit = candidate;
                targetTower = null;
            }
        } else {
            // Check if current target can still be attacked (e.g., ground unit cannot attack flying)
            if (!canAttackUnit(targetEnemyUnit)) {
                targetEnemyUnit = null;
            } else {
                double dx = targetEnemyUnit.getPreciseX() - preciseX;
                double dy = targetEnemyUnit.getPreciseY() - preciseY;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance > detectionRadius * 1.5) {
                    targetEnemyUnit = null;
                }
            }
        }
    }

    private double resolveSpeed(Card referenceCard) {
        if (referenceCard == null || referenceCard.getStats() == null) {
            return DEFAULT_SPEED_TILES_PER_SECOND;
        }
        int speedValue = referenceCard.getStats().getMoveSpeed();
        if (speedValue >= 50) {
            return 2.2;
        }
        if (speedValue >= 40) {
            return 1.8;
        }
        if (speedValue >= 30) {
            return 1.4;
        }
        if (speedValue >= 20) {
            return 1.1;
        }
        if (speedValue >= 10) {
            return 0.85;
        }
        return DEFAULT_SPEED_TILES_PER_SECOND;
    }

    private double resolveRange(Card referenceCard) {
        if (referenceCard == null || referenceCard.getStats() == null) {
            return 1.5;
        }
        return Math.max(0.5, referenceCard.getStats().getRange() / 10.0);
    }

    private int resolveDamage(Card referenceCard) {
        if (referenceCard == null || referenceCard.getStats() == null) {
            return 25;
        }
        return Math.max(10, referenceCard.getStats().getDamage());
    }

    private double resolveAttackInterval(Card referenceCard) {
        if (referenceCard == null || referenceCard.getStats() == null) {
            return 1.0;
        }
        int hitSpeedMillis = referenceCard.getStats().getHitSpeedMillis();
        if (hitSpeedMillis <= 0) {
            return 1.0;
        }
        return hitSpeedMillis / 1000.0;
    }

    private boolean isSpawnerBuilding() {
        if (card == null) {
            return false;
        }
        String cardId = card.getId();
        return "card_tombstone".equals(cardId) || "card_goblin_hut".equals(cardId) 
            || "card_barbarian_hut".equals(cardId);
    }

    private boolean isElixirCollector() {
        return card != null && "card_elixir_collector".equals(card.getId());
    }

    private void spawnUnits(Arena arena) {
        if (arena == null || card == null || position == null || owner == null) {
            return;
        }
        String cardId = card.getId();
        CardCatalogRepository cardRepo = new CardCatalogRepository();
        List<Card> allCards = cardRepo.findAll();
        Card spawnCard = null;
        int spawnCount = 0;
        double spawnInterval = 0;

        if ("card_tombstone".equals(cardId)) {
            // Spawns 1 skeleton every 4.9s, Lifetime: 60s
            // Note: "card_skeletons" is a swarm card that spawns 4 units, but tombstone spawns 1 skeleton
            // We need to create a single skeleton unit, not the swarm card
            spawnCard = allCards.stream().filter(c -> "card_skeletons".equals(c.getId())).findFirst().orElse(null);
            spawnCount = 1; // Spawn 1 skeleton (the card itself represents multiple, but we spawn 1)
            spawnInterval = 4.9;
        } else if ("card_goblin_hut".equals(cardId)) {
            // Spawns 1 spear goblin every 4.9s, Lifetime: 60s
            spawnCard = allCards.stream().filter(c -> "card_spear_goblins".equals(c.getId())).findFirst().orElse(null);
            spawnCount = 1; // Spawn 1 spear goblin (the card spawns 3, but hut spawns 1)
            spawnInterval = 4.9;
        } else if ("card_barbarian_hut".equals(cardId)) {
            // Spawns 2 barbarians every 14s, Lifetime: 60s
            spawnCard = allCards.stream().filter(c -> "card_barbarians".equals(c.getId())).findFirst().orElse(null);
            spawnCount = 2; // Spawn 2 barbarians (the card spawns 4, but hut spawns 2)
            spawnInterval = 14.0;
        }

        if (spawnCard == null) {
            return;
        }

        // Get unit HP from the card stats
        // Note: The card stats represent individual unit stats, not total for the swarm
        int individualHp = spawnCard.getStats() != null ? spawnCard.getStats().getHp() : 0;

        // Spawn units near the building
        for (int i = 0; i < spawnCount; i++) {
            // Try to find a free position near the building
            Position spawnPos = findSpawnPosition(arena, position, i);
            if (spawnPos != null && arena.isTileFree(spawnPos)) {
                Unit spawnedUnit = new Unit(spawnCard, spawnPos, individualHp, owner);
                Tower initialTarget = arena.findNearestEnemyTower(owner, spawnPos);
                spawnedUnit.setTargetTower(initialTarget);
                arena.addUnit(spawnedUnit);
            }
        }

        spawnCooldownSeconds = spawnInterval;
        
        // Check lifetime (60 seconds for spawner buildings)
        if (lifetimeSeconds >= 60.0) {
            currentHP = 0; // Destroy after lifetime
        }
    }

    private Position findSpawnPosition(Arena arena, Position buildingPos, int index) {
        if (arena == null || buildingPos == null) {
            return buildingPos;
        }
        // Try positions around the building
        int[][] offsets = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        if (index < offsets.length) {
            int[] offset = offsets[index];
            Position candidate = new Position(buildingPos.getX() + offset[0], buildingPos.getY() + offset[1]);
            if (arena.isWithinBounds(candidate)) {
                return candidate;
            }
        }
        // Fallback to building position if no valid offset found
        return buildingPos;
    }

    private void generateElixir(Arena arena) {
        if (arena == null || owner == null) {
            return;
        }
        // Find the player who owns this elixir collector
        // This requires access to Match, so we'll handle it in Match.advanceTime
        // For now, we'll just mark that elixir should be generated
        // The actual generation will be handled in Match class
    }

    /**
     * Gets the splash radius for AoE attacks. Returns 0 if unit doesn't have AoE.
     */
    private double getSplashRadius() {
        if (card == null) {
            return 0;
        }
        String cardId = card.getId();
        
        // Troops with AoE
        if ("card_bomber".equals(cardId)) {
            return 1.5; // Bomber splash radius
        } else if ("card_valkyrie".equals(cardId)) {
            return 1.0; // Valkyrie melee AoE radius
        } else if ("card_wizard".equals(cardId)) {
            return 1.5; // Wizard fireball splash radius
        }
        
        // Buildings with AoE
        if ("card_mortar".equals(cardId)) {
            return 5.0; // Mortar splash radius
        } else if ("card_bomb_tower".equals(cardId)) {
            return 1.8; // Bomb Tower splash radius
        }
        
        return 0; // No AoE
    }

    /**
     * Performs an area of effect attack, damaging all enemies within the splash radius.
     */
    private void performAoEAttack(Arena arena, Position center, double splashRadius, int damage) {
        if (arena == null || center == null || splashRadius <= 0) {
            return;
        }
        
        // Find all enemy units in splash radius
        List<Unit> unitsInRadius = arena.findUnitsInRadius(center, splashRadius);
        for (Unit unit : unitsInRadius) {
            if (unit != null && !unit.isDefeated() && unit.getOwner() != owner) {
                // Check if we can attack this unit (respects ground/flying restrictions)
                if (canAttackUnit(unit)) {
                    unit.takeDamage(damage);
                }
            }
        }
        
        // Also damage towers in splash radius when this unit can attack buildings.
        if (card != null && card.getTarget() != CardTarget.NONE) {
            List<Tower> towersInRadius = arena.findTowersInRadius(center, splashRadius);
            for (Tower tower : towersInRadius) {
                if (tower != null && !tower.isDestroyed() && tower.getOwner() != owner) {
                    tower.takeDamage(damage);
                }
            }
        }
    }
}
