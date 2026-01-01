package kuroyale.domain;

public class CardProgression {
    private String cardId;
    private int level;
    private Rarity rarity;
    
    public CardProgression() {
        this.level = 1; // Default level
    }
    
    public CardProgression(String cardId, int level, Rarity rarity) {
        this.cardId = cardId;
        this.level = level;
        this.rarity = rarity;
    }
    
    public String getCardId() {
        return cardId;
    }
    
    public void setCardId(String cardId) {
        this.cardId = cardId;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public Rarity getRarity() {
        return rarity;
    }
    
    public void setRarity(Rarity rarity) {
        this.rarity = rarity;
    }
    
    public boolean isMaxLevel() {
        return level >= 3;
    }
    
    public void upgrade() {
        if (!isMaxLevel()) {
            level++;
        }
    }
}

