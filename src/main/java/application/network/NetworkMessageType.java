package application.network;

/**
 * Network protocol message types (Phase 2 Feature 2).
 * <p>
 * Format: {@code TYPE|player_id|data|timestamp}
 */
public enum NetworkMessageType {
    HELLO,
    DECK,
    READY,
    START,
    PING,
    PONG,
    REQUEST_SNAPSHOT,
    INPUT_CARD_PLACED,
    STATE_SNAPSHOT,
    EMOTE_USED,
    DISCONNECT
}


