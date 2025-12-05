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
        if (arena == null || deltaSeconds <= 0 || speedTilesPerSecond <= 0 || isDefeated()) {
            return;
        }
        attackCooldownSeconds = Math.max(0, attackCooldownSeconds - deltaSeconds);

        acquireTargets(arena);
        Position targetPosition = null;
        boolean attackingTower = false;
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

        Position travelTarget = arena.resolvePathTarget(position, targetPosition);
        double dx = travelTarget.getX() - preciseX;
        double dy = travelTarget.getY() - preciseY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        boolean headingToFinalTarget = travelTarget == targetPosition;
        boolean inRange = headingToFinalTarget && distance <= attackRangeTiles;

        if (!inRange) {
            double step = speedTilesPerSecond * deltaSeconds;
            if (distance <= step || distance == 0) {
                preciseX = travelTarget.getX();
                preciseY = travelTarget.getY();
            } else {
                preciseX += (dx / distance) * step;
                preciseY += (dy / distance) * step;
            }
        } else if (attackCooldownSeconds <= 0 && attackDamage > 0) {
            if (attackingTower) {
                targetTower.takeDamage(attackDamage);
            } else if (targetEnemyUnit != null) {
                targetEnemyUnit.takeDamage(attackDamage);
                if (targetEnemyUnit.isDefeated()) {
                    targetEnemyUnit = null;
                }
            }
            attackCooldownSeconds = attackIntervalSeconds;
        }

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
        double detectionRadius = Math.max(attackRangeTiles * 2, 3.0);
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




