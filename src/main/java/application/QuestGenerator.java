package application;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import kuroyale.domain.Quest;
import kuroyale.domain.QuestType;

public class QuestGenerator {
    
    private final Random random;
    
    public QuestGenerator() {
        this.random = new Random();
    }
    
    public QuestGenerator(long seed) {
        this.random = new Random(seed);
    }
    
    public List<Quest> generateDailyQuests(long seed) {
        Random rng = new Random(seed);
        List<Quest> quests = new ArrayList<>();
        
        // Generate 3 random quests
        List<QuestType> availableTypes = new ArrayList<>(List.of(
            QuestType.WIN_MATCHES,
            QuestType.PLAY_MATCHES,
            QuestType.DEAL_DAMAGE,
            QuestType.DESTROY_TOWERS,
            QuestType.USE_CARDS
        ));
        
        for (int i = 0; i < 3 && !availableTypes.isEmpty(); i++) {
            int index = rng.nextInt(availableTypes.size());
            QuestType type = availableTypes.remove(index);
            quests.add(createQuest(type, i + 1, rng));
        }
        
        return quests;
    }
    
    private Quest createQuest(QuestType type, int questNumber, Random rng) {
        String id = "quest_" + type.name().toLowerCase() + "_" + questNumber;
        String description;
        int targetValue;
        int rewardGold;
        
        switch (type) {
            case WIN_MATCHES:
                targetValue = 2 + rng.nextInt(3); // 2-4 wins
                description = "Win " + targetValue + " matches";
                rewardGold = 50 + targetValue * 25;
                break;
            case PLAY_MATCHES:
                targetValue = 3 + rng.nextInt(3); // 3-5 matches
                description = "Play " + targetValue + " matches";
                rewardGold = 30 + targetValue * 15;
                break;
            case DEAL_DAMAGE:
                targetValue = 5000 + rng.nextInt(5000); // 5000-10000 damage
                description = "Deal " + targetValue + " damage to towers";
                rewardGold = 40 + targetValue / 200;
                break;
            case DESTROY_TOWERS:
                targetValue = 1 + rng.nextInt(2); // 1-2 towers
                description = "Destroy " + targetValue + " tower" + (targetValue > 1 ? "s" : "");
                rewardGold = 60 + targetValue * 40;
                break;
            case USE_CARDS:
                targetValue = 20 + rng.nextInt(20); // 20-39 cards
                description = "Use " + targetValue + " cards in matches";
                rewardGold = 35 + targetValue * 2;
                break;
            default:
                targetValue = 1;
                description = "Complete quest";
                rewardGold = 50;
        }
        
        return new Quest(id, description, targetValue, rewardGold, type);
    }
}

