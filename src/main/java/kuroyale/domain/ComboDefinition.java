package kuroyale.domain;

import java.util.List;
import java.util.Arrays;

/**
 * Defines the trigger conditions and effects for a combo type.
 * Uses Strategy pattern to encapsulate combo detection logic.
 */
public class ComboDefinition {
    private final ComboType type;
    private final ComboTrigger trigger;
    private final String effectDescription;

    public ComboDefinition(ComboType type, ComboTrigger trigger, String effectDescription) {
        this.type = type;
        this.trigger = trigger;
        this.effectDescription = effectDescription;
    }

    public ComboType getType() {
        return type;
    }

    public ComboTrigger getTrigger() {
        return trigger;
    }

    public String getEffectDescription() {
        return effectDescription;
    }

    /**
     * Checks if a combo can be triggered by the given card plays.
     */
    public boolean canTrigger(List<CardPlay> recentPlays) {
        return trigger.matches(recentPlays);
    }

    /**
     * Interface for combo trigger conditions.
     */
    public interface ComboTrigger {
        boolean matches(List<CardPlay> recentPlays);
    }

    /**
     * Represents a card play with timestamp.
     */
    public static class CardPlay {
        private final Card card;
        private final double timestamp;

        public CardPlay(Card card, double timestamp) {
            this.card = card;
            this.timestamp = timestamp;
        }

        public Card getCard() {
            return card;
        }

        public String getCardId() {
            return card != null && card.getId() != null ? card.getId() : "";
        }

        public String getCardName() {
            return card != null && card.getName() != null ? card.getName() : "";
        }

        public double getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Factory method to create all combo definitions.
     */
    public static List<ComboDefinition> createAllDefinitions() {
        return Arrays.asList(
            // Tank + Support: Giant or Knight, then any ranged troop
            new ComboDefinition(
                ComboType.TANK_SUPPORT,
                (plays) -> {
                    if (plays.size() < 2) return false;
                    CardPlay latest = plays.get(plays.size() - 1);
                    double windowStart = latest.getTimestamp() - 5.0;
                    for (int i = plays.size() - 2; i >= 0; i--) {
                        CardPlay play = plays.get(i);
                        if (play.getTimestamp() < windowStart) break;
                        if (isTank(play.getCardId()) && isRangedTroop(latest.getCardId())) {
                            return true;
                        }
                    }
                    return false;
                },
                "Ranged troop gains +15% damage"
            ),
            // Spell Synergy: Any two different spells
            new ComboDefinition(
                ComboType.SPELL_SYNERGY,
                (plays) -> {
                    if (plays.size() < 2) return false;
                    CardPlay latest = plays.get(plays.size() - 1);
                    if (!isSpell(latest.getCardId())) return false;
                    double windowStart = latest.getTimestamp() - 5.0;
                    for (int i = plays.size() - 2; i >= 0; i--) {
                        CardPlay play = plays.get(i);
                        if (play.getTimestamp() < windowStart) break;
                        if (isSpell(play.getCardId()) && !play.getCardId().equals(latest.getCardId())) {
                            return true;
                        }
                    }
                    return false;
                },
                "Second spell costs 1 less Elixir (refund 1 Elixir)"
            ),
            // Swarm Attack: Any two swarm cards
            new ComboDefinition(
                ComboType.SWARM_ATTACK,
                (plays) -> {
                    if (plays.size() < 2) return false;
                    CardPlay latest = plays.get(plays.size() - 1);
                    if (!isSwarm(latest.getCardId())) return false;
                    double windowStart = latest.getTimestamp() - 5.0;
                    for (int i = plays.size() - 2; i >= 0; i--) {
                        CardPlay play = plays.get(i);
                        if (play.getTimestamp() < windowStart) break;
                        if (isSwarm(play.getCardId())) {
                            return true;
                        }
                    }
                    return false;
                },
                "All swarm units gain +10% movement speed"
            ),
            // Building Defense: Any two buildings
            new ComboDefinition(
                ComboType.BUILDING_DEFENSE,
                (plays) -> {
                    if (plays.size() < 2) return false;
                    CardPlay latest = plays.get(plays.size() - 1);
                    if (!isBuilding(latest.getCardId())) return false;
                    double windowStart = latest.getTimestamp() - 5.0;
                    for (int i = plays.size() - 2; i >= 0; i--) {
                        CardPlay play = plays.get(i);
                        if (play.getTimestamp() < windowStart) break;
                        if (isBuilding(play.getCardId())) {
                            return true;
                        }
                    }
                    return false;
                },
                "Both buildings gain +20% HP"
            ),
            // Air Assault: Minions and Minion Horde
            new ComboDefinition(
                ComboType.AIR_ASSAULT,
                (plays) -> {
                    if (plays.size() < 2) return false;
                    CardPlay latest = plays.get(plays.size() - 1);
                    boolean hasMinions = latest.getCardId().equals("card_minions");
                    boolean hasHorde = latest.getCardId().equals("card_minion_horde");
                    if (!hasMinions && !hasHorde) return false;
                    double windowStart = latest.getTimestamp() - 5.0;
                    for (int i = plays.size() - 2; i >= 0; i--) {
                        CardPlay play = plays.get(i);
                        if (play.getTimestamp() < windowStart) break;
                        if (hasMinions && play.getCardId().equals("card_minion_horde")) return true;
                        if (hasHorde && play.getCardId().equals("card_minions")) return true;
                    }
                    return false;
                },
                "All air units gain +15% damage"
            ),
            // Royal Combo: Knight and Archers
            new ComboDefinition(
                ComboType.ROYAL_COMBO,
                (plays) -> {
                    if (plays.size() < 2) return false;
                    CardPlay latest = plays.get(plays.size() - 1);
                    boolean hasKnight = latest.getCardId().equals("card_knight");
                    boolean hasArchers = latest.getCardId().equals("card_archers");
                    if (!hasKnight && !hasArchers) return false;
                    double windowStart = latest.getTimestamp() - 5.0;
                    for (int i = plays.size() - 2; i >= 0; i--) {
                        CardPlay play = plays.get(i);
                        if (play.getTimestamp() < windowStart) break;
                        if (hasKnight && play.getCardId().equals("card_archers")) return true;
                        if (hasArchers && play.getCardId().equals("card_knight")) return true;
                    }
                    return false;
                },
                "Knight gains +100 HP"
            ),
            // Siege Mode: Mortar and defensive building
            new ComboDefinition(
                ComboType.SIEGE_MODE,
                (plays) -> {
                    if (plays.size() < 2) return false;
                    CardPlay latest = plays.get(plays.size() - 1);
                    boolean hasMortar = latest.getCardId().equals("card_mortar");
                    boolean hasDefBuilding = isDefensiveBuilding(latest.getCardId());
                    if (!hasMortar && !hasDefBuilding) return false;
                    double windowStart = latest.getTimestamp() - 5.0;
                    for (int i = plays.size() - 2; i >= 0; i--) {
                        CardPlay play = plays.get(i);
                        if (play.getTimestamp() < windowStart) break;
                        if (hasMortar && isDefensiveBuilding(play.getCardId())) return true;
                        if (hasDefBuilding && play.getCardId().equals("card_mortar")) return true;
                    }
                    return false;
                },
                "Mortar range increased by +2 tiles"
            ),
            // Rush Attack: Hog Rider and low-cost card (1-2 Elixir)
            new ComboDefinition(
                ComboType.RUSH_ATTACK,
                (plays) -> {
                    if (plays.size() < 2) return false;
                    CardPlay latest = plays.get(plays.size() - 1);
                    boolean hasHogRider = latest.getCardId().equals("card_hog_rider");
                    boolean isLowCost = isLowCostCard(latest.getCardId());
                    if (!hasHogRider && !isLowCost) return false;
                    double windowStart = latest.getTimestamp() - 5.0;
                    for (int i = plays.size() - 2; i >= 0; i--) {
                        CardPlay play = plays.get(i);
                        if (play.getTimestamp() < windowStart) break;
                        if (hasHogRider && isLowCostCard(play.getCardId())) return true;
                        if (isLowCost && play.getCardId().equals("card_hog_rider")) return true;
                    }
                    return false;
                },
                "Hog Rider speed increased by +20%"
            )
        );
    }

