package kuroyale.emote;

public class EmoteEvent {
    private final int playerId;
    private final EmoteType emoteType;
    private final long timestamp;

    public EmoteEvent(int playerId, EmoteType emoteType, long timestamp) {
        this.playerId = playerId;
        this.emoteType = emoteType;
        this.timestamp = timestamp;
    }

    public int getPlayerId() {
        return playerId;
    }

    public EmoteType getEmoteType() {
        return emoteType;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
