package application.network;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Consumer;
import kuroyale.domain.Card;
import kuroyale.domain.Deck;
import kuroyale.support.Result;

/**
 * Coordinates the lobby handshake (names, deck preview, ready states) over a {@link NetworkAdapter}.
 * <p>
 * Host is playerId=1, client is playerId=2 (simple convention).
 */
public class NetworkLobbyController implements NetworkAdapter.Listener {

    private final NetworkAdapter adapter;
    private final NetworkConfig config;
    private final boolean isHost;
    private final int localPlayerId;
    private final NetworkLobbyState state = new NetworkLobbyState();
    private Consumer<NetworkLobbyState> onStateChanged;
    private java.util.function.BiConsumer<Boolean, String> onStartMatch; // (isHostSide, startPayload)
    private Instant lastPingSentAt;
    private String startPayload = "";
    private String connectHost;
    private int connectPort;
    private Integer hostPort;
    private boolean autoStartedFromSnapshot;
    private String lastReceivedStartPayload = "";
    private String lastReceivedSnapshotJson = "";

    public NetworkLobbyController(NetworkAdapter adapter, NetworkConfig config, boolean isHost,
                                  String localName, Deck localDeck) {
        this.adapter = adapter;
        this.config = config != null ? config : NetworkConfig.defaults();
        this.isHost = isHost;
        this.localPlayerId = isHost ? 1 : 2;
        state.setHost(isHost);
        state.setLocalName(localName);
        state.setLocalDeckCardIds(deckCardIds(localDeck));
        state.setLocalDeckNames(idsToNames(state.getLocalDeckCardIds()));
    }

    public NetworkLobbyState getState() {
        return state;
    }

    public void setOnStateChanged(Consumer<NetworkLobbyState> onStateChanged) {
        this.onStateChanged = onStateChanged;
    }

    public void setOnStartMatch(Runnable onStartMatch) {
        this.onStartMatch = (ignored, payload) -> onStartMatch.run();
    }

    public void setOnStartMatch(java.util.function.BiConsumer<Boolean, String> onStartMatch) {
        this.onStartMatch = onStartMatch;
    }

    public void setStartPayload(String payload) {
        this.startPayload = payload != null ? payload : "";
    }

    public NetworkAdapter getAdapter() {
        return adapter;
    }

    public NetworkConfig getConfig() {
        return config;
    }

    public void startHost(int port) {
        this.hostPort = port;
        adapter.host(port, this);
    }

    public void startClient(String host, int port) {
        this.connectHost = host;
        this.connectPort = port;
        adapter.connect(host, port, this);
    }

    public String getConnectHost() {
        return connectHost;
    }

    public int getConnectPort() {
        return connectPort;
    }

    public Integer getHostPort() {
        return hostPort;
    }

    public String getLastReceivedStartPayload() {
        return lastReceivedStartPayload;
    }

    public String getLastReceivedSnapshotJson() {
        return lastReceivedSnapshotJson;
    }

    public void toggleReady() {
        state.setLocalReady(!state.isLocalReady());
        adapter.send(new NetworkMessage(NetworkMessageType.READY, localPlayerId, String.valueOf(state.isLocalReady()), nowStamp()));
        emit();
    }

    public Result<?> tryStartMatch() {
        if (!isHost) {
            return Result.fail("Only the host can start the match.");
        }
        if (!state.isLocalReady() || !state.isRemoteReady()) {
            return Result.fail("Both players must be Ready.");
        }
        adapter.send(new NetworkMessage(NetworkMessageType.START, localPlayerId, startPayload, nowStamp()));
        if (onStartMatch != null) {
            onStartMatch.accept(true, startPayload);
        }
        return Result.ok(null);
    }

    public void close() {
        try {
            adapter.send(new NetworkMessage(NetworkMessageType.DISCONNECT, localPlayerId, "", nowStamp()));
        } catch (Exception ignored) {
        }
        adapter.close();
    }

