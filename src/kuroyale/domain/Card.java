package kuroyale.domain;

public class Card {

    private String id;
    private String name;
    private int elixirCost;
    private CardType type;
    private CardStats stats;

    public Card() {
    }

    public Card(String id, String name, int elixirCost, CardType type, CardStats stats) {
        this.id = id;
        this.name = name;
        this.elixirCost = elixirCost;
        this.type = type;
        this.stats = stats;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getElixirCost() {
        return elixirCost;
    }

    public void setElixirCost(int elixirCost) {
        this.elixirCost = elixirCost;
    }

    public CardType getType() {
        return type;
    }

    public void setType(CardType type) {
        this.type = type;
    }

    public CardStats getStats() {
        return stats;
    }

    public void setStats(CardStats stats) {
        this.stats = stats;
    }
}




