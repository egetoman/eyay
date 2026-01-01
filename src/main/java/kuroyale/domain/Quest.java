package kuroyale.domain;

public class Quest {
    private String id;
    private String description;
    private int targetValue;
    private int rewardGold;
    private QuestType type;
    
    public Quest() {
    }
    
    public Quest(String id, String description, int targetValue, int rewardGold, QuestType type) {
        this.id = id;
        this.description = description;
        this.targetValue = targetValue;
        this.rewardGold = rewardGold;
        this.type = type;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getTargetValue() {
        return targetValue;
    }
    
    public void setTargetValue(int targetValue) {
        this.targetValue = targetValue;
    }
    
    public int getRewardGold() {
        return rewardGold;
    }
    
    public void setRewardGold(int rewardGold) {
        this.rewardGold = rewardGold;
    }
    
    public QuestType getType() {
        return type;
    }
    
    public void setType(QuestType type) {
        this.type = type;
    }
}

