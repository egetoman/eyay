package kuroyale.domain;

public class Unit {

    private Card card;
    private Position position;
    private int currentHP;
    private PlayerSide owner;

    public Unit() {
    }

    public Unit(Card card, Position position, int currentHP) {
        this(card, position, currentHP, PlayerSide.PLAYER);
    }

    public Unit(Card card, Position position, int currentHP, PlayerSide owner) {
        this.card = card;
        this.position = position;
        this.currentHP = currentHP;
        this.owner = owner;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public void setCurrentHP(int currentHP) {
        this.currentHP = currentHP;
    }

    public PlayerSide getOwner() {
        return owner;
    }

    public void setOwner(PlayerSide owner) {
        this.owner = owner;
    }

    public void moveTo(Position target) {
        // TODO: implement movement behavior
    }

    public void takeDamage(int amount) {
        currentHP = Math.max(0, currentHP - amount);
    }
}

