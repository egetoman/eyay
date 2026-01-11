package kuroyale.infrastructure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import kuroyale.domain.MatchRecord;

public class MatchHistoryRepository {
    
    private static final Path STORAGE_PATH = Paths.get(System.getProperty("user.home"), ".kuroyale", "match_history.json");
    private static final Type LIST_TYPE = new TypeToken<List<MatchRecord>>() { }.getType();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            // Java 16+ strong encapsulation prevents reflective access into java.time.*.
            // Serialize as ISO string to avoid InaccessibleObjectException.
            .registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
                @Override
                public void write(JsonWriter out, LocalDateTime value) throws IOException {
                    if (out == null) {
                        return;
                    }
                    if (value == null) {
                        out.nullValue();
                        return;
                    }
                    out.value(value.format(DATE_TIME_FORMATTER));
                }

                @Override
                public LocalDateTime read(JsonReader in) throws IOException {
                    if (in == null) {
                        return null;
                    }
                    if (in.peek() == JsonToken.NULL) {
                        in.nextNull();
                        return null;
                    }
                    try {
                        return LocalDateTime.parse(in.nextString(), DATE_TIME_FORMATTER);
                    } catch (Exception e) {
                        return null;
                    }
                }
            })
            .create();
    
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

