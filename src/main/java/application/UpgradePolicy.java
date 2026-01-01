package application;

import kuroyale.domain.Rarity;

public interface UpgradePolicy {
    int getUpgradeCost(Rarity rarity, int currentLevel);
    boolean canUpgrade(int currentLevel);
    CardStatsCalculator getStatsCalculator();
}

