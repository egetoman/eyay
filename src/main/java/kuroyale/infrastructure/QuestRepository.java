package kuroyale.infrastructure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import kuroyale.domain.DailyQuestSet;

public class QuestRepository {
    
    private static final Path STORAGE_DIR = Paths.get(System.getProperty("user.home"), ".kuroyale", "quests");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    
    public DailyQuestSet loadForDate(LocalDate date) {
        Path filePath = getFilePath(date);
        if (!Files.exists(filePath)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(filePath)) {
            return gson.fromJson(reader, DailyQuestSet.class);
        } catch (IOException e) {
            return null;
        }
    }
    
    public void save(DailyQuestSet questSet) {
        if (questSet == null) {
            return;
        }
        try {
            Files.createDirectories(STORAGE_DIR);
        } catch (IOException ignored) {
        }
        
        Path filePath = getFilePath(questSet.getDate());
        try (Writer writer = Files.newBufferedWriter(filePath)) {
            gson.toJson(questSet, DailyQuestSet.class, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save quest data", e);
        }
    }
    
    private Path getFilePath(LocalDate date) {
        return STORAGE_DIR.resolve("quests_" + date.format(DATE_FORMATTER) + ".json");
    }
}

