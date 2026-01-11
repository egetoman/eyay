package kuroyale;

import application.network.NetworkMatchController;
import application.network.NetworkSnapshot;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Window;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.Card;
import kuroyale.domain.Deck;
import kuroyale.domain.Player;
import kuroyale.domain.Position;
import kuroyale.domain.Tower;
import kuroyale.domain.TowerOwner;
import kuroyale.domain.TowerType;
import kuroyale.domain.Unit;
import kuroyale.infrastructure.CardCatalogRepository;

/**
 * Phase 2 Feature 2: Network Multiplayer - Match screen.
 * <p>
 * Host runs simulation and broadcasts snapshots; client renders snapshots and
 * sends deploy requests.
 */
public class NetworkMatchView {

    private final BorderPane root;
    private final ScreenNavigator navigator;
    private final NetworkMatchController controller;
    private final ArenaLayout layout;
    private final ArenaBoard arenaBoard;
    private final Label statusLabel = new Label();
    private final Label connectionLabel = new Label();
    private final Label timeLabel = new Label("—");
    private final Label phaseLabel = new Label("—");
    private final Label p1ElixirLabel = new Label("P1: —");
    private final Label p2ElixirLabel = new Label("P2: —");

    private final List<Card> localDeckCards;
    private final List<Card> handCards = new ArrayList<>();
    private Card nextCard;
    private int selectedIndex = -1;
    private final VBox deckBox = new VBox(6);
    private Timeline hostTicker;
    private Timeline bootstrapTicker;
    private final StackPane overlayLayer = new StackPane();

