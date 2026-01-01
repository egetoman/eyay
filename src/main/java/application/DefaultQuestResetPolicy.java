package application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DefaultQuestResetPolicy implements QuestResetPolicy {
    
    private static final int RESET_HOURS = 24;
    
    @Override
    public boolean shouldReset(LocalDateTime lastResetAt) {
        if (lastResetAt == null) {
            return true;
        }
        long hoursSinceReset = ChronoUnit.HOURS.between(lastResetAt, LocalDateTime.now());
        return hoursSinceReset >= RESET_HOURS;
    }
    
    @Override
    public LocalDate getResetDate() {
        return LocalDate.now();
    }
}

