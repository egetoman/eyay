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
        this.cards = cards != null ? cards : new ArrayList<>();
    }

    public boolean addCard(Card card) {
        if (card == null || cards == null) {
            return false;
        }
        if (cards.size() >= 8) {
            return false; // deck is full
        }
        // enforce uniqueness by card id
        boolean alreadyPresent = cards.stream()
                .anyMatch(existing -> existing.getId() != null && existing.getId().equals(card.getId()));
        if (alreadyPresent) {
            return false;
        }
        return cards.add(card);
    }

    public boolean removeCard(Card card) {
        if (card == null || cards == null) {
            return false;
        }
        return cards.removeIf(existing -> existing.getId() != null && existing.getId().equals(card.getId()));
    }

    public boolean isValid() {
        if (cards == null || cards.size() != 8) {
            return false;
        }
        long uniqueCount = cards.stream()
                .filter(c -> c != null && c.getId() != null)
                .map(Card::getId)
                .distinct()
                .count();
        return uniqueCount == 8;
    }
}

