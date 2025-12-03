package kuroyale.domain;

public class CardStats {

    private int hp;
    private int damage;
    private int range;
    private int speed;
    private int attackSpeed;

    public CardStats() {
    }

    public CardStats(int hp, int damage, int range, int speed, int attackSpeed) {
        this.hp = hp;
        this.damage = damage;
        this.range = range;
        this.speed = speed;
        this.attackSpeed = attackSpeed;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getAttackSpeed() {
        return attackSpeed;
    }

    public void setAttackSpeed(int attackSpeed) {
        this.attackSpeed = attackSpeed;
    }
}




