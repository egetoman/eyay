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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kuroyale.domain.AchievementProgress;

public class AchievementRepository {
    
    private static final Path STORAGE_PATH = Paths.get(System.getProperty("user.home"), ".kuroyale", "achievements.json");
    private static final Type MAP_TYPE = new TypeToken<Map<String, AchievementProgress>>() { }.getType();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public Map<String, AchievementProgress> loadAll() {
        if (!Files.exists(STORAGE_PATH)) {
            return new HashMap<>();
        }
        try (Reader reader = Files.newBufferedReader(STORAGE_PATH)) {
            Map<String, AchievementProgress> progress = gson.fromJson(reader, MAP_TYPE);
            return progress != null ? progress : new HashMap<>();
        } catch (IOException e) {
            return new HashMap<>();
        }
    }
    
    public void saveAll(Map<String, AchievementProgress> progress) {
        try {
            Files.createDirectories(STORAGE_PATH.getParent());
        } catch (IOException ignored) {
        }
        
        try (Writer writer = Files.newBufferedWriter(STORAGE_PATH)) {
            gson.toJson(progress, MAP_TYPE, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save achievement data", e);
        }
    }
}

