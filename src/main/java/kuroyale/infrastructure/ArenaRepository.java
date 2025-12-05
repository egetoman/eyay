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
import kuroyale.domain.ArenaLayout;

public class ArenaRepository {

    private static final Path STORAGE_PATH = Paths.get(System.getProperty("user.home"), ".kuroyale", "arenas.json");
    private static final Type LIST_TYPE = new TypeToken<List<ArenaLayout>>() { }.getType();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public List<ArenaLayout> loadAll() {
        if (!Files.exists(STORAGE_PATH)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(STORAGE_PATH)) {
            List<ArenaLayout> layouts = gson.fromJson(reader, LIST_TYPE);
            if (layouts == null) {
                return new ArrayList<>();
            }
            return layouts;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public void saveAll(List<ArenaLayout> layouts) {
        try {
            Files.createDirectories(STORAGE_PATH.getParent());
        } catch (IOException ignored) {
        }
        try (Writer writer = Files.newBufferedWriter(STORAGE_PATH)) {
            gson.toJson(layouts, LIST_TYPE, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist arena layouts", e);
        }
    }
}

