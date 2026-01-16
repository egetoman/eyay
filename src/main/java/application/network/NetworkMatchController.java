package application.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import kuroyale.domain.Arena;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.Card;
import kuroyale.domain.ElixirPhase;
import kuroyale.domain.Match;
import kuroyale.domain.MatchOutcome;
import kuroyale.domain.Player;
import kuroyale.domain.Position;
import kuroyale.domain.Tower;
import kuroyale.domain.TowerOwner;
import kuroyale.domain.Unit;
import kuroyale.infrastructure.CardCatalogRepository;
import kuroyale.support.Result;

/**
 * Controls a networked match.
 * <p>
 * Host-authoritative design:
 * - Host runs {@link Match#advanceTime(double)} and broadcasts {@link NetworkSnapshot}.
 * - Client sends inputs (card placement requests), and only renders snapshots.
 */
public class NetworkMatchController implements NetworkAdapter.Listener {

    private final NetworkAdapter adapter;
    private final NetworkConfig config;
    private final boolean isHost;
    private final int localPlayerId; // 1 host, 2 client
    private final Gson gson = new GsonBuilder().create();
    private final CardCatalogRepository cardCatalog = new CardCatalogRepository();

    private Match hostMatch;
    private ArenaLayout layout;

    // Client-side rendered state
    private NetworkSnapshot lastSnapshot;
    private final Map<String, Card> cardById = new HashMap<>();

    private Consumer<NetworkSnapshot> onSnapshot;
    private Consumer<String> onConnectionInfo;

    // Disconnect / reconnect handling (Phase 2 requirement)
    private volatile Instant opponentDisconnectedAt;
    private volatile boolean forcedFinished;
    private volatile Integer forcedWinnerPlayerId;
    private volatile String reconnectHost;
    private volatile int reconnectPort;

    private java.util.Set<String> allowedDeckIdsP1 = java.util.Collections.emptySet();
    private java.util.Set<String> allowedDeckIdsP2 = java.util.Collections.emptySet();

    private java.util.List<Card> localDeckCards = java.util.Collections.emptyList();

    public NetworkMatchController(NetworkAdapter adapter, NetworkConfig config, boolean isHost, int localPlayerId) {
        this.adapter = adapter;
        this.config = config != null ? config : NetworkConfig.defaults();
        this.isHost = isHost;
        this.localPlayerId = localPlayerId;
        for (Card c : cardCatalog.findAll()) {
            if (c != null && c.getId() != null) {
                cardById.put(c.getId(), c);
            }
        }
    }

    public void setOnSnapshot(Consumer<NetworkSnapshot> onSnapshot) {
        this.onSnapshot = onSnapshot;
    }

    public void setOnConnectionInfo(Consumer<String> onConnectionInfo) {
        this.onConnectionInfo = onConnectionInfo;
    }

    public int getLocalPlayerId() {
        return localPlayerId;
    }

    public boolean isHost() {
        return isHost;
    }

    public void attachHostMatch(Match match, ArenaLayout layout) {
        this.hostMatch = match;
        this.layout = layout;
    }

    public void attachLayoutForClient(ArenaLayout layout) {
        this.layout = layout;
    }

    public ArenaLayout getLayout() {
        return layout;
    }

    public NetworkSnapshot getLastSnapshot() {
        return lastSnapshot;
    }

    public void setAllowedDeckIds(int playerId, java.util.List<String> cardIds) {
        java.util.Set<String> set = new java.util.HashSet<>();
        if (cardIds != null) {
            for (String id : cardIds) {
                if (id != null && !id.isBlank()) {
                    set.add(id.trim());
                }
            }
        }
        if (playerId == 1) {
            allowedDeckIdsP1 = set;
        } else if (playerId == 2) {
            allowedDeckIdsP2 = set;
        }
    }

    public void setLocalDeck(kuroyale.domain.Deck deck) {
        if (deck == null || deck.getCards() == null) {
            localDeckCards = java.util.Collections.emptyList();
            return;
        }
        java.util.List<Card> cards = new java.util.ArrayList<>();
        for (Card c : deck.getCards()) {
            if (c != null) {
                cards.add(c);
            }
        }
        localDeckCards = cards;
    }

    public java.util.List<Card> getLocalDeckCards() {
        return new java.util.ArrayList<>(localDeckCards);
    }

