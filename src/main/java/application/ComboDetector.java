package application;

import kuroyale.domain.Card;
import kuroyale.domain.ComboDefinition;
import kuroyale.domain.ComboType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Detects card combos based on recent card plays.
 * Uses Observer pattern to notify listeners when combos are triggered.
 * 
 * Tracks the last 10 card plays with timestamps and detects combos
 * when cards are played within 5 seconds of each other.
 */
public class ComboDetector {
    private static final int MAX_TRACKED_PLAYS = 10;
    private static final double COMBO_WINDOW_SECONDS = 5.0;

    private final List<ComboDefinition> comboDefinitions;
    private final List<ComboDefinition.CardPlay> recentPlays;
    /**
     * Unique combos triggered at least once in the match. Used for combo counter + rewards.
     */
    private final Set<ComboType> triggeredCombos;
    /**
     * Cooldown per combo type to enforce: "Each combo can only trigger once per 5-second window".
     * A combo can re-trigger after COMBO_WINDOW_SECONDS has passed since its last trigger.
     */
    private final Map<ComboType, Double> lastTriggeredAtByType;

    /**
     * Listener interface for combo events.
     */
    public interface ComboListener {
        void onComboTriggered(ComboType comboType, String comboName, String effectDescription);
    }

    private ComboListener listener;

    public ComboDetector() {
        this.comboDefinitions = ComboDefinition.createAllDefinitions();
        this.recentPlays = new ArrayList<>();
        this.triggeredCombos = new HashSet<>();
        this.lastTriggeredAtByType = new EnumMap<>(ComboType.class);
    }

    /**
     * Sets the listener to be notified when combos are triggered.
     */
    public void setListener(ComboListener listener) {
        this.listener = listener;
    }

    /**
     * Records a card play and checks for combos.
     * @param card The card that was played
     * @param timestamp The game time when the card was played
     * @return The triggered combo type, or null if no combo was triggered
     */
    public ComboType recordCardPlay(Card card, double timestamp) {
        if (card == null) {
            return null;
        }
        
        // Add the new play
        ComboDefinition.CardPlay newPlay = new ComboDefinition.CardPlay(card, timestamp);
        recentPlays.add(newPlay);

        // Keep only the last MAX_TRACKED_PLAYS plays
        if (recentPlays.size() > MAX_TRACKED_PLAYS) {
            recentPlays.remove(0);
        }

        // Clean up plays older than the combo window
        cleanOldPlays(timestamp);

        // Check for combos
        return checkForCombos(timestamp);
    }

    /**
     * Updates the current game time (used for cleanup).
     */
    public void updateTime(double currentTime) {
        cleanOldPlays(currentTime);
    }

    /**
     * Resets the detector for a new match.
     */
    public void reset() {
        recentPlays.clear();
        triggeredCombos.clear();
        lastTriggeredAtByType.clear();
    }

    /**
     * Gets the set of combos that have been triggered in this match.
     */
    public Set<ComboType> getTriggeredCombos() {
        return new HashSet<>(triggeredCombos);
    }

    /**
     * Gets the count of unique combos triggered.
     */
    public int getTriggeredComboCount() {
        return triggeredCombos.size();
    }

    /**
     * Gets all available combo definitions.
     */
    public List<ComboDefinition> getAllComboDefinitions() {
        return new ArrayList<>(comboDefinitions);
    }

