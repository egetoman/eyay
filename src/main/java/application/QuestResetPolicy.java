package application;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface QuestResetPolicy {
    boolean shouldReset(LocalDateTime lastResetAt);
    LocalDate getResetDate();
}

