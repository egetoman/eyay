package kuroyale.domain;

public class Player {

    private String name;
    private Deck deck;
    private int trophies;

    public Player() {
    }

    public Player(String name, Deck deck, int trophies) {
        this.name = name;
        this.deck = deck;
        this.trophies = trophies;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Deck getDeck() {
        return deck;
    }

    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    public int getTrophies() {
        return trophies;
    }

    public void setTrophies(int trophies) {
        this.trophies = trophies;
    }
}




