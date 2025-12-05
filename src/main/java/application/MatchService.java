package application;

import kuroyale.domain.Arena;
import kuroyale.domain.Card;
import kuroyale.domain.Match;
import kuroyale.domain.Player;
import kuroyale.domain.Position;
import kuroyale.domain.Unit;
import kuroyale.infrastructure.MatchRepository;
import kuroyale.support.Result;
import kuroyale.domain.ArenaLayout;

public class MatchService {

    private final MatchRepository matchRepository;

    public MatchService(MatchRepository matchRepository) {
        //initialize the match repository
        this.matchRepository = matchRepository;
    }

    public Match createMatch(Player player, Player opponent) {
        return createMatch(player, opponent, null);
    }

    public Match createMatch(Player player, Player opponent, ArenaLayout layout) {
        Arena arena = new Arena();
        if (layout != null) {
            arena.loadLayout(layout);
        }
        Match match = new Match(player, opponent, arena);
        matchRepository.save(match);
        return match;
    }

    public Result<Unit> deployCard(Match match, Player player, Card card, Position position) {
        if (match == null) {
            return Result.fail("Match is not initialized.");
        }
        return match.deployCard(player, card, position);
    }
}