    // Helper methods to identify card types
    private static boolean isTank(String cardId) {
        return cardId.equals("card_giant") || cardId.equals("card_knight");
    }

    private static boolean isRangedTroop(String cardId) {
        return cardId.equals("card_musketeer") || cardId.equals("card_archers") 
            || cardId.equals("card_spear_goblins") || cardId.equals("card_wizard");
    }

    private static boolean isSpell(String cardId) {
        return cardId.equals("card_zap") || cardId.equals("card_arrows") 
            || cardId.equals("card_fireball") || cardId.equals("card_rocket");
    }

    private static boolean isSwarm(String cardId) {
        return cardId.equals("card_skeletons") || cardId.equals("card_goblins") 
            || cardId.equals("card_spear_goblins") || cardId.equals("card_archers")
            || cardId.equals("card_minions") || cardId.equals("card_minion_horde")
            || cardId.equals("card_barbarians");
    }

    private static boolean isBuilding(String cardId) {
        return cardId.startsWith("card_") && (
            cardId.equals("card_cannon") || cardId.equals("card_tesla")
            || cardId.equals("card_mortar") || cardId.equals("card_bomb_tower")
            || cardId.equals("card_inferno_tower") || cardId.equals("card_tombstone")
            || cardId.equals("card_goblin_hut") || cardId.equals("card_barbarian_hut")
            || cardId.equals("card_elixir_collector")
        );
    }

    private static boolean isDefensiveBuilding(String cardId) {
        return cardId.equals("card_cannon") || cardId.equals("card_tesla")
            || cardId.equals("card_bomb_tower") || cardId.equals("card_inferno_tower");
    }

    private static boolean isLowCostCard(String cardId) {
        // Cards with 1-2 Elixir cost
        return cardId.equals("card_skeletons") || cardId.equals("card_goblins")
            || cardId.equals("card_spear_goblins") || cardId.equals("card_zap");
    }
}

