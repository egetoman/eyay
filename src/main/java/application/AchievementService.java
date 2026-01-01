package application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kuroyale.domain.Achievement;
import kuroyale.domain.AchievementProgress;
import kuroyale.domain.AchievementType;
import kuroyale.infrastructure.AchievementRepository;
import kuroyale.infrastructure.PlayerProfileRepository;
import kuroyale.support.Result;

public class AchievementService {
    
    private final AchievementRepository achievementRepository;
    private final PlayerProfileRepository profileRepository;
    private final List<Achievement> allAchievements;
    
    public AchievementService(AchievementRepository achievementRepository, PlayerProfileRepository profileRepository) {
        this.achievementRepository = achievementRepository;
        this.profileRepository = profileRepository;
        this.allAchievements = initializeAchievements();
    }
    
    private List<Achievement> initializeAchievements() {
        List<Achievement> achievements = new ArrayList<>();
        
        achievements.add(new Achievement("ach_win_10", "First Victory", "Win 10 matches", 10, 100, AchievementType.WIN_TOTAL_MATCHES));
        achievements.add(new Achievement("ach_win_50", "Veteran", "Win 50 matches", 50, 500, AchievementType.WIN_TOTAL_MATCHES));
        achievements.add(new Achievement("ach_win_100", "Champion", "Win 100 matches", 100, 1000, AchievementType.WIN_TOTAL_MATCHES));
        
        achievements.add(new Achievement("ach_streak_5", "Hot Streak", "Win 5 matches in a row", 5, 200, AchievementType.WIN_STREAK));
        achievements.add(new Achievement("ach_streak_10", "Unstoppable", "Win 10 matches in a row", 10, 500, AchievementType.WIN_STREAK));
        
        achievements.add(new Achievement("ach_crowns_100", "Crown Collector", "Earn 100 total crowns", 100, 300, AchievementType.TOTAL_CROWNS));
        achievements.add(new Achievement("ach_crowns_500", "Crown Master", "Earn 500 total crowns", 500, 1500, AchievementType.TOTAL_CROWNS));
        
        achievements.add(new Achievement("ach_upgrade_5", "Upgrade Enthusiast", "Upgrade 5 cards", 5, 150, AchievementType.CARDS_UPGRADED));
        achievements.add(new Achievement("ach_upgrade_20", "Upgrade Master", "Upgrade 20 cards", 20, 600, AchievementType.CARDS_UPGRADED));
        
        achievements.add(new Achievement("ach_quest_10", "Quest Hunter", "Complete 10 daily quests", 10, 250, AchievementType.QUESTS_COMPLETED));
        achievements.add(new Achievement("ach_quest_50", "Quest Master", "Complete 50 daily quests", 50, 1000, AchievementType.QUESTS_COMPLETED));
        
        return achievements;
    }
    
    public List<Achievement> getAllAchievements() {
        return new ArrayList<>(allAchievements);
    }
    
    public Map<String, AchievementProgress> getProgress() {
        return new HashMap<>(achievementRepository.loadAll());
    }
    
    public void updateProgress(AchievementType type, int amount) {
        Map<String, AchievementProgress> progressMap = achievementRepository.loadAll();
        var profile = profileRepository.load();
        int totalReward = 0;
        
        for (Achievement achievement : allAchievements) {
            if (achievement.getType() != type) {
                continue;
            }
            
            AchievementProgress progress = progressMap.get(achievement.getId());
            if (progress == null) {
                progress = new AchievementProgress(achievement.getId(), 0, false);
            }
            
            if (progress.isUnlocked()) {
                continue; // Already unlocked
            }
            
            progress.addProgress(amount);
            
            if (progress.isCompleted(achievement) && !progress.isUnlocked()) {
                progress.setUnlocked(true);
                totalReward += achievement.getRewardGold();
            }
            
            progressMap.put(achievement.getId(), progress);
        }
        
        if (totalReward > 0) {
            profile.addGold(totalReward);
            profileRepository.save(profile);
        }
        
        achievementRepository.saveAll(progressMap);
    }
    
    public Result<Integer> claimAchievementReward(String achievementId) {
        Achievement achievement = allAchievements.stream()
            .filter(a -> a.getId().equals(achievementId))
            .findFirst()
            .orElse(null);
        
        if (achievement == null) {
            return Result.fail("Achievement not found");
        }
        
        Map<String, AchievementProgress> progressMap = achievementRepository.loadAll();
        AchievementProgress progress = progressMap.get(achievementId);
        
        if (progress == null || !progress.isUnlocked()) {
            return Result.fail("Achievement not unlocked");
        }
        
        // Rewards are automatically granted when unlocked, so this is just for display
        return Result.ok(achievement.getRewardGold());
    }
}

