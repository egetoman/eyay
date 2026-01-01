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
        this.progress = progress != null ? new ArrayList<>(progress) : new ArrayList<>();
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
        return new ArrayList<>(progress);
    }
    
    public void setProgress(List<QuestProgress> progress) {
        this.progress = progress != null ? new ArrayList<>(progress) : new ArrayList<>();
    }
    
    public QuestProgress getProgressForQuest(String questId) {
        return progress.stream()
            .filter(p -> p.getQuestId().equals(questId))
            .findFirst()
            .orElse(null);
    }
    
    public void addQuest(Quest quest) {
        if (quest != null && !quests.contains(quest)) {
            quests.add(quest);
            progress.add(new QuestProgress(quest.getId(), 0, QuestStatus.IN_PROGRESS));
        }
    }
}

