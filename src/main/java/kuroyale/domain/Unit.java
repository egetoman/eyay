package kuroyale.domain;

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

    public Unit() {
        this.speedTilesPerSecond = DEFAULT_SPEED_TILES_PER_SECOND;
        this.attackRangeTiles = 1.5;
        this.attackDamage = 50;
        this.attackIntervalSeconds = 1.0;
        this.attackCooldownSeconds = 0;
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

    public TowerOwner getOwner() {
        return owner;
    }

    public void setOwner(TowerOwner owner) {
        this.owner = owner;
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

    public void tick(Arena arena, double deltaSeconds) {
        // Buildings should never move or acquire targets - they are stationary
        if (card != null && card.getType() == CardType.BUILDING) {
            // Buildings can still attack if they have attack capabilities
            attackCooldownSeconds = Math.max(0, attackCooldownSeconds - deltaSeconds);

            // Buildings attack nearby enemies if they have damage
            if (attackDamage > 0 && attackCooldownSeconds <= 0 && attackRangeTiles > 0) {
                // Find nearest enemy in range
                Unit nearestEnemy = arena.findNearestEnemyUnit(owner, position, attackRangeTiles);
                if (nearestEnemy != null) {
                    nearestEnemy.takeDamage(attackDamage);
                    attackCooldownSeconds = attackIntervalSeconds;
                }
            }
            return; // Buildings don't move, so exit early
        }

        if (arena == null || deltaSeconds <= 0 || speedTilesPerSecond <= 0 || isDefeated()) {
            return;
        }
        attackCooldownSeconds = Math.max(0, attackCooldownSeconds - deltaSeconds);

        // Check if we're currently locked onto a tower (in range and attacking it)
        boolean lockedOntoTower = false;
        if (targetTower != null && !targetTower.isDestroyed() && targetTower.getPosition() != null) {
            double dx = targetTower.getPosition().getX() - preciseX;
            double dy = targetTower.getPosition().getY() - preciseY;
            double distanceToTower = Math.sqrt(dx * dx + dy * dy);
            lockedOntoTower = (distanceToTower <= attackRangeTiles);
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

        // Calculate path and distance
        Position travelTarget = arena.resolvePathTarget(position, targetPosition);
        double dx = travelTarget.getX() - preciseX;
        double dy = travelTarget.getY() - preciseY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        boolean headingToFinalTarget = travelTarget == targetPosition;
        boolean inRange = headingToFinalTarget && distance <= attackRangeTiles;

        // MOVEMENT: Move if not in attack range
        if (!inRange) {
            double step = speedTilesPerSecond * deltaSeconds;
            if (distance <= step) {
                // Smoothly arrive at target
                preciseX = travelTarget.getX();
                preciseY = travelTarget.getY();
            } else {
                // Move toward target
                preciseX += (dx / distance) * step;
                preciseY += (dy / distance) * step;
            }
        }

        // ATTACK: Attack if in range and cooldown ready (independent of movement)
        if (inRange && attackCooldownSeconds <= 0 && attackDamage > 0) {
            if (attackingTower) {
                targetTower.takeDamage(attackDamage);
            } else if (targetEnemyUnit != null) {
                targetEnemyUnit.takeDamage(attackDamage);
                // Immediately check if target was defeated and try to find new one
                if (targetEnemyUnit.isDefeated()) {
                    targetEnemyUnit = null;
                    // Try to immediately acquire a new enemy unit
                    double detectionRadius = Math.max(attackRangeTiles * 1.5, 2.5);
                    Unit newTarget = arena.findNearestEnemyUnit(owner, position, detectionRadius);
                    if (newTarget != null) {
                        targetEnemyUnit = newTarget;
                    }
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

    private void acquireTargets(Arena arena) {
        // Building-only units (Giant, Hog Rider) should never target enemy troops
        if (card != null && card.getTarget() == CardTarget.BUILDINGS) {
            targetEnemyUnit = null; // Always null for building-only units
            return;
        }

        // Normal targeting logic for units that can attack troops
        // Reduced detection radius for more natural engagement (1.5x instead of 2x)
        double detectionRadius = Math.max(attackRangeTiles * 1.5, 2.5);
        if (targetEnemyUnit == null || targetEnemyUnit.isDefeated()) {
            Unit candidate = arena.findNearestEnemyUnit(owner, position, detectionRadius);
            if (candidate != null) {
                targetEnemyUnit = candidate;
                targetTower = null;
            }
        } else {
            double dx = targetEnemyUnit.getPreciseX() - preciseX;
            double dy = targetEnemyUnit.getPreciseY() - preciseY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance > detectionRadius * 1.5) {
                targetEnemyUnit = null;
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
}
