package application;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import kuroyale.domain.Quest;
import kuroyale.domain.QuestType;

public class QuestGenerator {
    
    public QuestGenerator() {
    }
    
    public QuestGenerator(long seed) {
    }
    /**
     * REQUIRES:
     *  - seed can be any long value.
     *
     * MODIFIES:
     *  - nothing.
     *
     * EFFECTS:
     *  - Generates and returns exactly 3 Quest objects.
     *  - Quest generation is deterministic with respect to the given seed.
     *  - All generated quests have distinct QuestType values.
     *  - Each generated Quest has:
     *      - a non-null id, description, and type,
     *      - a positive target value,
     *      - a positive reward gold value.
     *  - Different seed values may result in different quest sets.
     */

    public List<Quest> generateDailyQuests(long seed) {
        Random rng = new Random(seed);
        List<Quest> quests = new ArrayList<>();
        
        // Generate 3 random quests from the 15 defined types
        List<QuestType> availableTypes = new ArrayList<>(List.of(
            QuestType.WIN_MATCHES,
            QuestType.DESTROY_CROWN_TOWERS,
            QuestType.PLAY_SPELL_CARDS,
            QuestType.DEPLOY_TROOP_CARDS,
            QuestType.SPEND_ELIXIR,
            QuestType.WIN_WITHOUT_LOSING_CROWN,
            QuestType.PLAY_BUILDING_CARDS,
            QuestType.DEAL_SPELL_DAMAGE,
            QuestType.WIN_ONLY_COMMON,
            QuestType.COMPLETE_CHALLENGES,
            QuestType.WIN_NETWORK_MATCH,
            QuestType.PLAY_20_CARDS_SINGLE_MATCH,
            QuestType.WIN_STREAK,
            QuestType.DESTROY_ENEMY_KING,
            QuestType.WIN_PVP_MATCH
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
                targetValue = 3;
                description = "Win 3 matches";
                rewardGold = 250;
                break;
            case DESTROY_CROWN_TOWERS:
                targetValue = 5;
                description = "Destroy 5 Crown Towers";
                rewardGold = 200;
                break;
            case PLAY_SPELL_CARDS:
                targetValue = 10;
                description = "Play 10 spell cards";
                rewardGold = 150;
                break;
            case DEPLOY_TROOP_CARDS:
                targetValue = 15;
                description = "Deploy 15 troop cards";
                rewardGold = 175;
                break;
            case SPEND_ELIXIR:
                targetValue = 100;
                description = "Spend 100 total Elixir";
                rewardGold = 100;
                break;
            case WIN_WITHOUT_LOSING_CROWN:
                targetValue = 1;
                description = "Win a match without losing a Crown Tower";
                rewardGold = 300;
                break;
            case PLAY_BUILDING_CARDS:
                targetValue = 5;
                description = "Play 5 building cards";
                rewardGold = 150;
                break;
            case DEAL_SPELL_DAMAGE:
                targetValue = 3000;
                description = "Deal 3000 damage with spells";
                rewardGold = 200;
                break;
            case WIN_ONLY_COMMON:
                targetValue = 1;
                description = "Win using only common cards";
                rewardGold = 250;
                break;
            case COMPLETE_CHALLENGES:
                targetValue = 2;
                description = "Complete 2 challenges";
                rewardGold = 300;
                break;
            case WIN_NETWORK_MATCH:
                targetValue = 1;
                description = "Win a network multiplayer match";
                rewardGold = 200;
                break;
            case PLAY_20_CARDS_SINGLE_MATCH:
                targetValue = 20;
                description = "Play 20 cards in a single match";
                rewardGold = 150;
                break;
            case WIN_STREAK:
                targetValue = 2;
                description = "Win 2 matches in a row";
                rewardGold = 300;
                break;
            case DESTROY_ENEMY_KING:
                targetValue = 1;
                description = "Destroy an enemy King Tower";
                rewardGold = 350;
                break;
            case WIN_PVP_MATCH:
                targetValue = 1;
                description = "Win a PvP match";
                rewardGold = 200;
                break;
            default:
                targetValue = 1;
                description = "Complete quest";
                rewardGold = 50;
        }
        
        return new Quest(id, description, targetValue, rewardGold, type);
    }
}

