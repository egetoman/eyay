package application.network;

import java.util.Objects;

/**
 * Simple wire message wrapper for the Phase 2 network protocol.
 */
public class NetworkMessage {
    private final NetworkMessageType type;
    private final int playerId;
    private final String data;
    private final String timestamp;

    public NetworkMessage(NetworkMessageType type, int playerId, String data, String timestamp) {
        this.type = type;
        this.playerId = playerId;
        this.data = data != null ? data : "";
        this.timestamp = timestamp != null ? timestamp : "";
    }

    public NetworkMessageType getType() {
        return type;
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getData() {
        return data;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String encode() {
        // Keep it line-based; avoid newlines in payload.
        return type + "|" + playerId + "|" + sanitize(data) + "|" + sanitize(timestamp);
    }

    public static NetworkMessage parse(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.split("\\|", -1);
        if (parts.length < 4) {
            return null;
        }
        NetworkMessageType type;
        try {
            type = NetworkMessageType.valueOf(parts[0]);
        } catch (Exception e) {
            return null;
        }
        int pid;
        try {
            pid = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            pid = 0;
        }
        return new NetworkMessage(type, pid, parts[2], parts[3]);
    }

    private static String sanitize(String s) {
        return s.replace("\n", " ").replace("\r", " ");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NetworkMessage)) return false;
        NetworkMessage that = (NetworkMessage) o;
        return playerId == that.playerId && type == that.type
            && Objects.equals(data, that.data) && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, playerId, data, timestamp);
    }
}


