package application;

import kuroyale.domain.CardStats;

public class DefaultCardStatsCalculator implements CardStatsCalculator {
    
    private static final double LEVEL_1_MULTIPLIER = 1.0;
    private static final double LEVEL_2_MULTIPLIER = 1.10;
    private static final double LEVEL_3_MULTIPLIER = 1.20;
    
    @Override
    public CardStats calculateStatsForLevel(CardStats baseStats, int level) {
        if (baseStats == null || level < 1) {
            return baseStats;
        }
        
        double multiplier;
        if (level <= 1) {
            multiplier = LEVEL_1_MULTIPLIER;
        } else if (level == 2) {
            multiplier = LEVEL_2_MULTIPLIER;
        } else {
            multiplier = LEVEL_3_MULTIPLIER;
        }
        
        return new CardStats(
            (int) Math.round(baseStats.getHp() * multiplier),
            (int) Math.round(baseStats.getDamage() * multiplier),
            baseStats.getRange(), // Range doesn't change
            baseStats.getMoveSpeed(), // Move speed doesn't change
            baseStats.getHitSpeedMillis() // Hit speed doesn't change
        );
    }
    
    @Override
    public CardStats previewNextLevel(CardStats currentStats, int currentLevel) {
        if (currentStats == null || currentLevel >= 3) {
            return currentStats;
        }
        return calculateStatsForLevel(currentStats, currentLevel + 1);
    }
}

