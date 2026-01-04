package kuroyale.domain;

/**
 * Represents the 8 different combo types in the game.
 * Each combo type has specific card requirements and effects.
 */
public enum ComboType {
    TANK_SUPPORT("Tank + Support", "Play Giant or Knight, then play any ranged troop within 5 seconds"),
    SPELL_SYNERGY("Spell Synergy", "Play any two different spells within 5 seconds"),
    SWARM_ATTACK("Swarm Attack", "Play any two swarm cards within 5 seconds"),
    BUILDING_DEFENSE("Building Defense", "Play any two building cards within 5 seconds"),
    AIR_ASSAULT("Air Assault", "Play Minions and Minion Horde within 5 seconds"),
    ROYAL_COMBO("Royal Combo", "Play Knight and Archers within 5 seconds"),
    SIEGE_MODE("Siege Mode", "Play Mortar and any defensive building within 5 seconds"),
    RUSH_ATTACK("Rush Attack", "Play Hog Rider and any low-cost card (1-2 Elixir) within 5 seconds");

    private final String displayName;
    private final String description;

    ComboType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}

