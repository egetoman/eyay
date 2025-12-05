package application;

import java.util.ArrayList;
import java.util.List;
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
            return stored;
        }
        return buildDefaultDeck();
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




