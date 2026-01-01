package kuroyale.domain;

public class QuestProgress {
    private String questId;
    private int currentProgress;
    private QuestStatus status;
    
    public QuestProgress() {
        this.status = QuestStatus.IN_PROGRESS;
    }
    
    public QuestProgress(String questId, int currentProgress, QuestStatus status) {
        this.questId = questId;
        this.currentProgress = currentProgress;
        this.status = status;
    }
    
    public String getQuestId() {
        return questId;
    }
    
    public void setQuestId(String questId) {
        this.questId = questId;
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
    
    public QuestStatus getStatus() {
        return status;
    }
    
    public void setStatus(QuestStatus status) {
        this.status = status;
    }
    
    public boolean isCompleted() {
        return status == QuestStatus.COMPLETED || status == QuestStatus.CLAIMED;
    }
    
    public boolean isClaimed() {
        return status == QuestStatus.CLAIMED;
    }
    
    public void markCompleted() {
        if (status == QuestStatus.IN_PROGRESS) {
            status = QuestStatus.COMPLETED;
        }
    }
    
    public void markClaimed() {
        if (status == QuestStatus.COMPLETED) {
            status = QuestStatus.CLAIMED;
        }
    }
}

