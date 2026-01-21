package application;

import kuroyale.domain.AchievementType;
import kuroyale.domain.Match;
import kuroyale.domain.MatchOutcome;
import kuroyale.domain.QuestType;
import kuroyale.domain.TowerOwner;

/**
 * Centralized helper for updating quests and achievements after a match.
 * Ensures consistent tracking across different game modes (local, AI, network).
 */
public class QuestUpdateHelper {

    public static void updateAfterMatch(QuestService questService, AchievementService achievementService, Match match,
            boolean isWin, int playerCrowns) {
        if (questService != null) {
            updateQuests(questService, match, isWin, playerCrowns);
        }
        if (achievementService != null) {
            updateAchievements(achievementService, isWin, playerCrowns);
        }
    }

    private static void updateQuests(QuestService questService, Match match, boolean isWin, int playerCrowns) {
        // Track win/loss for streak (Logic depends on implementation in QuestService,
        // assuming it handles boolean correctly)
        questService.recordMatchResult(isWin);

        if (isWin) {
            questService.updateProgress(QuestType.WIN_MATCHES, 1);
            // Note: AI matches might need differentiation if strictly PvP, but for now
            // assuming generic win

            // For QuestType.WIN_PVP_MATCH, the caller should determine if it's a PvP match
            // and call updateProgress manually if needed,
            // or we add a parameter. For safety, let's keep generic wins here.
            // If match.isBotEnabled() is false, it's likely PvP.
            if (match != null && !match.isBotEnabled()) {
                questService.updateProgress(QuestType.WIN_PVP_MATCH, 1);
            }

            MatchOutcome outcome = match.getOutcome();
            // Win without losing crown tower
            if (outcome != null && outcome.getOpponentCrowns() == 0) {
                questService.updateProgress(QuestType.WIN_WITHOUT_LOSING_CROWN, 1);
            }

            // Check for win using only common rarity cards
            if (match != null && match.isPlayerDeckAllCommon()) {
                questService.updateProgress(QuestType.WIN_ONLY_COMMON, 1);
            }
        }

        // Update crown tower destruction progress
        if (playerCrowns > 0) {
            questService.updateProgress(QuestType.DESTROY_CROWN_TOWERS, playerCrowns);
        }

        // Check for king tower destruction (3 crowns)
        if (playerCrowns >= 3) {
            questService.updateProgress(QuestType.DESTROY_ENEMY_KING, 1);
        }

        // Track card plays from match statistics
        if (match != null) {
            int spellsPlayed = match.getPlayerSpellsPlayed();
            int troopsDeployed = match.getPlayerTroopsDeployed();
            int buildingsPlayed = match.getPlayerBuildingsPlayed();
            int elixirSpent = match.getPlayerElixirSpent();
            int totalCardsPlayed = match.getPlayerCardsPlayed();
            int spellDamage = match.getPlayerSpellDamageDealt();

            if (spellsPlayed > 0) {
                questService.updateProgress(QuestType.PLAY_SPELL_CARDS, spellsPlayed);
            }
            if (troopsDeployed > 0) {
                questService.updateProgress(QuestType.DEPLOY_TROOP_CARDS, troopsDeployed);
            }
            if (buildingsPlayed > 0) {
                questService.updateProgress(QuestType.PLAY_BUILDING_CARDS, buildingsPlayed);
            }
            if (elixirSpent > 0) {
                questService.updateProgress(QuestType.SPEND_ELIXIR, elixirSpent);
            }
            if (totalCardsPlayed >= 20) {
                questService.updateProgress(QuestType.PLAY_20_CARDS_SINGLE_MATCH, 1);
            }
            if (spellDamage > 0) {
                questService.updateProgress(QuestType.DEAL_SPELL_DAMAGE, spellDamage);
            }
        }
    }

    private static void updateAchievements(AchievementService achievementService, boolean isWin, int playerCrowns) {
        if (isWin) {
            achievementService.updateProgress(AchievementType.WIN_TOTAL_MATCHES, 1);
        }

        if (playerCrowns > 0) {
            achievementService.updateProgress(AchievementType.TOTAL_CROWNS, playerCrowns);
        }
    }
}
