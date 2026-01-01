package application;

import kuroyale.domain.CardStats;

public interface CardStatsCalculator {
    CardStats calculateStatsForLevel(CardStats baseStats, int level);
    CardStats previewNextLevel(CardStats currentStats, int currentLevel);
}

