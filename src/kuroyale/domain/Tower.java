package kuroyale.domain;

public class Tower {

    private int hp;
    private Position position;
    private int damage;
    private double attackSpeed;

    public Tower() {
    }

    public Tower(int hp, Position position, int damage, double attackSpeed) {
        this.hp = hp;
        this.position = position;
        this.damage = damage;
        this.attackSpeed = attackSpeed;
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
}




