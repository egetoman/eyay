package kuroyale.domain;

public class Card {

    private String id;
    private String name;
    private int elixirCost;
    private CardType type;
    private CardStats stats;
    private CardTarget target;
    private UnitMovementType movementType;
    private String description;

    public Card() {
        this.movementType = UnitMovementType.GROUND; // Default to ground
    }

    public Card(String id, String name, int elixirCost, CardType type, CardStats stats, CardTarget target,
            String description) {
        this(id, name, elixirCost, type, stats, target, UnitMovementType.GROUND, description);
    }

    public Card(String id, String name, int elixirCost, CardType type, CardStats stats, CardTarget target,
            UnitMovementType movementType, String description) {
        this.id = id;
        this.name = name;
        this.elixirCost = elixirCost;
        this.type = type;
        this.stats = stats;
        this.target = target;
        this.movementType = movementType != null ? movementType : UnitMovementType.GROUND;
        this.description = description;
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

    public CardTarget getTarget() {
        return target;
    }

    public void setTarget(CardTarget target) {
        this.target = target;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UnitMovementType getMovementType() {
        return movementType != null ? movementType : UnitMovementType.GROUND;
    }

    public void setMovementType(UnitMovementType movementType) {
        this.movementType = movementType != null ? movementType : UnitMovementType.GROUND;
    }
}