    /**
     * Gets combos that are possible with the given deck.
     */
    public List<ComboDefinition> getPossibleCombos(List<Card> deckCards) {
        if (deckCards == null || deckCards.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> cardIds = new HashSet<>();
        for (Card card : deckCards) {
            if (card != null && card.getId() != null) {
                cardIds.add(card.getId());
            }
        }

        List<ComboDefinition> possible = new ArrayList<>();
        for (ComboDefinition def : comboDefinitions) {
            if (canTriggerCombo(def, cardIds)) {
                possible.add(def);
            }
        }
        return possible;
    }

    private void cleanOldPlays(double currentTime) {
        double cutoffTime = currentTime - COMBO_WINDOW_SECONDS;
        recentPlays.removeIf(play -> play.getTimestamp() < cutoffTime);
    }

    private ComboType checkForCombos(double timestamp) {
        // Get plays within the combo window
        List<ComboDefinition.CardPlay> windowPlays = getPlaysInWindow(timestamp);

        // Check each combo definition
        for (ComboDefinition def : comboDefinitions) {
            // Enforce "once per 5-second window" (cooldown)
            Double lastAt = lastTriggeredAtByType.get(def.getType());
            if (lastAt != null && (timestamp - lastAt) < COMBO_WINDOW_SECONDS) {
                continue;
            }

            if (def.canTrigger(windowPlays)) {
                triggeredCombos.add(def.getType());
                lastTriggeredAtByType.put(def.getType(), timestamp);
                if (listener != null) {
                    listener.onComboTriggered(
                        def.getType(),
                        def.getType().getDisplayName(),
                        def.getEffectDescription()
                    );
                }
                return def.getType();
            }
        }

        return null;
    }

    private List<ComboDefinition.CardPlay> getPlaysInWindow(double currentTime) {
        List<ComboDefinition.CardPlay> windowPlays = new ArrayList<>();
        double windowStart = currentTime - COMBO_WINDOW_SECONDS;

        for (ComboDefinition.CardPlay play : recentPlays) {
            if (play.getTimestamp() >= windowStart) {
                windowPlays.add(play);
            }
        }

        return windowPlays;
    }

    private boolean canTriggerCombo(ComboDefinition def, Set<String> deckCardIds) {
        // For simplicity, we check if the deck contains cards that could potentially
        // trigger the combo. This is a simplified check - actual detection happens during gameplay.
        // This method is used for the "possible combos" UI, so we can be lenient.
        ComboType type = def.getType();
        
        switch (type) {
            case TANK_SUPPORT:
                return (deckCardIds.contains("card_giant") || deckCardIds.contains("card_knight"))
                    && (deckCardIds.contains("card_musketeer") || deckCardIds.contains("card_archers")
                        || deckCardIds.contains("card_spear_goblins") || deckCardIds.contains("card_wizard"));
            case SPELL_SYNERGY:
                long spellCount = deckCardIds.stream()
                    .filter(id -> id.equals("card_zap") || id.equals("card_arrows")
                        || id.equals("card_fireball") || id.equals("card_rocket"))
                    .count();
                return spellCount >= 2;
            case SWARM_ATTACK:
                long swarmCount = deckCardIds.stream()
                    .filter(id -> id.equals("card_skeletons") || id.equals("card_goblins")
                        || id.equals("card_spear_goblins") || id.equals("card_archers")
                        || id.equals("card_minions") || id.equals("card_minion_horde")
                        || id.equals("card_barbarians"))
                    .count();
                return swarmCount >= 2;
            case BUILDING_DEFENSE:
                long buildingCount = deckCardIds.stream()
                    .filter(this::isBuilding)
                    .count();
                return buildingCount >= 2;
            case AIR_ASSAULT:
                return deckCardIds.contains("card_minions") && deckCardIds.contains("card_minion_horde");
            case ROYAL_COMBO:
                return deckCardIds.contains("card_knight") && deckCardIds.contains("card_archers");
            case SIEGE_MODE:
                return deckCardIds.contains("card_mortar")
                    && (deckCardIds.contains("card_cannon") || deckCardIds.contains("card_tesla")
                        || deckCardIds.contains("card_bomb_tower") || deckCardIds.contains("card_inferno_tower"));
            case RUSH_ATTACK:
                return deckCardIds.contains("card_hog_rider")
                    && (deckCardIds.contains("card_skeletons") || deckCardIds.contains("card_goblins")
                        || deckCardIds.contains("card_spear_goblins") || deckCardIds.contains("card_zap"));
            default:
                return false;
        }
    }

    private boolean isBuilding(String cardId) {
        return cardId.equals("card_cannon") || cardId.equals("card_tesla")
            || cardId.equals("card_mortar") || cardId.equals("card_bomb_tower")
            || cardId.equals("card_inferno_tower") || cardId.equals("card_tombstone")
            || cardId.equals("card_goblin_hut") || cardId.equals("card_barbarian_hut")
            || cardId.equals("card_elixir_collector");
    }
}

