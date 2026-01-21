package application.quest;

import application.QuestGenerator;
import application.QuestResetPolicy;
import application.QuestService;
import kuroyale.domain.*;
import kuroyale.infrastructure.PlayerProfileRepository;
import kuroyale.infrastructure.QuestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QuestUpdateTest {

    private FakeQuestRepository questRepository;
    private FakePlayerProfileRepository profileRepository;
    private FakeQuestGenerator questGenerator;
    private FakeResetPolicy resetPolicy;

    private QuestService questService;
    private DailyQuestSet dailyQuestSet;

    @BeforeEach
    void setUp() {
        questRepository = new FakeQuestRepository();
        profileRepository = new FakePlayerProfileRepository();
        questGenerator = new FakeQuestGenerator();
        resetPolicy = new FakeResetPolicy();

        questService = new QuestService(questRepository, profileRepository, questGenerator, resetPolicy);

        // Setup a dummy quest
        Quest spellQuest = new Quest("quest_xyz", "Deal damage", 100, 50, QuestType.DEAL_SPELL_DAMAGE);
        QuestProgress progress = new QuestProgress("quest_xyz", 0, QuestStatus.IN_PROGRESS);
        dailyQuestSet = new DailyQuestSet(LocalDate.now(), Collections.singletonList(spellQuest),
                Collections.singletonList(progress));
        dailyQuestSet.addQuest(spellQuest); // Ensure map is populated

        questRepository.setQuestSet(dailyQuestSet);
    }

    @Test
    void updateProgress_updatesQuestAndSaves() {
        // Act
        questService.updateProgress(QuestType.DEAL_SPELL_DAMAGE, 50);

        // Assert
        assertTrue(questRepository.isSaveCalled(), "Save should be called");
        assertEquals(50, dailyQuestSet.getProgressForQuest("quest_xyz").getCurrentProgress(),
                "Progress should be updated");
    }

    // Manual Mocks
    static class FakeQuestRepository extends QuestRepository {
        private DailyQuestSet questSet;
        private boolean saveCalled = false;

        public void setQuestSet(DailyQuestSet questSet) {
            this.questSet = questSet;
        }

        public boolean isSaveCalled() {
            return saveCalled;
        }

        @Override
        public DailyQuestSet loadForDate(LocalDate date) {
            return questSet;
        }

        @Override
        public void save(DailyQuestSet questSet) {
            this.saveCalled = true;
            this.questSet = questSet;
        }
    }

    static class FakePlayerProfileRepository extends PlayerProfileRepository {
        @Override
        public PlayerProfile load() {
            return new PlayerProfile("Test User", 0); // Dummy profile
        }
    }

    static class FakeQuestGenerator extends QuestGenerator {
    }

    static class FakeResetPolicy implements QuestResetPolicy {
        @Override
        public boolean shouldReset(java.time.LocalDateTime lastResetAt) {
            return false;
        }

        @Override
        public LocalDate getResetDate() {
            return LocalDate.now();
        }
    }
}
