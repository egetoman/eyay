package application;

import kuroyale.domain.Card;
import kuroyale.domain.Match;
import kuroyale.domain.Player;
import kuroyale.domain.Position;
import kuroyale.domain.Unit;
import kuroyale.support.Result;

/**
 * Application-layer controller for match interactions.
 * <p>
 * Model–View separation: UI should not call domain mutation methods directly. UI calls this controller,
 * which delegates to {@link MatchService} and exposes read-only access to the underlying {@link Match}.
 */
public class MatchController {
    private final MatchService matchService;
    private final Match match;

    public MatchController(MatchService matchService, Match match) {
        this.matchService = matchService;
        this.match = match;
    }

    public Match getMatch() {
        return match;
    }

    public Player getPlayer() {
        return match != null ? match.getPlayer() : null;
    }

    public Player getOpponent() {
        return match != null ? match.getOpponent() : null;
    }

    public Result<Unit> deployCard(Player actingPlayer, Card card, Position position) {
        if (matchService == null) {
            return Result.fail("Match service is not available.");
        }
        return matchService.deployCard(match, actingPlayer, card, position);
    }

    public void advanceTime(double deltaSeconds) {
        if (match != null) {
            match.advanceTime(deltaSeconds);
        }
    }
}


