package application;

import java.time.LocalDate;
import java.util.List;
import kuroyale.domain.DailyQuestSet;
import kuroyale.domain.Quest;
import kuroyale.domain.QuestProgress;
import kuroyale.domain.QuestStatus;
import kuroyale.domain.QuestType;
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
        LocalDate today = resetPolicy.getResetDate();
        DailyQuestSet questSet = questRepository.loadForDate(today);

        LocalDate existingDate = questSet != null ? questSet.getDate() : null;
        java.time.LocalDateTime lastResetAt = existingDate != null ? existingDate.atStartOfDay() : null;
        boolean shouldReset = questSet == null
                || existingDate == null
                || !existingDate.equals(today)
                || resetPolicy.shouldReset(lastResetAt);

        if (shouldReset) {
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

    public int countUnclaimedRewards() {
        DailyQuestSet questSet = getTodayQuests();
        if (questSet == null) {
            return 0;
        }
        int count = 0;
        for (QuestProgress progress : questSet.getProgress()) {
            if (progress != null && progress.getStatus() == QuestStatus.COMPLETED) {
                count++;
            }
        }
        return count;
    }

    /**
     * Records a match result for win streak tracking.
     * Call this with isWin=true on victory, isWin=false on loss/draw.
     * Updates WIN_STREAK quest progress when appropriate.
     * 
     * @param isWin true if the player won the match
     */
    public void recordMatchResult(boolean isWin) {
        var profile = profileRepository.load();
        
        if (isWin) {
            profile.incrementWinStreak();
            profileRepository.save(profile);
            
            // Update WIN_STREAK quest with the current streak value
            // This allows tracking consecutive wins
            updateProgress(QuestType.WIN_STREAK, 1);
        } else {
            profile.resetWinStreak();
            profileRepository.save(profile);
        }
    }

    /**
     * Updates progress for all quests of the given type.
     * If any quest reaches its target, it is marked as COMPLETED.
     * 
     * @param type   The type of quest to update (e.g., WIN_MATCHES, DESTROY_CROWN_TOWERS)
     * @param amount The amount of progress to add
     */
    public void updateProgress(QuestType type, int amount) {
        if (type == null || amount <= 0) {
            return;
        }
        
        DailyQuestSet questSet = getTodayQuests();
        if (questSet == null) {
            return;
        }
        
        boolean anyUpdated = false;
        
        for (Quest quest : questSet.getQuests()) {
            if (quest == null || quest.getType() != type) {
                continue;
            }
            
            QuestProgress progress = questSet.getProgressForQuest(quest.getId());
            if (progress == null) {
                continue;
            }
            
            // Skip if already completed or claimed
            if (progress.isCompleted()) {
                continue;
            }
            
            // Add progress
            progress.addProgress(amount);
            anyUpdated = true;
            
            // Check if quest is now completed
            if (progress.getCurrentProgress() >= quest.getTargetValue()) {
                progress.markCompleted();
            }
        }
        
        // Save if any progress was updated
        if (anyUpdated) {
            questRepository.save(questSet);
        }
    }
}

