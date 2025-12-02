package kuroyale.infrastructure;

import java.util.Arrays;
import java.util.List;
import kuroyale.domain.Card;
import kuroyale.domain.CardStats;
import kuroyale.domain.CardType;

public class CardCatalogRepository {

    public List<Card> findAll() {
        CardStats knightStats = new CardStats(1000, 150, 1, 2, 1);
        Card knight = new Card("card_knight", "Knight", 3, CardType.TROOP, knightStats);

        CardStats cannonStats = new CardStats(1200, 100, 5, 0, 1);
        Card cannon = new Card("card_cannon", "Cannon", 3, CardType.BUILDING, cannonStats);

        CardStats fireballStats = new CardStats(0, 500, 4, 0, 0);
        Card fireball = new Card("card_fireball", "Fireball", 4, CardType.SPELL, fireballStats);

        return Arrays.asList(knight, cannon, fireball);
    }
}




