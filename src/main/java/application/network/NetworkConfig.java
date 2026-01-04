package application.network;

/**
 * Configuration for Phase 2 Network Multiplayer.
 * Values are loaded from {@code network_config.txt} with sensible defaults.
 */
public class NetworkConfig {
    private final int defaultPort;
    private final int connectionTimeoutMillis;
    private final int reconnectAttempts;
    private final int syncIntervalMillis;
    private final int maxMessageSize;

    public NetworkConfig(int defaultPort,
                         int connectionTimeoutMillis,
                         int reconnectAttempts,
                         int syncIntervalMillis,
                         int maxMessageSize) {
        this.defaultPort = defaultPort;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
        this.reconnectAttempts = reconnectAttempts;
        this.syncIntervalMillis = syncIntervalMillis;
        this.maxMessageSize = maxMessageSize;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public int getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    public int getReconnectAttempts() {
        return reconnectAttempts;
    }

    public int getSyncIntervalMillis() {
        return syncIntervalMillis;
    }

    public int getMaxMessageSize() {
        return maxMessageSize;
    }

    public static NetworkConfig defaults() {
        return new NetworkConfig(8080, 5000, 3, 200, 1024);
    }
}


