package kuroyale.domain;

import java.util.HashMap;
import java.util.Map;

public final class CardRarityCatalog {
    private static final Map<String, Rarity> RARITY_BY_ID = new HashMap<>();

    static {
        // Common (12 total)
        RARITY_BY_ID.put("card_skeletons", Rarity.COMMON);
        RARITY_BY_ID.put("card_goblins", Rarity.COMMON);
        RARITY_BY_ID.put("card_spear_goblins", Rarity.COMMON);
        RARITY_BY_ID.put("card_archers", Rarity.COMMON);
        RARITY_BY_ID.put("card_knight", Rarity.COMMON);
        RARITY_BY_ID.put("card_bomber", Rarity.COMMON);
        RARITY_BY_ID.put("card_cannon", Rarity.COMMON);
        RARITY_BY_ID.put("card_tombstone", Rarity.COMMON);
        RARITY_BY_ID.put("card_arrows", Rarity.COMMON);
        RARITY_BY_ID.put("card_zap", Rarity.COMMON);
        RARITY_BY_ID.put("card_barbarian_hut", Rarity.COMMON);
        RARITY_BY_ID.put("card_elixir_collector", Rarity.COMMON);

        // Rare (10 total)
        RARITY_BY_ID.put("card_musketeer", Rarity.RARE);
        RARITY_BY_ID.put("card_mini_pekka", Rarity.RARE);
        RARITY_BY_ID.put("card_valkyrie", Rarity.RARE);
        RARITY_BY_ID.put("card_minions", Rarity.RARE);
        RARITY_BY_ID.put("card_barbarians", Rarity.RARE);
        RARITY_BY_ID.put("card_tesla", Rarity.RARE);
        RARITY_BY_ID.put("card_mortar", Rarity.RARE);
        RARITY_BY_ID.put("card_goblin_hut", Rarity.RARE);
        RARITY_BY_ID.put("card_fireball", Rarity.RARE);
        RARITY_BY_ID.put("card_rocket", Rarity.RARE);

        // Epic (4 total)
        RARITY_BY_ID.put("card_giant", Rarity.EPIC);
        RARITY_BY_ID.put("card_wizard", Rarity.EPIC);
        RARITY_BY_ID.put("card_bomb_tower", Rarity.EPIC);
        RARITY_BY_ID.put("card_inferno_tower", Rarity.EPIC);

        // Legendary (2 total)
        RARITY_BY_ID.put("card_hog_rider", Rarity.LEGENDARY);
        RARITY_BY_ID.put("card_minion_horde", Rarity.LEGENDARY);
    }

    private CardRarityCatalog() {
    }

    public static Rarity rarityForCardId(String cardId) {
        if (cardId == null || cardId.isBlank()) {
            return Rarity.COMMON;
        }
        return RARITY_BY_ID.getOrDefault(cardId, Rarity.COMMON);
    }
}
