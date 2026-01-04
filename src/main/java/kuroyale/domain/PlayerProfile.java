package kuroyale.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerProfile {
    private String playerName;
    private int gold;
    private Map<String, CardProgression> cardProgressions;
    private Map<String, ChallengeProgress> challengeProgress;
    
    public PlayerProfile() {
        this.gold = 0;
        this.cardProgressions = new HashMap<>();
        this.challengeProgress = new HashMap<>();
    }
    
    public PlayerProfile(String playerName, int gold) {
        this.playerName = playerName;
        this.gold = gold;
        this.cardProgressions = new HashMap<>();
        this.challengeProgress = new HashMap<>();
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public int getGold() {
        return gold;
    }
    
    public void setGold(int gold) {
        this.gold = Math.max(0, gold);
    }
    
    public void addGold(int amount) {
        this.gold = Math.max(0, this.gold + amount);
    }
    
    public void spendGold(int amount) {
        this.gold = Math.max(0, this.gold - amount);
    }
    
    public boolean hasEnoughGold(int amount) {
        return gold >= amount;
    }
    
    public Map<String, CardProgression> getCardProgressions() {
        return new HashMap<>(cardProgressions);
    }
    
    public void setCardProgressions(Map<String, CardProgression> progressions) {
        this.cardProgressions = progressions != null ? new HashMap<>(progressions) : new HashMap<>();
    }
    
    public CardProgression getCardProgression(String cardId) {
        return cardProgressions.get(cardId);
    }
    
    public void setCardProgression(String cardId, CardProgression progression) {
        if (cardId != null && progression != null) {
            cardProgressions.put(cardId, progression);
        }
    }
    
    public List<CardProgression> getAllProgressions() {
        return new ArrayList<>(cardProgressions.values());
    }

    public Map<String, ChallengeProgress> getChallengeProgress() {
        if (challengeProgress == null) {
            challengeProgress = new HashMap<>();
        }
        return new HashMap<>(challengeProgress);
    }

    public void setChallengeProgress(Map<String, ChallengeProgress> challengeProgress) {
        this.challengeProgress = challengeProgress != null ? new HashMap<>(challengeProgress) : new HashMap<>();
    }

    public ChallengeProgress getChallengeProgress(String challengeId) {
        if (challengeProgress == null) {
            challengeProgress = new HashMap<>();
        }
        return challengeProgress.get(challengeId);
    }

    public void setChallengeProgress(String challengeId, ChallengeProgress progress) {
        if (challengeProgress == null) {
            challengeProgress = new HashMap<>();
        }
        if (challengeId != null && progress != null) {
            challengeProgress.put(challengeId, progress);
        }
    }
}

