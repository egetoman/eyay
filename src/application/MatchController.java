package application;

import kuroyale.domain.Arena;
import kuroyale.domain.Card;
import kuroyale.domain.Match;
import kuroyale.domain.Player;
import kuroyale.domain.PlayerSide;
import kuroyale.support.Result;

/**
 * Coordinates UI actions with match logic (Controller pattern).
 */
public class MatchController {

    private final MatchService matchService;
    private Match activeMatch;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    public Result<Match> startMatch(Player player, Player opponent, Arena arena) {
        Result<Match> result = matchService.startNewMatch(player, opponent, arena);
        if (result.isSuccess()) {
            activeMatch = result.getData();
        }
        return result;
    }

    public Match getActiveMatch() {
        return activeMatch;
    }

    public Result<Card> playCard(int handIndex, PlayerSide side) {
        if (activeMatch == null) {
            return Result.fail("No active match");
        }
        return matchService.playCard(activeMatch, handIndex, side);
    }

    public void pause() {
        if (activeMatch != null) {
            activeMatch.pause();
        }
    }

    public void resume() {
        if (activeMatch != null) {
            activeMatch.resume();
        }
    }
}
