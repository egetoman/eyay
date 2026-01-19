package kuroyale.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.awt.Desktop;

/**
 * Simple file logger for in-game events. Appends to ~/.kuroyale/game_events.log.
 */
public final class GameEventLogger {
    private static final Path LOG_PATH = Paths.get(System.getProperty("user.home"), ".kuroyale", "game_events.log");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    private GameEventLogger() {
    }

    /**
     * Clears the log file at the start of each match.
     */
    public static void resetForMatch() {
        try {
            Files.createDirectories(LOG_PATH.getParent());
            Files.writeString(LOG_PATH, "", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
            // Logging should never break gameplay.
        }
    }

    /**
     * Ensures the log file exists without truncating it.
     */
    public static void ensureLogFile() {
        try {
            Files.createDirectories(LOG_PATH.getParent());
            if (!Files.exists(LOG_PATH)) {
                Files.writeString(LOG_PATH, "", StandardOpenOption.CREATE);
            }
        } catch (IOException ignored) {
            // Logging should never break gameplay.
        }
    }

    public static void log(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String line = FORMATTER.format(Instant.now()) + " | " + message + System.lineSeparator();
        try {
            Files.createDirectories(LOG_PATH.getParent());
            Files.writeString(LOG_PATH, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Logging should never break gameplay.
        }
    }

    public static Path getLogPath() {
        return LOG_PATH;
    }

    /**
     * Opens the log file with the OS default app.
     */
    public static void openLogFile() {
        try {
            Files.createDirectories(LOG_PATH.getParent());
            if (!Files.exists(LOG_PATH)) {
                Files.writeString(LOG_PATH, "", StandardOpenOption.CREATE);
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(LOG_PATH.toFile());
            }
        } catch (IOException ignored) {
            // Opening the log should never break gameplay.
        }
    }
}
