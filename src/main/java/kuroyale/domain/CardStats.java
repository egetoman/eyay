package kuroyale.domain;

public class CardStats {

    private int hp;
    private int damage;
    private int range;
    private int moveSpeed;
    private int hitSpeedMillis;

    public CardStats() {
    }

    public CardStats(int hp, int damage, int range, int moveSpeed, int hitSpeedMillis) {
        this.hp = hp;
        this.damage = damage;
        this.range = range;
        this.moveSpeed = moveSpeed;
        this.hitSpeedMillis = hitSpeedMillis;
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

    public int getMoveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(int moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public int getHitSpeedMillis() {
        return hitSpeedMillis;
    }

    public void setHitSpeedMillis(int hitSpeedMillis) {
        this.hitSpeedMillis = hitSpeedMillis;
    }
}




