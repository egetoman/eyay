package kuroyale.domain;

public class Unit {

    private Card card;
    private Position position;
    private int currentHP;

    public Unit() {
    }

    public Unit(Card card, Position position, int currentHP) {
        this.card = card;
        this.position = position;
        this.currentHP = currentHP;
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

    public void moveTo(Position target) {
        // TODO: implement movement behavior
    }

    public void takeDamage(int amount) {
        // TODO: implement damage handling
    }
}




