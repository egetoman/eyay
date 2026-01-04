package application.challenge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kuroyale.domain.Card;
import kuroyale.domain.CardCostPolicy;
import kuroyale.domain.CardType;
import kuroyale.domain.Deck;
import kuroyale.support.Result;

/**
 * Factory for the 5 required challenges (Phase 2 Feature 4).
 * <p>
 * Pattern: Factory + Strategy. Each ChallengeDefinition is a Strategy object created by this Factory.
 */
public class ChallengeFactory {

    public List<ChallengeDefinition> allChallenges() {
        List<ChallengeDefinition> list = new ArrayList<>();
        list.add(swarmMaster());
        list.add(spellBarrage());
        list.add(noBuildings());
        list.add(budgetBattle());
        list.add(tankRush());
        return list;
    }

    public ChallengeDefinition byId(String id) {
        for (ChallengeDefinition def : allChallenges()) {
            if (def.getId().equals(id)) {
                return def;
            }
        }
        return null;
    }

    private ChallengeDefinition swarmMaster() {
        Set<String> swarmIds = Set.of(
            "card_skeletons",
            "card_goblins",
            "card_spear_goblins",
            "card_archers",
            "card_minions",
            "card_minion_horde",
            "card_barbarians"
        );
        return new BaseChallenge("1", "Swarm Master",
            "Deck must include at least 5 swarm troops. Remaining cards must be TROOPs (no spells/buildings).",
            250, 180) {
            @Override
            public Result<?> validateDeck(Deck deck) {
                if (deck == null || !deck.isValid()) {
                    return Result.fail("Deck must have exactly 8 cards.");
                }
                int swarmCount = 0;
                for (Card c : deck.getCards()) {
                    if (c == null) continue;
                    if (c.getType() != CardType.TROOP) {
                        return Result.fail("Only troop cards are allowed.");
                    }
                    if (swarmIds.contains(c.getId())) {
                        swarmCount++;
                    }
                }
                if (swarmCount < 5) {
                    return Result.fail("Deck must contain at least 5 swarm cards.");
                }
                return Result.ok(null);
            }
        };
    }

    private ChallengeDefinition spellBarrage() {
        Set<String> required = Set.of("card_zap", "card_arrows", "card_fireball", "card_rocket");
        return new BaseChallenge("2", "Spell Barrage",
            "Deck must contain all 4 spells (Zap, Arrows, Fireball, Rocket). Spells cost 1 less elixir (min 1).",
            300, 200) {
            @Override
            public Result<?> validateDeck(Deck deck) {
                if (deck == null || !deck.isValid()) {
                    return Result.fail("Deck must have exactly 8 cards.");
                }
                Set<String> present = new HashSet<>();
                for (Card c : deck.getCards()) {
                    if (c == null) continue;
                    if (c.getType() == CardType.SPELL) {
                        present.add(c.getId());
                    }
                }
                for (String id : required) {
                    if (!present.contains(id)) {
                        return Result.fail("Deck must include all spells: Zap, Arrows, Fireball, Rocket.");
                    }
                }
                return Result.ok(null);
            }

            @Override
            public CardCostPolicy buildCostPolicy() {
                // Spell cost -1, min 1, applied to the player only (actingPlayer is match.getPlayer()).
                return (actingPlayer, card, baseCost) -> {
                    if (actingPlayer == null || card == null) {
                        return baseCost;
                    }
                    if (card.getType() == CardType.SPELL) {
                        return Math.max(1, baseCost - 1);
                    }
                    return baseCost;
                };
            }
        };
    }

    private ChallengeDefinition noBuildings() {
        return new BaseChallenge("3", "No Buildings Allowed",
            "No building cards are allowed in the deck.",
            200, 180) {
            @Override
            public Result<?> validateDeck(Deck deck) {
                if (deck == null || !deck.isValid()) {
                    return Result.fail("Deck must have exactly 8 cards.");
                }
                for (Card c : deck.getCards()) {
                    if (c == null) continue;
                    if (c.getType() == CardType.BUILDING) {
                        return Result.fail("Buildings are not allowed in this challenge.");
                    }
                }
                return Result.ok(null);
            }
        };
    }

    private ChallengeDefinition budgetBattle() {
        return new BaseChallenge("4", "Budget Battle",
            "Only cards with elixir cost 3 or less are allowed.",
            250, 160) {
            @Override
            public Result<?> validateDeck(Deck deck) {
                if (deck == null || !deck.isValid()) {
                    return Result.fail("Deck must have exactly 8 cards.");
                }
                for (Card c : deck.getCards()) {
                    if (c == null) continue;
                    if (c.getElixirCost() > 3) {
                        return Result.fail("All cards must cost 3 elixir or less.");
                    }
                }
                return Result.ok(null);
            }
        };
    }

    private ChallengeDefinition tankRush() {
        Set<String> allowed = Set.of(
            "card_giant",
            "card_knight",
            "card_valkyrie",
            "card_mini_pekka",
            "card_barbarians"
        );
        return new BaseChallenge("5", "Tank Rush",
            "Deck must include at least 5 tank troops (Giant, Knight, Valkyrie, Mini P.E.K.K.A, Barbarians). No spells/buildings.",
            300, 200) {
            @Override
            public Result<?> validateDeck(Deck deck) {
                if (deck == null || !deck.isValid()) {
                    return Result.fail("Deck must have exactly 8 cards.");
                }
                int tankCount = 0;
                for (Card c : deck.getCards()) {
                    if (c == null) continue;
                    if (c.getType() != CardType.TROOP) {
                        return Result.fail("No spells/buildings allowed in this challenge.");
                    }
                    if (allowed.contains(c.getId())) {
                        tankCount++;
                    }
                }
                if (tankCount < 5) {
                    return Result.fail("Deck must include at least 5 of: Giant, Knight, Valkyrie, Mini P.E.K.K.A, Barbarians.");
                }
                return Result.ok(null);
            }
        };
    }

    private abstract static class BaseChallenge implements ChallengeDefinition {
        private final String id;
        private final String name;
        private final String rules;
        private final int reward;
        private final int timeLimitSeconds;

        private BaseChallenge(String id, String name, String rules, int reward, int timeLimitSeconds) {
            this.id = id;
            this.name = name;
            this.rules = rules;
            this.reward = reward;
            this.timeLimitSeconds = timeLimitSeconds;
        }

        @Override public String getId() { return id; }
        @Override public String getName() { return name; }
        @Override public String getRulesSummary() { return rules; }
        @Override public int getRewardGold() { return reward; }
        @Override public int getTimeLimitSeconds() { return timeLimitSeconds; }
        @Override public CardCostPolicy buildCostPolicy() { return null; }
    }
}


