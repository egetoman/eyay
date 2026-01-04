package application.quest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import application.QuestGenerator;
import kuroyale.domain.Quest;
import kuroyale.domain.QuestType;

public class QuestGeneratorTest {

    private QuestGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new QuestGenerator();
    }

    /**
     * Test 1:
     * Verifies that exactly three quests are generated.
     */
    @Test
    void generateDailyQuests_returnsExactlyThreeQuests() {
        List<Quest> quests = generator.generateDailyQuests(1L);

        assertEquals(3, quests.size(), "Exactly three quests should be generated");
    }

    /**
     * Test 2:
     * Verifies that generated quest types are unique.
     */
    @Test
    void generateDailyQuests_generatesUniqueQuestTypes() {
        List<Quest> quests = generator.generateDailyQuests(1L);

        Set<QuestType> types =
                quests.stream().map(Quest::getType).collect(Collectors.toSet());

        assertEquals(3, types.size(), "All quest types should be unique");
    }

    /**
     * Test 3:
     * Verifies that the same seed produces the same quests (deterministic behavior).
     */
    @Test
    void generateDailyQuests_sameSeedProducesSameQuests() {
        List<Quest> first = generator.generateDailyQuests(42L);
        List<Quest> second = generator.generateDailyQuests(42L);

        for (int i = 0; i < 3; i++) {
            assertEquals(first.get(i).getId(), second.get(i).getId());
            assertEquals(first.get(i).getDescription(), second.get(i).getDescription());
            assertEquals(first.get(i).getTargetValue(), second.get(i).getTargetValue());
            assertEquals(first.get(i).getRewardGold(), second.get(i).getRewardGold());
            assertEquals(first.get(i).getType(), second.get(i).getType());
        }
    }

    /**
     * Test 4 (extra):
     * Verifies that all generated quests have non-null fields.
     */
    @Test
    void generateDailyQuests_questsHaveValidFields() {
        List<Quest> quests = generator.generateDailyQuests(5L);

        for (Quest quest : quests) {
            assertNotNull(quest.getId(), "Quest id should not be null");
            assertNotNull(quest.getDescription(), "Quest description should not be null");
            assertNotNull(quest.getType(), "Quest type should not be null");
            assertTrue(quest.getTargetValue() > 0, "Target value should be positive");
            assertTrue(quest.getRewardGold() > 0, "Reward gold should be positive");
        }
    }

    /**
     * Test 5 (extra):
     * Verifies that different seeds can produce different quest sets.
     */
    @Test
    void generateDailyQuests_differentSeedsProduceDifferentResults() {
        List<Quest> quests1 = generator.generateDailyQuests(1L);
        List<Quest> quests2 = generator.generateDailyQuests(2L);

        boolean anyDifference = false;
        for (int i = 0; i < 3; i++) {
            if (!quests1.get(i).getId().equals(quests2.get(i).getId())) {
                anyDifference = true;
                break;
            }
        }

        assertTrue(anyDifference,
                "Different seeds should be able to produce different quests");
    }
}
