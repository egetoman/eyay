package application;

import application.QuestService;
import application.AchievementService;
import application.QuestUpdateHelper;
import kuroyale.domain.Match;
import kuroyale.domain.MatchOutcome;
import kuroyale.domain.QuestType;
import kuroyale.domain.TowerOwner;
import kuroyale.domain.Unit;
import kuroyale.domain.Position;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class QuestUpdateHelperTest {

    @Test
    void updateAfterMatch_updatesWinQuests() {
        FakeQuestService questService = new FakeQuestService();
        Match match = new FakeMatch(true); // Winner

        QuestUpdateHelper.updateAfterMatch(questService, null, match, true, 3);

        assertTrue(questService.updatedTypes.contains(QuestType.WIN_MATCHES), "Should update WIN_MATCHES");
        assertTrue(questService.updatedTypes.contains(QuestType.WIN_PVP_MATCH), "Should update WIN_PVP_MATCH");
        // Our FakeMatch returns 0 opponent crowns, so this should trigger
        assertTrue(questService.updatedTypes.contains(QuestType.WIN_WITHOUT_LOSING_CROWN),
                "Should update WIN_WITHOUT_LOSING_CROWN");
    }

    @Test
    void updateAfterMatch_updatesStatQuests() {
        FakeQuestService questService = new FakeQuestService();
        FakeMatch match = new FakeMatch(false);
        match.spellsPlayed = 5;
        match.spellDamage = 1000;

        QuestUpdateHelper.updateAfterMatch(questService, null, match, false, 0);

        assertTrue(questService.updatedTypes.contains(QuestType.PLAY_SPELL_CARDS), "Should update PLAY_SPELL_CARDS");
        assertEquals(5, questService.progressAmounts.getOrDefault(QuestType.PLAY_SPELL_CARDS, 0));

        assertTrue(questService.updatedTypes.contains(QuestType.DEAL_SPELL_DAMAGE), "Should update DEAL_SPELL_DAMAGE");
        assertEquals(1000, questService.progressAmounts.getOrDefault(QuestType.DEAL_SPELL_DAMAGE, 0));
    }

    // Manual Stubs
    static class FakeQuestService extends QuestService {
        Set<QuestType> updatedTypes = new HashSet<>();
        java.util.Map<QuestType, Integer> progressAmounts = new java.util.HashMap<>();

        public FakeQuestService() {
            super(null, null, null, null);
        }

        @Override
        public void updateProgress(QuestType type, int amount) {
            updatedTypes.add(type);
            progressAmounts.put(type, amount);
        }

        @Override
        public void recordMatchResult(boolean win) {
        }
    }

    static class FakeMatch extends Match {
        boolean isWinner;
        int spellsPlayed = 0;
        int spellDamage = 0;

        public FakeMatch(boolean isWinner) {
            super(null, null, null);
            this.isWinner = isWinner;
        }

        @Override
        public MatchOutcome getOutcome() {
            return new MatchOutcome(
                    isWinner ? TowerOwner.PLAYER : TowerOwner.OPPONENT,
                    3, 0, "TEST" // Player crowns, Opponent crowns, Reason
            );
        }

        @Override
        public boolean isBotEnabled() {
            return false;
        }

        @Override
        public boolean isPlayerDeckAllCommon() {
            return false;
        }

        @Override
        public int getPlayerSpellsPlayed() {
            return spellsPlayed;
        }

        @Override
        public int getPlayerSpellDamageDealt() {
            return spellDamage;
        }

        @Override
        public int getPlayerTroopsDeployed() {
            return 0;
        }

        @Override
        public int getPlayerBuildingsPlayed() {
            return 0;
        }

        @Override
        public int getPlayerElixirSpent() {
            return 0;
        }

        @Override
        public int getPlayerCardsPlayed() {
            return 0;
        }
    }
}
