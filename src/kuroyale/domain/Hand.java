package kuroyale.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Represents the 4-card hand that cycles through the deck.
 */
public class Hand {

    private final List<Card> cards;
    private final Deque<Card> drawPile;

    public Hand(Deck deck) {
        this.cards = new ArrayList<>();
        this.drawPile = new ArrayDeque<>();
        reset(deck);
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    public void reset(Deck deck) {
        cards.clear();
        drawPile.clear();
        if (deck == null || deck.getCards() == null) {
            return;
        }
        drawPile.addAll(deck.getCards());
        dealInitialHand();
    }

    private void dealInitialHand() {
        while (cards.size() < 4 && !drawPile.isEmpty()) {
            cards.add(drawPile.poll());
        }
    }

    public boolean contains(Card card) {
        return cards.contains(card);
    }

    public boolean canPlay(Card card, int currentElixir) {
        return card != null && cards.contains(card) && currentElixir >= card.getElixirCost();
    }

    /**
     * Plays the card at the given index, cycles it to the back of the draw pile,
     * and draws the next card to keep 4 cards in hand when possible.
     */
    public Card playCard(int index) {
        if (index < 0 || index >= cards.size()) {
            return null;
        }
        Card played = cards.remove(index);
        if (played != null) {
            drawPile.offer(played);
        }
        if (!drawPile.isEmpty()) {
            cards.add(drawPile.poll());
        }
        return played;
    }
}
