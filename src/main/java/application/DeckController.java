package application;

import java.util.List;

import kuroyale.domain.Card;
import kuroyale.domain.Deck;

public class DeckController {

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        //initialize the deck service
        this.deckService = deckService;
    }

    public List<Card> getAvailableCards() {
        //get all available cards from the deck service
        return deckService.getAvailableCards();
    }

    public Deck loadDeck() {
        //load the deck from the deck service
        return deckService.loadDeck();
    }

    public Deck buildDefaultDeck() {
        return deckService.buildDefaultDeck();
    }

    public void saveDeck(Deck deck) {
        //save the deck to the deck service
        deckService.saveDeck(deck);
    }
}