    public Result<?> requestDeploy(Card card, Position position) {
        if (card == null || position == null) {
            return Result.fail("Invalid deployment.");
        }
        if (!isAllowedForPlayer(localPlayerId, card.getId())) {
            return Result.fail("Card is not in your deck.");
        }
        // Host applies its own input immediately (authoritative).
        if (isHost && hostMatch != null) {
            Player acting = localPlayerId == 1 ? hostMatch.getPlayer() : hostMatch.getOpponent();
            if (acting == null) {
                return Result.fail("Host player not available.");
            }
            return hostMatch.deployCard(acting, card, position);
        }

        String data = card.getId() + "," + position.getX() + "," + position.getY();
        adapter.send(new NetworkMessage(NetworkMessageType.INPUT_CARD_PLACED, localPlayerId, data, nowStamp()));
        return Result.ok(null);
    }

    public void setReconnectTarget(String host, int port) {
        this.reconnectHost = host;
        this.reconnectPort = port;
    }

    public void close() {
        try {
            adapter.send(new NetworkMessage(NetworkMessageType.DISCONNECT, localPlayerId, "", nowStamp()));
        } catch (Exception ignored) {
        }
        adapter.close();
    }

    /**
     * Client-side helper: asks the host to resend START + a fresh snapshot.
     * This removes timing issues during reconnect/navigation.
     */
    public void requestSnapshot() {
        if (isHost) {
            // Host can just emit a snapshot to its own UI; client will receive via adapter anyway.
            if (hostMatch != null) {
                deliverSnapshot(buildSnapshot(hostMatch));
            }
            return;
        }
        adapter.send(new NetworkMessage(NetworkMessageType.REQUEST_SNAPSHOT, localPlayerId, "", nowStamp()));
    }

    /**
     * Host-only tick: advances simulation and publishes a snapshot at most every {@code syncIntervalMillis}.
     */
    public void hostTick(double deltaSeconds) {
        if (!isHost || hostMatch == null) {
            return;
        }
        if (forcedFinished) {
            // Keep broadcasting final state for a short while; simplest: send one final snapshot and stop ticking externally.
            NetworkSnapshot snap = buildSnapshot(hostMatch);
            snap.finished = true;
            snap.winnerPlayerId = forcedWinnerPlayerId;
            adapter.send(new NetworkMessage(NetworkMessageType.STATE_SNAPSHOT, 0, gson.toJson(snap), nowStamp()));
            deliverSnapshot(snap);
            return;
        }

        // If opponent disconnected, allow 5s for reconnection; then award win to remaining player.
        if (opponentDisconnectedAt != null) {
            long elapsed = Duration.between(opponentDisconnectedAt, Instant.now()).toMillis();
            if (elapsed >= 5000) {
                forcedFinished = true;
                forcedWinnerPlayerId = localPlayerId;
                if (onConnectionInfo != null) {
                    onConnectionInfo.accept("Opponent disconnected - victory awarded");
                }
            }
        }
        hostMatch.advanceTime(deltaSeconds);
        NetworkSnapshot snap = buildSnapshot(hostMatch);
        adapter.send(new NetworkMessage(NetworkMessageType.STATE_SNAPSHOT, 0, gson.toJson(snap), nowStamp()));
        deliverSnapshot(snap);
    }

    @Override
    public void onStatus(ConnectionStatus status, String detail) {
        if (onConnectionInfo != null) {
            onConnectionInfo.accept((status != null ? status.name() : "UNKNOWN") + (detail != null ? (" - " + detail) : ""));
        }
        if (status == ConnectionStatus.CONNECTED) {
            opponentDisconnectedAt = null;
            // If we are host and a new connection arrives while a match is already running,
            // instruct the client lobby to jump directly into the match view.
            if (isHost && hostMatch != null && layout != null) {
                try {
                    adapter.send(new NetworkMessage(NetworkMessageType.START, 1, gson.toJson(layout), nowStamp()));
                    // Immediately send a snapshot so the client can render without waiting.
                    NetworkSnapshot snap = buildSnapshot(hostMatch);
                    adapter.send(new NetworkMessage(NetworkMessageType.STATE_SNAPSHOT, 0, gson.toJson(snap), nowStamp()));
                } catch (Exception ignored) {
                }
            }
        }
        if (status == ConnectionStatus.DISCONNECTED) {
            // Host waits for reconnect; client attempts reconnect.
            if (isHost) {
                opponentDisconnectedAt = Instant.now();
            } else if (reconnectHost != null && !reconnectHost.isBlank() && reconnectPort > 0) {
                attemptReconnectAsync();
            }
        }
    }