    public NetworkMatchView(ScreenNavigator navigator, NetworkMatchController controller, ArenaLayout layout) {
        this.navigator = navigator;
        this.controller = controller;
        this.layout = layout;

        root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #0f1216;");

        // Ensure we always close sockets when leaving this screen (navigate back to
        // main menu, window closed, etc.)
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                // Screen removed from stage
                if (this.controller != null) {
                    this.controller.close();
                }
            }
            if (newScene != null) {
                newScene.windowProperty().addListener((o, oldWin, newWin) -> {
                    if (newWin != null) {
                        newWin.addEventHandler(javafx.stage.WindowEvent.WINDOW_HIDDEN, ev -> {
                            if (this.controller != null) {
                                this.controller.close();
                            }
                        });
                    }
                });
            }
        });

        Label title = new Label("Network Match");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#f5f5f5"));

        connectionLabel.setTextFill(Color.web("#cbd0d6"));
        connectionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        HBox meta = new HBox(12,
                metric("Conn", connectionLabel),
                metric("Phase", phaseLabel),
                metric("Time", timeLabel),
                metric("Elixir", new Label()) // placeholder; updated below
        );
        meta.setAlignment(Pos.CENTER_RIGHT);

        VBox header = new VBox(6, title, meta);
        header.setPadding(new Insets(10, 10, 15, 10));
        root.setTop(header);

        boolean flipVertical = controller != null && controller.getLocalPlayerId() == 2;
        arenaBoard = new ArenaBoard(layout, flipVertical);
        arenaBoard.setOnTileSelected(this::handleTileClick);
        overlayLayer.setVisible(false);
        overlayLayer.setMouseTransparent(true);
        root.setCenter(new StackPane(arenaBoard.getView(), overlayLayer));

        // Local deck (client/host sees own deck only)
        List<Card> provided = controller != null ? controller.getLocalDeckCards() : null;
        localDeckCards = (provided != null && !provided.isEmpty())
                ? provided
                : new CardCatalogRepository().findAll();
        initializeHand();
        refreshDeckUI();

        statusLabel.setTextFill(Color.web("#d2d7e5"));
        statusLabel.setWrapText(true);
        statusLabel.setText("Select a card and click to deploy. Host is authoritative.");

        Button back = new Button("Exit to Menu");
        back.setOnAction(e -> {
            stopHostTicker();
            stopBootstrapTicker();
            if (controller != null) {
                controller.close();
            }
            navigator.showWelcomeScreen();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox elixirRow = new HBox(12, p1ElixirLabel, p2ElixirLabel, spacer, back);
        elixirRow.setAlignment(Pos.CENTER_LEFT);
        p1ElixirLabel.setTextFill(Color.web("#cbd0d6"));
        p2ElixirLabel.setTextFill(Color.web("#cbd0d6"));

        VBox bottom = new VBox(10, deckBox, elixirRow, statusLabel);
        bottom.setPadding(new Insets(12));
        bottom.setStyle("-fx-background-color: #181b22; -fx-border-color: #2d2f36; -fx-border-width: 2 0 0 0;");
        root.setBottom(bottom);

        // Wire controller callbacks
        controller.setOnConnectionInfo(text -> Platform.runLater(() -> connectionLabel.setText(text)));
        controller.setOnSnapshot(snap -> Platform.runLater(() -> applySnapshot(snap)));

        // Reconnect scenario: we may already have a snapshot cached before callbacks
        // were attached.
        NetworkSnapshot cached = controller.getLastSnapshot();
        if (cached != null) {
            applySnapshot(cached);
        }
        // Proactively request snapshots for a short period (fixes cases where the first
        // request happens before socket is ready).
        startBootstrapSnapshotRequests();

        // Host ticks simulation
        if (controller.isHost()) {
            hostTicker = new Timeline(new KeyFrame(Duration.seconds(0.2), e -> controller.hostTick(0.2)));
            hostTicker.setCycleCount(Timeline.INDEFINITE);
            hostTicker.play();
        }
    }

    private void startBootstrapSnapshotRequests() {
        if (controller == null) {
            return;
        }
        // Try up to ~5 seconds (10 * 0.5s) until we have phase/time populated.
        bootstrapTicker = new Timeline(new KeyFrame(Duration.seconds(0.5), e -> {
            boolean stillEmpty = "—".equals(phaseLabel.getText()) || "—".equals(timeLabel.getText());
            if (stillEmpty) {
                controller.requestSnapshot();
            } else {
                stopBootstrapTicker();
            }
        }));
        bootstrapTicker.setCycleCount(10);
        bootstrapTicker.setOnFinished(e -> stopBootstrapTicker());
        bootstrapTicker.play();
        controller.requestSnapshot();
    }

    private VBox metric(String caption, Label value) {
        Label cap = new Label(caption.toUpperCase());
        cap.setTextFill(Color.web("#8f94a3"));
        cap.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        value.setTextFill(Color.WHITE);
        value.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        VBox v = new VBox(2, cap, value);
        v.setPadding(new Insets(8, 12, 8, 12));
        v.setStyle("-fx-background-color: #1c1f2a; -fx-background-radius: 8;");
        return v;
    }

    private void initializeHand() {
        handCards.clear();
        // Simplified: just take first 8 from catalog
        List<Card> picked = new ArrayList<>();
        for (Card c : localDeckCards) {
            if (picked.size() >= 8)
                break;
            picked.add(c);
        }
        for (int i = 0; i < 4; i++) {
            handCards.add(i < picked.size() ? picked.get(i) : null);
        }
        nextCard = picked.size() > 4 ? picked.get(4) : null;
    }

    private void refreshDeckUI() {
        deckBox.getChildren().clear();
        HBox row = new HBox(8);
        for (int i = 0; i < 4; i++) {
            Card c = i < handCards.size() ? handCards.get(i) : null;
            StackPane slot = StartGameUiBits.createCardSlot(c, true, i == selectedIndex);
            final int idx = i;
            slot.setOnMouseClicked(e -> {
                selectedIndex = idx;
                refreshDeckUI();
            });
            row.getChildren().add(slot);
        }
        StackPane next = StartGameUiBits.createCardSlot(nextCard, false, false);
        VBox nextCol = new VBox(4, new Label("Next"), next);
        nextCol.setAlignment(Pos.CENTER);
        row.getChildren().add(0, nextCol);
        deckBox.getChildren().add(row);
    }

    private void handleTileClick(Position pos) {
        if (selectedIndex < 0 || selectedIndex >= handCards.size()) {
            statusLabel.setTextFill(Color.web("#f05a5b"));
            statusLabel.setText("Select a card first.");
            return;
        }
        Card card = handCards.get(selectedIndex);
        if (card == null) {
            statusLabel.setTextFill(Color.web("#f05a5b"));
            statusLabel.setText("Selected slot is empty.");
            return;
        }
        if (!isValidSide(pos)) {
            statusLabel.setTextFill(Color.web("#f05a5b"));
            statusLabel.setText("Invalid side for your player.");
            return;
        }
        var result = controller.requestDeploy(card, pos);
        if (result != null && !result.isSuccess()) {
            statusLabel.setTextFill(Color.web("#f05a5b"));
            statusLabel.setText(result.getMessage() != null ? result.getMessage() : "Deploy failed.");
            return;
        }
        statusLabel.setTextFill(Color.web("#9be564"));
        statusLabel.setText("Requested deploy: " + card.getName() + " at (" + pos.getX() + "," + pos.getY() + ")");
        selectedIndex = -1;
        refreshDeckUI();
    }

    private boolean isValidSide(Position tile) {
        if (tile == null)
            return false;
        int height = layout.getHeight();
        int riverTop = height / 2 - 1;
        int riverBottom = riverTop + 1;
        if (tile.getY() == riverTop || tile.getY() == riverBottom) {
            return false;
        }
        boolean isPlayer1 = controller.getLocalPlayerId() == 1;
        if (isPlayer1) {
            return tile.getY() <= riverTop - 1;
        } else {
            return tile.getY() >= riverBottom + 1;
        }
    }

    private void applySnapshot(NetworkSnapshot snap) {
        if (snap == null)
            return;
        phaseLabel.setText(snap.phase != null ? snap.phase : "—");
        timeLabel.setText(formatTime(snap.remainingSeconds));
        p1ElixirLabel.setText("P1 Elixir: " + snap.player1Elixir);
        p2ElixirLabel.setText("P2 Elixir: " + snap.player2Elixir);

        // Update tower HP on the layout's tower objects (mutable objects, list is
        // unmodifiable)
        for (NetworkSnapshot.TowerHp th : snap.towers) {
            for (Tower t : layout.getTowers()) {
                if (t == null || t.getPosition() == null || t.getType() == null || t.getOwner() == null)
                    continue;
                if (t.getPosition().getX() == th.x && t.getPosition().getY() == th.y
                        && t.getType().name().equals(th.type)
                        && ((t.getOwner() == TowerOwner.PLAYER ? 1 : 2) == th.owner)) {
                    t.setHp(th.hp);
                }
            }
        }

        // Render units
        List<Unit> units = new ArrayList<>();
        for (NetworkSnapshot.UnitState us : snap.units) {
            Card card = new CardCatalogRepository().findAll().stream()
                    .filter(c -> c != null && c.getId().equals(us.cardId))
                    .findFirst().orElse(null);
            if (card == null)
                continue;
            TowerOwner owner = us.owner == 1 ? TowerOwner.PLAYER : TowerOwner.OPPONENT;
            Unit u = new Unit(card, new Position((int) Math.round(us.x), (int) Math.round(us.y)), us.hp, owner);
            u.setPrecisePosition(us.x, us.y);
            units.add(u);
        }
        arenaBoard.renderUnits(units);

        if (snap.finished) {
            showFinishedOverlay(snap);
        }
    }

    private void showFinishedOverlay(NetworkSnapshot snap) {
        overlayLayer.setVisible(true);
        overlayLayer.setMouseTransparent(false);
        overlayLayer.getChildren().clear();
        javafx.scene.shape.Rectangle dim = new javafx.scene.shape.Rectangle();
        dim.widthProperty().bind(overlayLayer.widthProperty());
        dim.heightProperty().bind(overlayLayer.heightProperty());
        dim.setFill(Color.color(0, 0, 0, 0.65));

        Label over = new Label("Match Ended");
        over.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        over.setTextFill(Color.WHITE);

        String winner = "Draw";
        if (snap.winnerPlayerId != null) {
            winner = snap.winnerPlayerId == controller.getLocalPlayerId() ? "You" : "Opponent";
        }
        Label winnerLabel = new Label("Winner: " + winner);
        winnerLabel.setTextFill(Color.web("#ffd54f"));
        winnerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Button exit = new Button("Return to Menu");
        exit.setOnAction(e -> {
            stopHostTicker();
            stopBootstrapTicker();
            if (controller != null) {
                controller.close();
            }
            navigator.showWelcomeScreen();
        });

        VBox box = new VBox(14, over, winnerLabel, exit);
        box.setAlignment(Pos.CENTER);
        overlayLayer.getChildren().addAll(dim, box);
        StackPane.setAlignment(box, Pos.CENTER);
    }

    private String formatTime(double remainingSeconds) {
        int total = (int) Math.max(0, Math.ceil(remainingSeconds));
        int min = total / 60;
        int sec = total % 60;
        return String.format("%02d:%02d", min, sec);
    }

    private void stopHostTicker() {
        if (hostTicker != null) {
            hostTicker.stop();
            hostTicker = null;
        }
    }

    private void stopBootstrapTicker() {
        if (bootstrapTicker != null) {
            bootstrapTicker.stop();
            bootstrapTicker = null;
        }
    }

    public Parent getRoot() {
        return root;
    }
}
