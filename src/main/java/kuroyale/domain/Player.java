package kuroyale.domain;

public class Player {

    private static final int DEFAULT_MAX_ELIXIR = 10;

    private String name;
    private Deck deck;
    private int trophies;
    private int currentElixir;
    private int maxElixir;

    public Player() {
        this.currentElixir = DEFAULT_MAX_ELIXIR / 2;
        this.maxElixir = DEFAULT_MAX_ELIXIR;
    }

    public Player(String name, Deck deck, int trophies) {
        this(name, deck, trophies, DEFAULT_MAX_ELIXIR / 2, DEFAULT_MAX_ELIXIR);
    }

    public Player(String name, Deck deck, int trophies, int currentElixir, int maxElixir) {
        this.name = name;
        this.deck = deck;
        this.trophies = trophies;
        this.currentElixir = Math.min(currentElixir, maxElixir);
        this.maxElixir = maxElixir;
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

    public int getCurrentElixir() {
        return currentElixir;
    }

    public void setCurrentElixir(int currentElixir) {
        this.currentElixir = Math.max(0, Math.min(currentElixir, maxElixir));
    }

    public int getMaxElixir() {
        return maxElixir;
    }

    public void setMaxElixir(int maxElixir) {
        this.maxElixir = Math.max(1, maxElixir);
        if (currentElixir > this.maxElixir) {
            currentElixir = this.maxElixir;
        }
    }

    public boolean hasEnoughElixir(int cost) {
        return currentElixir >= cost;
    }

    public void spendElixir(int cost) {
        if (cost <= 0) {
            return;
        }
        currentElixir = Math.max(0, currentElixir - cost);
    }

    public void regenerateElixir(int amount) {
        if (amount <= 0) {
            return;
        }
        currentElixir = Math.min(maxElixir, currentElixir + amount);
    }
}