    @Override
    public void onStatus(ConnectionStatus status, String detail) {
        state.setStatus(status);
        state.setStatusDetail(detail);
        emit();
        if (status == ConnectionStatus.CONNECTED) {
            // Send HELLO + DECK on connect
            adapter.send(new NetworkMessage(NetworkMessageType.HELLO, localPlayerId, safe(state.getLocalName()), nowStamp()));
            adapter.send(new NetworkMessage(NetworkMessageType.DECK, localPlayerId, join(state.getLocalDeckCardIds()), nowStamp()));
            // Start ping loop (simple)
            sendPing();
        }
    }

    @Override
    public void onMessage(NetworkMessage message) {
        if (message == null || message.getType() == null) {
            return;
        }
        switch (message.getType()) {
            case HELLO:
                state.setRemoteName(message.getData());
                emit();
                break;
            case DECK:
                List<String> ids = splitCsv(message.getData());
                state.setRemoteDeckCardIds(ids);
                state.setRemoteDeckNames(idsToNames(ids));
                emit();
                break;
            case READY:
                state.setRemoteReady("true".equalsIgnoreCase(message.getData()));
                emit();
                break;
            case START:
                // Host may include start payload (layout, etc)
                lastReceivedStartPayload = message.getData() != null ? message.getData() : "";
                if (onStartMatch != null) {
                    onStartMatch.accept(false, message.getData());
                }
                break;
            case STATE_SNAPSHOT:
                lastReceivedSnapshotJson = message.getData() != null ? message.getData() : "";
                // If a host is already in-match, it may stream snapshots immediately.
                // Use the snapshot as a signal to auto-transition into match view (reconnect scenario).
                if (!autoStartedFromSnapshot && onStartMatch != null) {
                    autoStartedFromSnapshot = true;
                    onStartMatch.accept(false, lastReceivedStartPayload);
                }
                break;
            case PING:
                adapter.send(new NetworkMessage(NetworkMessageType.PONG, localPlayerId, message.getData(), nowStamp()));
                break;
            case PONG:
                handlePong(message.getData());
                break;
            default:
                // Ignore other types in lobby
                break;
        }
    }

    @Override
    public void onError(String message, Exception error) {
        state.setStatus(ConnectionStatus.DISCONNECTED);
        state.setStatusDetail(message != null ? message : "Network error");
        emit();
    }

    private void sendPing() {
        lastPingSentAt = Instant.now();
        adapter.send(new NetworkMessage(NetworkMessageType.PING, localPlayerId, String.valueOf(lastPingSentAt.toEpochMilli()), nowStamp()));
    }

    private void handlePong(String payload) {
        if (payload == null || lastPingSentAt == null) {
            return;
        }
        try {
            long sentMillis = Long.parseLong(payload.trim());
            long nowMillis = Instant.now().toEpochMilli();
            int rtt = (int) Math.max(0, nowMillis - sentMillis);
            state.setLatencyMillis(rtt);
            emit();
            // schedule next ping very roughly via callers; keep it simple: ping again when we receive a pong
            sendPing();
        } catch (Exception ignored) {
        }
    }

    private void emit() {
        if (onStateChanged != null) {
            onStateChanged.accept(state);
        }
    }

    private List<String> deckCardIds(Deck deck) {
        if (deck == null || deck.getCards() == null) {
            return java.util.Collections.emptyList();
        }
        return deck.getCards().stream()
            .filter(Objects::nonNull)
            .map(Card::getId)
            .filter(id -> id != null && !id.isBlank())
            .toList();
    }

    private List<String> idsToNames(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.util.Map<String, String> idToName = new java.util.HashMap<>();
        for (Card c : new kuroyale.infrastructure.CardCatalogRepository().findAll()) {
            if (c != null && c.getId() != null && c.getName() != null) {
                idToName.put(c.getId(), c.getName());
            }
        }
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                continue;
            }
            String trimmed = id.trim();
            out.add(idToName.getOrDefault(trimmed, trimmed));
        }
        return out;
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return java.util.Collections.emptyList();
        }
        String[] parts = csv.split(",");
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    private String join(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringJoiner sj = new StringJoiner(",");
        for (String s : items) {
            if (s != null && !s.isBlank()) {
                sj.add(s.trim());
            }
        }
        return sj.toString();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String nowStamp() {
        // Keep simple; match examples "mm:ss" is optional here.
        return String.valueOf(Instant.now().toEpochMilli());
    }
}


