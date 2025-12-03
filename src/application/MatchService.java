package application;

import kuroyale.domain.Arena;
import kuroyale.domain.Card;
import kuroyale.domain.Hand;
import kuroyale.domain.Match;
import kuroyale.domain.Player;
import kuroyale.domain.PlayerSide;
import kuroyale.support.Result;
import kuroyale.infrastructure.MatchRepository;

public class MatchService {

    private final MatchRepository matchRepository;

    public MatchService(MatchRepository matchRepository) {
        //initialize the match repository
        this.matchRepository = matchRepository;
    }

    public Result<Match> startNewMatch(Player player, Player opponent, Arena playerArena) {
        if (player == null || opponent == null) {
            return Result.fail("Players must be provided");
        }
        if (playerArena == null) {
            playerArena = new Arena();
        }
        Arena mirrored = playerArena.mirrorVertically();
        // For Phase I we keep a single arena reference for both sides; mirroring keeps symmetry data.
        Match match = new Match(player, opponent, playerArena);
        match.setOpponentHand(new Hand(opponent.getDeck()));
        match.setPlayerHand(new Hand(player.getDeck()));
        match.start();
        matchRepository.save(match);
        return Result.ok(match);
    }

    public Result<Card> playCard(Match match, int handIndex, PlayerSide side) {
        if (match == null) {
            return Result.fail("Match not initialized");
        }
        Hand hand = side == PlayerSide.PLAYER ? match.getPlayerHand() : match.getOpponentHand();
        if (hand == null) {
            return Result.fail("Hand not initialized");
        }
        if (handIndex < 0 || handIndex >= hand.getCards().size()) {
            return Result.fail("Invalid hand index");
        }
        Card card = hand.getCards().get(handIndex);
        if (side == PlayerSide.PLAYER) {
            if (!match.getPlayerElixir().spend(card.getElixirCost())) {
                return Result.fail("Not enough elixir");
            }
        } else {
            if (!match.getOpponentElixir().spend(card.getElixirCost())) {
                return Result.fail("Not enough elixir (opponent)");
            }
        }
        hand.playCard(handIndex);
        return Result.ok(card);
    }
}
