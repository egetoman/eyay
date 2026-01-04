package application;

import kuroyale.domain.CardStats;

public class DefaultCardStatsCalculator implements CardStatsCalculator {
    
    // Stats increase by 10% per level
    private static final double STAT_MULTIPLIER_PER_LEVEL = 1.10;
    
    @Override
    public CardStats calculateStatsForLevel(CardStats baseStats, int level) {
        if (baseStats == null || level < 1) {
            return baseStats;
        }
        
        // Level 1 is base stats, so we multiply by (1.10 ^ (level - 1))
        double multiplier = Math.pow(STAT_MULTIPLIER_PER_LEVEL, level - 1);
        
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

