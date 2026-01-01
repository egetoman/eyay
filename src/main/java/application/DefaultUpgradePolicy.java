package application;

import kuroyale.domain.Rarity;

public class DefaultUpgradePolicy implements UpgradePolicy {
    
    private static final int MAX_LEVEL = 3;
    
    @Override
    public int getUpgradeCost(Rarity rarity, int currentLevel) {
        if (currentLevel >= MAX_LEVEL) {
            return Integer.MAX_VALUE; // Cannot upgrade beyond max
        }
        
        int baseCost = getBaseCost(rarity);
        // Cost increases with level: base * (level + 1)
        return baseCost * (currentLevel + 1);
    }
    
    private int getBaseCost(Rarity rarity) {
        if (rarity == null) {
            return 50; // Default for unknown rarity
        }
        switch (rarity) {
            case COMMON:
                return 50;
            case RARE:
                return 100;
            case EPIC:
                return 200;
            case LEGENDARY:
                return 400;
            default:
                return 50;
        }
    }
    
    @Override
    public boolean canUpgrade(int currentLevel) {
        return currentLevel < MAX_LEVEL;
    }
    
    @Override
    public CardStatsCalculator getStatsCalculator() {
        return new DefaultCardStatsCalculator();
    }
}

