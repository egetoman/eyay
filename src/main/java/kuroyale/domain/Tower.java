package kuroyale.domain;

public class Tower {

    private int hp;
    private int maxHp;
    private Position position;
    private int damage;
    /**
     * Attack interval in seconds (e.g., 0.8 means fires every 0.8s).
     */
    private double attackSpeed;
    private TowerType type;
    private TowerOwner owner;
    private double attackCooldownSeconds;

    public Tower() {
    }

    public Tower(int hp, Position position, int damage, double attackSpeed, TowerType type, TowerOwner owner) {
        this.hp = hp;
        this.maxHp = hp;
        this.position = position;
        this.damage = damage;
        this.attackSpeed = attackSpeed;
        this.type = type;
        this.owner = owner;
        this.attackCooldownSeconds = 0;
    }

    public Tower(Tower other) {
        this(
                other.hp,
                other.position != null ? new Position(other.position.getX(), other.position.getY()) : null,
                other.damage,
                other.attackSpeed,
                other.type,
                other.owner);
        this.maxHp = other.maxHp;
        this.attackCooldownSeconds = other.attackCooldownSeconds;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public double getAttackSpeed() {
        return attackSpeed;
    }

    public void setAttackSpeed(double attackSpeed) {
        this.attackSpeed = attackSpeed;
    }

    public TowerType getType() {
        return type;
    }

    public void setType(TowerType type) {
        this.type = type;
    }

    public TowerOwner getOwner() {
        return owner;
    }

    public void setOwner(TowerOwner owner) {
        this.owner = owner;
    }

    public boolean isDestroyed() {
        return hp <= 0;
    }

    public void takeDamage(int amount) {
        if (amount <= 0) {
            return;
        }
        hp = Math.max(0, hp - amount);
    }

    /**
     * Tower auto-attack tick.
     * Towers automatically fire at the nearest enemy unit in range when their cooldown is ready.
     */
    public void tick(Arena arena, double deltaSeconds) {
        if (arena == null || deltaSeconds <= 0) {
            return;
        }
        if (isDestroyed() || owner == null || position == null) {
            return;
        }
        if (damage <= 0) {
            return;
        }
        double interval = attackSpeed > 0 ? attackSpeed : 1.0;
        attackCooldownSeconds = Math.max(0, attackCooldownSeconds - deltaSeconds);
        if (attackCooldownSeconds > 0) {
            return;
        }
        double range = resolveAttackRangeTiles();
        Unit target = arena.findNearestEnemyUnit(owner, position, range);
        if (target != null) {
            target.takeDamage(damage);
            attackCooldownSeconds = interval;
        }
    }

    private double resolveAttackRangeTiles() {
        // Reasonable defaults for this grid-based arena.
        // (Princess/Crown towers typically have slightly longer range than king.)
        if (type == TowerType.CROWN) {
            return 7.5;
        }
        if (type == TowerType.KING) {
            return 7.0;
        }
        return 7.0;
    }
}
