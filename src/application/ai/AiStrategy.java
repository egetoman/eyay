package application.ai;

import kuroyale.domain.Match;
import kuroyale.support.Result;

public interface AiStrategy {
    Result<?> playTurn(Match match);
}
