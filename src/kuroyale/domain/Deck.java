package kuroyale.domain;

import java.util.ArrayList;
import java.util.List;

public class Deck {

    private List<Card> cards;

    public Deck() {
        this.cards = new ArrayList<>();
    }

    public Deck(List<Card> cards) {
        this.cards = cards != null ? cards : new ArrayList<>();
    }

    public List<Card> getCards() {
        return cards;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
    }

    public void addCard(Card card) {
        // TODO: implement card addition rules
    }

    public void removeCard(Card card) {
        // TODO: implement card removal rules
    }

    public boolean isValid() {
        // TODO: implement deck validation rules
        return false;
    }
}




