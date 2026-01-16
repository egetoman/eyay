package kuroyale.domain;

public class AchievementProgress {
    private String achievementId;
    private int currentProgress;
    private boolean unlocked;
    private boolean claimed;
    
    public AchievementProgress() {
        this.unlocked = false;
        this.claimed = false;
    }
    
    public AchievementProgress(String achievementId, int currentProgress, boolean unlocked) {
        this.achievementId = achievementId;
        this.currentProgress = currentProgress;
        this.unlocked = unlocked;
        this.claimed = unlocked;
    }
    
    public String getAchievementId() {
        return achievementId;
    }
    
    public void setAchievementId(String achievementId) {
        this.achievementId = achievementId;
    }
    
    public int getCurrentProgress() {
        return currentProgress;
    }
    
    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = Math.max(0, currentProgress);
    }
    
    public void addProgress(int amount) {
        this.currentProgress = Math.max(0, this.currentProgress + amount);
    }
    
    public boolean isUnlocked() {
        return unlocked;
    }
    
    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }
    
    public boolean isCompleted(Achievement achievement) {
        return achievement != null && currentProgress >= achievement.getTargetValue();
    }
}

