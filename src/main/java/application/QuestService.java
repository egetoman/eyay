package application;

import java.time.LocalDate;
import java.util.List;
import kuroyale.domain.DailyQuestSet;
import kuroyale.domain.Quest;
import kuroyale.domain.QuestProgress;
import kuroyale.domain.QuestStatus;
import kuroyale.infrastructure.PlayerProfileRepository;
import kuroyale.infrastructure.QuestRepository;
import kuroyale.support.Result;

public class QuestService {
    
    private final QuestRepository questRepository;
    private final PlayerProfileRepository profileRepository;
    private final QuestGenerator questGenerator;
    private final QuestResetPolicy resetPolicy;
    
    public QuestService(QuestRepository questRepository,
                       PlayerProfileRepository profileRepository,
                       QuestGenerator questGenerator,
                       QuestResetPolicy resetPolicy) {
        this.questRepository = questRepository;
        this.profileRepository = profileRepository;
        this.questGenerator = questGenerator;
        this.resetPolicy = resetPolicy;
    }
    
    public DailyQuestSet getTodayQuests() {
        LocalDate today = LocalDate.now();
        DailyQuestSet questSet = questRepository.loadForDate(today);
        
        if (questSet == null || resetPolicy.shouldReset(null)) {
            // Generate new quests for today
            long seed = today.toEpochDay();
            List<Quest> quests = questGenerator.generateDailyQuests(seed);
            questSet = new DailyQuestSet(today, quests, null);
            
            // Initialize progress for each quest
            for (Quest quest : quests) {
                questSet.addQuest(quest);
            }
            
            questRepository.save(questSet);
        }
        
        return questSet;
    }
    
    public Result<Integer> claimReward(String questId) {
        LocalDate today = LocalDate.now();
        DailyQuestSet questSet = questRepository.loadForDate(today);
        
        if (questSet == null) {
            return Result.fail("No quests found for today");
        }
        
        QuestProgress progress = questSet.getProgressForQuest(questId);
        if (progress == null) {
            return Result.fail("Quest not found");
        }
        
        if (progress.isClaimed()) {
            return Result.fail("Reward already claimed");
        }
        
        if (progress.getStatus() != QuestStatus.COMPLETED) {
            return Result.fail("Quest not completed");
        }
        
        // Find the quest to get reward amount
        Quest quest = questSet.getQuests().stream()
            .filter(q -> q.getId().equals(questId))
            .findFirst()
            .orElse(null);
        
        if (quest == null) {
            return Result.fail("Quest not found");
        }
        
        // Mark as claimed and add gold
        progress.markClaimed();
        var profile = profileRepository.load();
        profile.addGold(quest.getRewardGold());
        
        try {
            questRepository.save(questSet);
            profileRepository.save(profile);
            return Result.ok(quest.getRewardGold());
        } catch (Exception e) {
            // Revert changes
            progress.setStatus(QuestStatus.COMPLETED);
            profile.spendGold(quest.getRewardGold());
            return Result.fail("Save failed - reverted: " + e.getMessage());
        }
    }
}