    @Override
    public void onMessage(NetworkMessage message) {
        if (message == null || message.getType() == null) {
            return;
        }
        if (message.getType() == NetworkMessageType.DISCONNECT) {
            opponentDisconnectedAt = Instant.now();
            if (onConnectionInfo != null) {
                onConnectionInfo.accept("Opponent disconnected - attempting reconnect…");
            }
            if (!isHost && reconnectHost != null && !reconnectHost.isBlank() && reconnectPort > 0) {
                attemptReconnectAsync();
            }
            return;
        }
        if (isHost) {
            // If a client re-joins mid-match, it will start by sending lobby messages (HELLO/DECK/READY/PING).
            // Respond by pushing the client into match view immediately.
            if (message.getType() == NetworkMessageType.HELLO
                || message.getType() == NetworkMessageType.DECK
                || message.getType() == NetworkMessageType.READY
                || message.getType() == NetworkMessageType.PING) {
                if (message.getType() == NetworkMessageType.PING) {
                    adapter.send(new NetworkMessage(NetworkMessageType.PONG, localPlayerId, message.getData(), nowStamp()));
                }
                if (hostMatch != null && layout != null) {
                    try {
                        adapter.send(new NetworkMessage(NetworkMessageType.START, 1, gson.toJson(layout), nowStamp()));
                        NetworkSnapshot snap = buildSnapshot(hostMatch);
                        adapter.send(new NetworkMessage(NetworkMessageType.STATE_SNAPSHOT, 0, gson.toJson(snap), nowStamp()));
                    } catch (Exception ignored) {
                    }
                }
                return;
            }
            if (message.getType() == NetworkMessageType.REQUEST_SNAPSHOT) {
                if (hostMatch != null && layout != null) {
                    try {
                        adapter.send(new NetworkMessage(NetworkMessageType.START, 1, gson.toJson(layout), nowStamp()));
                        NetworkSnapshot snap = buildSnapshot(hostMatch);
                        adapter.send(new NetworkMessage(NetworkMessageType.STATE_SNAPSHOT, 0, gson.toJson(snap), nowStamp()));
                    } catch (Exception ignored) {
                    }
                }
                return;
            }
            if (message.getType() == NetworkMessageType.INPUT_CARD_PLACED) {
                handleRemoteDeploy(message);
                return;
            }
            return;
        }
        if (message.getType() == NetworkMessageType.STATE_SNAPSHOT) {
            handleSnapshot(message.getData());
        }
    }

    @Override
    public void onError(String message, Exception error) {
        if (onConnectionInfo != null) {
            onConnectionInfo.accept("DISCONNECTED - " + (message != null ? message : "Network error"));
        }
        opponentDisconnectedAt = Instant.now();
        if (!isHost && reconnectHost != null && !reconnectHost.isBlank() && reconnectPort > 0) {
            attemptReconnectAsync();
        }
    }

    private void attemptReconnectAsync() {
        // Try reconnect for up to 5 seconds, limited by config reconnect attempts.
        new Thread(() -> {
            int attempts = Math.max(1, config.getReconnectAttempts());
            long deadline = Instant.now().toEpochMilli() + 5000;
            for (int i = 0; i < attempts && Instant.now().toEpochMilli() < deadline; i++) {
                try {
                    if (onConnectionInfo != null) {
                        onConnectionInfo.accept("Reconnecting… (" + (i + 1) + "/" + attempts + ")");
                    }
                    adapter.connect(reconnectHost, reconnectPort, this);
                    return; // connect() will drive status callbacks
                } catch (Exception ignored) {
                }
                try {
                    Thread.sleep(800);
                } catch (InterruptedException ignored) {
                }
            }
            if (onConnectionInfo != null) {
                onConnectionInfo.accept("Reconnect failed - match ended");
            }
        }, "kuroyale-reconnect").start();
    }

