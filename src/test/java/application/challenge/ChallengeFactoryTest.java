package application.challenge;

import kuroyale.domain.Card;
import kuroyale.domain.CardCostPolicy;
import kuroyale.domain.CardType;
import kuroyale.domain.Deck;
import kuroyale.infrastructure.CardCatalogRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ChallengeFactoryTest {

    @Test
    void budgetBattle_rejectsCardCostAbove3() {
        ChallengeFactory factory = new ChallengeFactory();
        ChallengeDefinition budget = factory.byId("4");
        assertNotNull(budget);

        Deck deck = new Deck(pickFirst8());
        // Ensure at least one expensive card in the deck (Rocket costs 6 in catalog)
        assertTrue(deck.getCards().stream().anyMatch(c -> c != null && c.getElixirCost() > 3));

        var result = budget.validateDeck(deck);
        assertFalse(result.isSuccess());
    }

    @Test
    void spellBarrage_costPolicy_reducesSpellCostMin1() {
        ChallengeFactory factory = new ChallengeFactory();
        ChallengeDefinition spell = factory.byId("2");
        assertNotNull(spell);

        CardCostPolicy policy = spell.buildCostPolicy();
        assertNotNull(policy);

        Card zap = find("card_zap");
        assertNotNull(zap);
        assertEquals(CardType.SPELL, zap.getType());

        int base = zap.getElixirCost(); // 2
        int modified = policy.resolveCost(null, zap, base);
        // when actingPlayer is null policy returns base, but our Match wrapper applies only for player.
        assertEquals(base, modified);

        // simulate non-null acting player
        modified = policy.resolveCost(new kuroyale.domain.Player(), zap, base);
        assertEquals(1, modified);
    }

    private List<Card> pickFirst8() {
        List<Card> all = new CardCatalogRepository().findAll();
        List<Card> picked = new ArrayList<>();
        for (Card c : all) {
            if (picked.size() >= Deck.MAX_CARDS) break;
            picked.add(c);
        }
        return picked;
    }

    private Card find(String id) {
        for (Card c : new CardCatalogRepository().findAll()) {
            if (c != null && id.equals(c.getId())) {
                return c;
            }
        }
        return null;
    }
}


