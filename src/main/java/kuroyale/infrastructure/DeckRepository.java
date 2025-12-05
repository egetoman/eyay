package kuroyale.infrastructure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import kuroyale.domain.Deck;

public class DeckRepository {

    private static final Path STORAGE_PATH = Paths.get(System.getProperty("user.home"), ".kuroyale", "deck.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public Deck load() {
        if (!Files.exists(STORAGE_PATH)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(STORAGE_PATH)) {
            return gson.fromJson(reader, Deck.class);
        } catch (IOException e) {
            return null;
        }
    }

    public void save(Deck deck) {
        if (deck == null) {
            return;
        }
        try {
            Files.createDirectories(STORAGE_PATH.getParent());
        } catch (IOException ignored) {
        }

        try (Writer writer = Files.newBufferedWriter(STORAGE_PATH)) {
            gson.toJson(deck, Deck.class, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save deck data", e);
        }
    }
}




