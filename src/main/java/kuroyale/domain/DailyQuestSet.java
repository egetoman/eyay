package kuroyale.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DailyQuestSet {
    private String dateString; // Store as string for Gson compatibility
    private List<Quest> quests;
    private List<QuestProgress> progress;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    public DailyQuestSet() {
        this.dateString = LocalDate.now().format(DATE_FORMATTER);
        this.quests = new ArrayList<>();
        this.progress = new ArrayList<>();
    }
    
    public DailyQuestSet(LocalDate date, List<Quest> quests, List<QuestProgress> progress) {
        this.dateString = date != null ? date.format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER);
        this.quests = quests != null ? new ArrayList<>(quests) : new ArrayList<>();
        
        // If progress is null but quests are provided, initialize progress for each quest
        if (progress != null) {
            this.progress = new ArrayList<>(progress);
        } else {
            this.progress = new ArrayList<>();
            // Initialize progress for all provided quests
            if (quests != null) {
                for (Quest quest : quests) {
                    if (quest != null) {
                        this.progress.add(new QuestProgress(quest.getId(), 0, QuestStatus.IN_PROGRESS));
                    }
                }
            }
        }
    }
    
    public LocalDate getDate() {
        if (dateString == null) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
    
    public void setDate(LocalDate date) {
        this.dateString = date != null ? date.format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER);
    }
    
    public String getDateString() {
        return dateString;
    }
    
    public void setDateString(String dateString) {
        this.dateString = dateString;
    }
    
    public List<Quest> getQuests() {
        return new ArrayList<>(quests);
    }
    
    public void setQuests(List<Quest> quests) {
        this.quests = quests != null ? new ArrayList<>(quests) : new ArrayList<>();
    }
    
    public List<QuestProgress> getProgress() {
        if (progress == null) {
            progress = new ArrayList<>();
        }
        return new ArrayList<>(progress);
    }
    
    public void setProgress(List<QuestProgress> progress) {
        this.progress = progress != null ? new ArrayList<>(progress) : new ArrayList<>();
    }
    
    public QuestProgress getProgressForQuest(String questId) {
        if (questId == null) {
            return null;
        }
        // Ensure progress list exists (Gson might set it to null during deserialization)
        if (progress == null) {
            progress = new ArrayList<>();
        }
        // Find existing progress
        QuestProgress existing = progress.stream()
            .filter(p -> p != null && questId.equals(p.getQuestId()))
            .findFirst()
            .orElse(null);
        
        // If not found, create it (handles deserialization cases where progress is empty)
        if (existing == null) {
            existing = new QuestProgress(questId, 0, QuestStatus.IN_PROGRESS);
            progress.add(existing);
        }
        return existing;
    }
    
    public void addQuest(Quest quest) {
        if (quest == null) {
            return;
        }
        // Add quest if not already present
        if (!quests.contains(quest)) {
            quests.add(quest);
        }
        // Ensure progress exists for this quest
        QuestProgress existing = getProgressForQuest(quest.getId());
        if (existing == null) {
            progress.add(new QuestProgress(quest.getId(), 0, QuestStatus.IN_PROGRESS));
        }
    }
}

