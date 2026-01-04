package application.challenge;

import java.util.HashMap;
import java.util.Map;
import kuroyale.domain.Match;

/**
 * Runtime data for a started challenge match.
 * Stored in-memory and used to compute star rating after match completion.
 */
public class ChallengeSession {
    private final String challengeId;
    private final Match match;
    private final Map<String, Integer> initialPlayerTowerHp = new HashMap<>();

    public ChallengeSession(String challengeId, Match match) {
        this.challengeId = challengeId;
        this.match = match;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public Match getMatch() {
        return match;
    }

    public Map<String, Integer> getInitialPlayerTowerHp() {
        return new HashMap<>(initialPlayerTowerHp);
    }

    public void recordInitialTowerHp(String key, int hp) {
        if (key != null) {
            initialPlayerTowerHp.put(key, hp);
        }
    }
}


