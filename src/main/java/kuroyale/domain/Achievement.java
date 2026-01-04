package kuroyale.domain;

public class Achievement {
    private String id;
    private String name;
    private String description;
    private int targetValue;
    private int rewardGold;
    private AchievementType type;
    
    public Achievement() {
    }
    
    public Achievement(String id, String name, String description, int targetValue, int rewardGold, AchievementType type) {
        this.id = id;
        this.name = name;
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
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
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
    
    public AchievementType getType() {
        return type;
    }
    
    public void setType(AchievementType type) {
        this.type = type;
    }
}

