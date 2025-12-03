package application;

import java.util.List;

import main.java.kuroyale.domain.Card;
import main.java.kuroyale.domain.Deck;
import main.java.kuroyale.infrastructure.CardCatalogRepository;
import main.java.kuroyale.infrastructure.DeckRepository;

public class DeckService {

    private final CardCatalogRepository cardCatalogRepository;
    private final DeckRepository deckRepository;

    public DeckService(CardCatalogRepository cardCatalogRepository, DeckRepository deckRepository) {
        //initialize the card catalog repository and the deck repository
        this.cardCatalogRepository = cardCatalogRepository;
        this.deckRepository = deckRepository;
    }

    public List<Card> getAvailableCards() {
        //get all available cards from the card catalog repository
        return cardCatalogRepository.findAll();
    }

    public Deck loadDeck() {
        //load the deck from the deck repository
        return deckRepository.load();
    }

    public void saveDeck(Deck deck) {
        //save the deck to the deck repository
        deckRepository.save(deck);
    }
}




