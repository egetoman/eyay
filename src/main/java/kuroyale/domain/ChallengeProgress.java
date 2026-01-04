package kuroyale.domain;

/**
 * Persistent progress for a single Challenge (Phase 2 Feature 4).
 */
public class ChallengeProgress {
    private String challengeId;
    private boolean unlocked;
    private int bestStars;
    private double bestTimeSeconds;
    private int attempts;
    private int completions;

    public ChallengeProgress() {
        this.bestTimeSeconds = 0;
    }

    public ChallengeProgress(String challengeId, boolean unlocked) {
        this.challengeId = challengeId;
        this.unlocked = unlocked;
        this.bestStars = 0;
        this.bestTimeSeconds = 0;
        this.attempts = 0;
        this.completions = 0;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public int getBestStars() {
        return bestStars;
    }

    public void setBestStars(int bestStars) {
        this.bestStars = Math.max(0, Math.min(3, bestStars));
    }

    public double getBestTimeSeconds() {
        return bestTimeSeconds;
    }

    public void setBestTimeSeconds(double bestTimeSeconds) {
        this.bestTimeSeconds = Math.max(0, bestTimeSeconds);
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = Math.max(0, attempts);
    }

    public int getCompletions() {
        return completions;
    }

    public void setCompletions(int completions) {
        this.completions = Math.max(0, completions);
    }

    public void incrementAttempts() {
        attempts++;
    }

    public void incrementCompletions() {
        completions++;
    }
}