    private void handleRemoteDeploy(NetworkMessage message) {
        if (hostMatch == null || hostMatch.isOver()) {
            return;
        }
        // data: cardId,x,y
        String[] parts = message.getData() != null ? message.getData().split(",") : new String[0];
        if (parts.length < 3) {
            return;
        }
        String cardId = parts[0].trim();
        Integer x = tryInt(parts[1]);
        Integer y = tryInt(parts[2]);
        if (cardId.isEmpty() || x == null || y == null) {
            return;
        }
        Card card = cardById.get(cardId);
        if (card == null) {
            return;
        }
        if (!isAllowedForPlayer(message.getPlayerId(), cardId)) {
            return;
        }
        Player acting = (message.getPlayerId() == 1) ? hostMatch.getPlayer() : hostMatch.getOpponent();
        if (acting == null) {
            return;
        }
        hostMatch.deployCard(acting, card, new Position(x, y));
    }

    private boolean isAllowedForPlayer(int playerId, String cardId) {
        if (cardId == null || cardId.isBlank()) {
            return false;
        }
        String trimmed = cardId.trim();
        if (playerId == 1) {
            return allowedDeckIdsP1.isEmpty() || allowedDeckIdsP1.contains(trimmed);
        }
        if (playerId == 2) {
            return allowedDeckIdsP2.isEmpty() || allowedDeckIdsP2.contains(trimmed);
        }
        return false;
    }

    private void handleSnapshot(String json) {
        try {
            NetworkSnapshot snap = gson.fromJson(json, NetworkSnapshot.class);
            deliverSnapshot(snap);
        } catch (Exception ignored) {
        }
    }

    private void deliverSnapshot(NetworkSnapshot snap) {
        if (snap == null) {
            return;
        }
        this.lastSnapshot = snap;
        if (onSnapshot != null) {
            onSnapshot.accept(snap);
        }
    }

    private NetworkSnapshot buildSnapshot(Match match) {
        NetworkSnapshot snap = new NetworkSnapshot();
        snap.elapsedSeconds = match.getElapsedSeconds();
        snap.remainingSeconds = match.getRemainingSeconds();
        ElixirPhase phase = match.getCurrentElixirPhase();
        if (phase == ElixirPhase.TRIPLE) {
            snap.phase = "TRIPLE";
        } else if (phase == ElixirPhase.DOUBLE) {
            snap.phase = "DOUBLE";
        } else {
            snap.phase = "NORMAL";
        }

        Player p1 = match.getPlayer();
        Player p2 = match.getOpponent();
        snap.player1Elixir = p1 != null ? p1.getCurrentElixir() : 0;
        snap.player2Elixir = p2 != null ? p2.getCurrentElixir() : 0;

        Arena arena = match.getArena();
        if (arena != null) {
            for (Tower t : arena.getTowers()) {
                if (t == null || t.getPosition() == null || t.getType() == null || t.getOwner() == null) {
                    continue;
                }
                NetworkSnapshot.TowerHp th = new NetworkSnapshot.TowerHp();
                th.owner = t.getOwner() == TowerOwner.PLAYER ? 1 : 2;
                th.type = t.getType().name();
                th.x = t.getPosition().getX();
                th.y = t.getPosition().getY();
                th.hp = t.getHp();
                snap.towers.add(th);
            }
            for (Unit u : arena.getUnits()) {
                if (u == null || u.getCard() == null) {
                    continue;
                }
                NetworkSnapshot.UnitState us = new NetworkSnapshot.UnitState();
                us.cardId = u.getCard().getId();
                us.cardName = u.getCard().getName();
                us.owner = u.getOwner() == TowerOwner.PLAYER ? 1 : 2;
                us.x = u.getPreciseX();
                us.y = u.getPreciseY();
                us.hp = u.getCurrentHP();
                snap.units.add(us);
            }
        }

        snap.finished = forcedFinished || match.isOver();
        MatchOutcome outcome = match.getOutcome();
        if (forcedFinished && forcedWinnerPlayerId != null) {
            snap.winnerPlayerId = forcedWinnerPlayerId;
        } else if (outcome != null && outcome.getWinner() != null) {
            snap.winnerPlayerId = outcome.getWinner() == TowerOwner.PLAYER ? 1 : 2;
        }
        if (outcome != null) {
            snap.player1Crowns = outcome.getPlayerCrowns();
            snap.player2Crowns = outcome.getOpponentCrowns();
            snap.endReason = outcome.getReason();
        }
        return snap;
    }

    private Integer tryInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String nowStamp() {
        return String.valueOf(Instant.now().toEpochMilli());
    }
}


