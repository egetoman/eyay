package application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kuroyale.domain.Card;
import kuroyale.domain.Deck;
import kuroyale.infrastructure.CardCatalogRepository;
import kuroyale.infrastructure.DeckRepository;

public class DeckService {

    private final CardCatalogRepository cardCatalogRepository;
    private final DeckRepository deckRepository;

    public DeckService(CardCatalogRepository cardCatalogRepository, DeckRepository deckRepository) {
        this.cardCatalogRepository = cardCatalogRepository;
        this.deckRepository = deckRepository;
    }

    public List<Card> getAvailableCards() {
        return cardCatalogRepository.findAll();
    }

    public Deck loadDeck() {
        Deck stored = deckRepository.load();
        if (stored != null && stored.isValid()) {
            // Rehydrate cards from catalog to ensure all fields (like movementType) are up-to-date
            return rehydrateDeck(stored);
        }
        return buildDefaultDeck();
    }

    /**
     * Rehydrates a deck by replacing stored cards with canonical versions from the catalog.
     * This ensures all card fields (including movementType for air/ground targeting) are correct,
     * even if the deck was saved before certain fields were added.
     */
    private Deck rehydrateDeck(Deck stored) {
        Map<String, Card> catalogById = new HashMap<>();
        for (Card c : cardCatalogRepository.findAll()) {
            if (c != null && c.getId() != null) {
                catalogById.put(c.getId(), c);
            }
        }

        List<Card> rehydrated = new ArrayList<>();
        for (Card storedCard : stored.getCards()) {
            if (storedCard == null || storedCard.getId() == null) {
                continue;
            }
            Card canonical = catalogById.get(storedCard.getId());
            if (canonical != null) {
                rehydrated.add(canonical);
            } else {
                // Fallback to stored card if not found in catalog
                rehydrated.add(storedCard);
            }
        }
        return new Deck(rehydrated);
    }

    public Deck buildDefaultDeck() {
        List<Card> available = getAvailableCards();
        List<Card> selection = new ArrayList<>();
        for (Card card : available) {
            if (selection.size() >= Deck.MAX_CARDS) {
                break;
            }
            selection.add(card);
        }
        return new Deck(selection);
    }

    public void saveDeck(Deck deck) {
        if (deck == null || !deck.isValid()) {
            throw new IllegalArgumentException("Deck must contain exactly " + Deck.MAX_CARDS + " cards.");
        }
        deckRepository.save(deck);
    }
}




