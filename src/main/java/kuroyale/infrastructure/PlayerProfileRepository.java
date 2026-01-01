package kuroyale.infrastructure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import kuroyale.domain.PlayerProfile;

public class PlayerProfileRepository {
    
    private static final Path STORAGE_PATH = Paths.get(System.getProperty("user.home"), ".kuroyale", "player_profile.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public PlayerProfile load() {
        if (!Files.exists(STORAGE_PATH)) {
            return createDefaultProfile();
        }
        try (Reader reader = Files.newBufferedReader(STORAGE_PATH)) {
            PlayerProfile profile = gson.fromJson(reader, PlayerProfile.class);
            return profile != null ? profile : createDefaultProfile();
        } catch (IOException e) {
            return createDefaultProfile();
        }
    }
    
    public void save(PlayerProfile profile) {
        if (profile == null) {
            return;
        }
        try {
            Files.createDirectories(STORAGE_PATH.getParent());
        } catch (IOException ignored) {
        }
        
        try (Writer writer = Files.newBufferedWriter(STORAGE_PATH)) {
            gson.toJson(profile, PlayerProfile.class, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save player profile", e);
        }
    }
    
    private PlayerProfile createDefaultProfile() {
        PlayerProfile profile = new PlayerProfile("Player", 1000); // Start with 1000 gold
        return profile;
    }
}

