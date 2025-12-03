package application.ai;

import java.util.List;
import java.util.Random;
import kuroyale.domain.Card;
import kuroyale.domain.Hand;
import kuroyale.domain.Match;
import kuroyale.domain.PlayerSide;
import kuroyale.support.Result;
import application.MatchService;

/**
 * Simple Strategy implementation that plays a random affordable card.
 */
public class RandomAiStrategy implements AiStrategy {

    private final MatchService matchService;
    private final Random random = new Random();

    public RandomAiStrategy(MatchService matchService) {
        this.matchService = matchService;
    }

    @Override
    public Result<Card> playTurn(Match match) {
        if (match == null || match.getOpponentHand() == null) {
            return Result.fail("Match or AI hand is not ready");
        }
        Hand hand = match.getOpponentHand();
        List<Card> cards = hand.getCards();
        if (cards.isEmpty()) {
            return Result.fail("No cards to play");
        }
        // pick a random playable card by elixir
        int attempts = cards.size();
        while (attempts-- > 0) {
            int idx = random.nextInt(cards.size());
            Card candidate = cards.get(idx);
            if (match.getOpponentElixir().canSpend(candidate.getElixirCost())) {
                return matchService.playCard(match, idx, PlayerSide.OPPONENT);
            }
        }
        return Result.fail("AI has insufficient elixir");
    }
}
