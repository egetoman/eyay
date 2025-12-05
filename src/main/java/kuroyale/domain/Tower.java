package kuroyale.domain;

public class Tower {

    private int hp;
    private Position position;
    private int damage;
    private double attackSpeed;
    private TowerType type;
    private TowerOwner owner;

    public Tower() {
    }

    public Tower(int hp, Position position, int damage, double attackSpeed, TowerType type, TowerOwner owner) {
        this.hp = hp;
        this.position = position;
        this.damage = damage;
        this.attackSpeed = attackSpeed;
        this.type = type;
        this.owner = owner;
    }

    public Tower(Tower other) {
        this(
            other.hp,
            other.position != null ? new Position(other.position.getX(), other.position.getY()) : null,
            other.damage,
            other.attackSpeed,
            other.type,
            other.owner
        );
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
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
}




