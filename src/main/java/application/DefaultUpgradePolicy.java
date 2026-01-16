package application;

import kuroyale.domain.Rarity;

public class DefaultUpgradePolicy implements UpgradePolicy {
    
    private static final int MAX_LEVEL = 3;
    
    @Override
    public int getUpgradeCost(Rarity rarity, int currentLevel) {
        if (currentLevel >= MAX_LEVEL) {
            return Integer.MAX_VALUE; // Cannot upgrade beyond max
        }
        return resolveUpgradeCost(rarity, currentLevel);
    }
    
    private int resolveUpgradeCost(Rarity rarity, int currentLevel) {
        if (rarity == null) {
            rarity = Rarity.COMMON;
        }
        boolean toLevel2 = currentLevel == 1;
        switch (rarity) {
            case COMMON:
                return toLevel2 ? 200 : 500;
            case RARE:
                return toLevel2 ? 400 : 1000;
            case EPIC:
                return toLevel2 ? 800 : 2000;
            case LEGENDARY:
                return toLevel2 ? 1500 : 4000;
            default:
                return toLevel2 ? 200 : 500;
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

