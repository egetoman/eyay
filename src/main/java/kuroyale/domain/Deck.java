package kuroyale.domain;

import java.util.ArrayList;
import java.util.List;

public class Deck {

    public static final int MAX_CARDS = 8;

    private final List<Card> cards;

    public Deck() {
        this.cards = new ArrayList<>();
    }

    public Deck(List<Card> cards) {
        if (cards != null) {
            this.cards = new ArrayList<>(cards);
        } else {
            this.cards = new ArrayList<>();
        }
    }

    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    public void setCards(List<Card> newCards) {
        cards.clear();
        if (newCards != null) {
            for (Card card : newCards) {
                if (cards.size() >= MAX_CARDS) {
                    break;
                }
                cards.add(card);
            }
        }
    }

    public boolean addCard(Card card) {
        if (card == null || cards.size() >= MAX_CARDS || containsCard(card)) {
            return false;
        }
        cards.add(card);
        return true;
    }

    public boolean removeCard(Card card) {
        if (card == null) {
            return false;
        }
        String targetId = card.getId();
        for (int i = 0; i < cards.size(); i++) {
            Card existing = cards.get(i);
            if (matches(existing, targetId)) {
                cards.remove(i);
                return true;
            }
        }
        return false;
    }

    public void clear() {
        cards.clear();
    }

    public int size() {
        return cards.size();
    }

    public boolean isFull() {
        return cards.size() >= MAX_CARDS;
    }

    public boolean isValid() {
        return cards.size() == MAX_CARDS;
    }

    public boolean containsCard(Card card) {
        if (card == null) {
            return false;
        }
        return containsCardId(card.getId());
    }

    public boolean containsCardId(String cardId) {
        for (Card existing : cards) {
            if (matches(existing, cardId)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(Card card, String id) {
        if (card == null) {
            return false;
        }
        if (id == null) {
            return false;
        }
        return id.equals(card.getId());
    }
}




