package kuroyale.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * OVERVIEW:
 *   A Deck represents a collection of cards that a player uses in battle.
 *   A deck can contain at most MAX_CARDS (8) cards, and each card can appear
 *   at most once in the deck (uniqueness by card ID).
 * 
 * ABSTRACT FUNCTION:
 *   AF(this) = {card1, card2, ..., cardN} where:
 *     - N = cards.size() and 0 <= N <= MAX_CARDS
 *     - Each card in the set is unique (by card ID)
 *     - The set represents the player's deck configuration
 * 
 * REPRESENTATION INVARIANT:
 *   - cards != null
 *   - cards.size() <= MAX_CARDS
 *   - For all i, j in [0, cards.size()): if i != j, then cards.get(i).getId() != cards.get(j).getId()
 *   - For all c in cards: c != null
 */
public class Deck {

    public static final int MAX_CARDS = 8;

    private final List<Card> cards;

    public Deck() {
        this.cards = new ArrayList<>();
        assert repOk();
    }

    public Deck(List<Card> cards) {
        if (cards != null) {
            this.cards = new ArrayList<>(cards);
            // Remove duplicates and nulls to maintain RI
            List<Card> cleaned = new ArrayList<>();
            for (Card card : this.cards) {
                if (card != null && cleaned.size() < MAX_CARDS) {
                    boolean isDuplicate = false;
                    for (Card existing : cleaned) {
                        if (existing != null && card.getId() != null && 
                            card.getId().equals(existing.getId())) {
                            isDuplicate = true;
                            break;
                        }
                    }
                    if (!isDuplicate) {
                        cleaned.add(card);
                    }
                }
            }
            this.cards.clear();
            this.cards.addAll(cleaned);
        } else {
            this.cards = new ArrayList<>();
        }
        assert repOk();
    }

    /**
     * Checks the representation invariant.
     * 
     * @return true if the representation invariant holds, false otherwise
     */
    public boolean repOk() {
        if (cards == null) {
            return false;
        }
        if (cards.size() > MAX_CARDS) {
            return false;
        }
        // Check for null cards
        for (Card card : cards) {
            if (card == null) {
                return false;
            }
        }
        // Check for duplicate card IDs
        for (int i = 0; i < cards.size(); i++) {
            Card card1 = cards.get(i);
            if (card1 == null || card1.getId() == null) {
                return false;
            }
            for (int j = i + 1; j < cards.size(); j++) {
                Card card2 = cards.get(j);
                if (card2 != null && card2.getId() != null && 
                    card1.getId().equals(card2.getId())) {
                    return false;
                }
            }
        }
        return true;
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
                if (card != null && !containsCard(card)) {
                    cards.add(card);
                }
            }
        }
        assert repOk();
    }

    public boolean addCard(Card card) {
        if (card == null || cards.size() >= MAX_CARDS || containsCard(card)) {
            return false;
        }
        cards.add(card);
        assert repOk();
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
                assert repOk();
                return true;
            }
        }
        return false;
    }

    public void clear() {
        cards.clear();
        assert repOk();
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




