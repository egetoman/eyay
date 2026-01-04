package application.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads {@code network_config.txt}.
 * <p>
 * Lookup order:
 * - {@code ./network_config.txt} (project root / working directory)
 * - {@code ~/.kuroyale/network_config.txt}
 * <p>
 * If missing or invalid, defaults are used.
 */
public class NetworkConfigService {

    private static final Path PROJECT_FILE = Paths.get("network_config.txt");
    private static final Path HOME_FILE = Paths.get(System.getProperty("user.home"), ".kuroyale", "network_config.txt");

    public NetworkConfig load() {
        NetworkConfig defaults = NetworkConfig.defaults();
        Map<String, String> kv = new HashMap<>();

        if (Files.exists(PROJECT_FILE)) {
            kv.putAll(readKeyValues(PROJECT_FILE));
        } else if (Files.exists(HOME_FILE)) {
            kv.putAll(readKeyValues(HOME_FILE));
        }

        int port = intOr(kv.get("DEFAULT_PORT"), defaults.getDefaultPort(), 1, 65535);
        int timeout = intOr(kv.get("CONNECTION_TIMEOUT"), defaults.getConnectionTimeoutMillis(), 250, 60000);
        int retries = intOr(kv.get("RECONNECT_ATTEMPTS"), defaults.getReconnectAttempts(), 0, 20);
        int sync = intOr(kv.get("SYNC_INTERVAL"), defaults.getSyncIntervalMillis(), 50, 2000);
        int max = intOr(kv.get("MAX_MESSAGE_SIZE"), defaults.getMaxMessageSize(), 256, 1024 * 1024);

        return new NetworkConfig(port, timeout, retries, sync, max);
    }

    private Map<String, String> readKeyValues(Path file) {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(file)) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int idx = trimmed.indexOf('=');
                if (idx <= 0 || idx >= trimmed.length() - 1) {
                    continue;
                }
                String key = trimmed.substring(0, idx).trim();
                String val = trimmed.substring(idx + 1).trim();
                if (!key.isEmpty() && !val.isEmpty()) {
                    map.put(key, val);
                }
            }
        } catch (IOException ignored) {
        }
        return map;
    }

    private int intOr(String raw, int fallback, int min, int max) {
        if (raw == null) {
            return fallback;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            if (v < min || v > max) {
                return fallback;
            }
            return v;
        } catch (Exception e) {
            return fallback;
        }
    }
}


