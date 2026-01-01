package kuroyale.infrastructure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import kuroyale.domain.MatchRecord;

public class MatchHistoryRepository {
    
    private static final Path STORAGE_PATH = Paths.get(System.getProperty("user.home"), ".kuroyale", "match_history.json");
    private static final Type LIST_TYPE = new TypeToken<List<MatchRecord>>() { }.getType();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public List<MatchRecord> loadAll() {
        if (!Files.exists(STORAGE_PATH)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(STORAGE_PATH)) {
            List<MatchRecord> records = gson.fromJson(reader, LIST_TYPE);
            if (records == null) {
                return new ArrayList<>();
            }
            // Sort by date, most recent first
            records.sort((a, b) -> {
                if (a.getDateTime() == null || b.getDateTime() == null) {
                    return 0;
                }
                return b.getDateTime().compareTo(a.getDateTime());
            });
            return records;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
    
    public void save(MatchRecord record) {
        List<MatchRecord> records = loadAll();
        records.add(0, record); // Add to beginning (most recent first)
        saveAll(records);
    }
    
    public void saveAll(List<MatchRecord> records) {
        try {
            Files.createDirectories(STORAGE_PATH.getParent());
        } catch (IOException ignored) {
        }
        
        try (Writer writer = Files.newBufferedWriter(STORAGE_PATH)) {
            gson.toJson(records, LIST_TYPE, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save match history", e);
        }
    }
}

