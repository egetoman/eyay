package application;

import main.java.kuroyale.domain.Arena;
import main.java.kuroyale.domain.Match;
import main.java.kuroyale.domain.Player;
import main.java.kuroyale.infrastructure.MatchRepository;

public class MatchService {

    private final MatchRepository matchRepository;

    public MatchService(MatchRepository matchRepository) {
        //initialize the match repository
        this.matchRepository = matchRepository;
    }

    public Match createMatch(Player player, Player opponent) {
        //create a new match with the given player and opponent
        Match match = new Match(player, opponent, new Arena());
        matchRepository.save(match);
        return match;
    }
}




