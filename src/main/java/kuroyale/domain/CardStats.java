package kuroyale.domain;

public class CardStats {

    private int hp;
    private int damage;
    private int range;
    private int moveSpeed;
    private int hitSpeedMillis;
    private int spawnCount;

    public CardStats() {
        this.spawnCount = 1; // Default to 1 unit
    }

    public CardStats(int hp, int damage, int range, int moveSpeed, int hitSpeedMillis) {
        this(hp, damage, range, moveSpeed, hitSpeedMillis, 1);
    }

    public CardStats(int hp, int damage, int range, int moveSpeed, int hitSpeedMillis, int spawnCount) {
        this.hp = hp;
        this.damage = damage;
        this.range = range;
        this.moveSpeed = moveSpeed;
        this.hitSpeedMillis = hitSpeedMillis;
        this.spawnCount = spawnCount > 0 ? spawnCount : 1;
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

    public int getSpawnCount() {
        return spawnCount > 0 ? spawnCount : 1;
    }

    public void setSpawnCount(int spawnCount) {
        this.spawnCount = spawnCount > 0 ? spawnCount : 1;
    }
}




