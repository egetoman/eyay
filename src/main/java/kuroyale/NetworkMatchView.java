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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
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
import kuroyale.domain.Position;
import kuroyale.domain.Tower;
import kuroyale.domain.TowerOwner;
import kuroyale.domain.TowerType;
import kuroyale.domain.Unit;
import kuroyale.infrastructure.CardCatalogRepository;
import kuroyale.emote.EmoteBubbleManager;
import kuroyale.emote.EmoteEvent;
import kuroyale.emote.EmoteLimiter;
import kuroyale.emote.EmotePanel;
import kuroyale.emote.EmoteSettings;
import kuroyale.emote.EmoteSound;
import kuroyale.emote.EmoteType;

/**
 * Phase 2 Feature 2: Network Multiplayer - Match screen.
 * <p>
 * Host runs simulation and broadcasts snapshots; client renders snapshots and
 * sends deploy requests.
 */
public class NetworkMatchView {

    private static final double ELIXIR_BAR_WIDTH = 600;
    private static final double ELIXIR_BAR_HEIGHT = 20;

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
    private Label localElixirValue;
    private Region localElixirFill;

    private final List<Card> localDeckCards;
    private final java.util.Deque<Card> drawPile = new java.util.ArrayDeque<>();
    private final List<Card> handCards = new ArrayList<>();
    private Card nextCard;
    private int selectedIndex = -1;
    private final VBox deckBox = new VBox(6);
    private Timeline hostTicker;
    private Timeline bootstrapTicker;
    private final StackPane overlayLayer = new StackPane();
    private final Pane emoteLayer = new Pane();
    private boolean finishedOverlayShown = false;
    private final EmotePanel emotePanel;
    private final StackPane emotePanelLayer = new StackPane();
    private final EmoteLimiter emoteLimiter = EmoteLimiter.defaultLimiter();
    private final EmoteBubbleManager emoteBubbles;

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
        emoteLayer.setMouseTransparent(true);
        emoteLayer.prefWidthProperty().bind(root.widthProperty());
        emoteLayer.prefHeightProperty().bind(root.heightProperty());
        root.setCenter(new StackPane(arenaBoard.getView(), emoteLayer, overlayLayer));

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

        emotePanel = new EmotePanel(this::handleEmoteSelected);
        emotePanelLayer.getChildren().add(emotePanel.getView());
        StackPane.setAlignment(emotePanel.getView(), Pos.BOTTOM_RIGHT);
        StackPane.setMargin(emotePanel.getView(), new Insets(0, 24, 120, 0));
        root.getChildren().add(emotePanelLayer);

        emoteBubbles = new EmoteBubbleManager(emoteLayer);

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
        Button emoteButton = new Button("Emotes");
        emoteButton.setOnAction(e -> emotePanel.toggle());

        p1ElixirLabel.setTextFill(Color.web("#cbd0d6"));
        p2ElixirLabel.setTextFill(Color.web("#cbd0d6"));

        VBox elixirPanel = buildElixirPanel();
        HBox elixirRow = new HBox(16, elixirPanel, spacer, emoteButton, back);
        elixirRow.setAlignment(Pos.CENTER_LEFT);

        VBox bottom = new VBox(10, deckBox, elixirRow, statusLabel);
        bottom.setPadding(new Insets(12));
        bottom.setStyle("-fx-background-color: #181b22; -fx-border-color: #2d2f36; -fx-border-width: 2 0 0 0;");
        root.setBottom(bottom);

        // Wire controller callbacks
        controller.setOnConnectionInfo(text -> Platform.runLater(() -> connectionLabel.setText(text)));
        controller.setOnSnapshot(snap -> Platform.runLater(() -> applySnapshot(snap)));
        controller.setOnEmote(event -> Platform.runLater(() -> handleRemoteEmote(event)));

        // Reconnect scenario: we may already have a snapshot cached before callbacks
        // were attached.
        NetworkSnapshot cached = controller.getLastSnapshot();
        if (cached != null) {
            applySnapshot(cached);
        }
        // Proactively request snapshots for a short period (fixes cases where the first
        // request happens before socket is ready).
        startBootstrapSnapshotRequests();

        // Host ticks simulation at ~30 FPS for smooth gameplay (same as single-player)
        if (controller.isHost()) {
            double tickDuration = 0.033;
            hostTicker = new Timeline(new KeyFrame(Duration.seconds(tickDuration), e -> controller.hostTick(tickDuration)));
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

    private VBox buildElixirPanel() {
        // Clash Royale style: long bar with tick marks (same as LocalPvP)
        Region track = new Region();
        track.setPrefHeight(ELIXIR_BAR_HEIGHT);
        track.setMaxWidth(ELIXIR_BAR_WIDTH);
        track.setMinWidth(ELIXIR_BAR_WIDTH);
        track.setStyle(
                "-fx-background-color: #0d1016; -fx-background-radius: 10; -fx-border-color: #3b3f55; -fx-border-radius: 10;");

        localElixirFill = new Region();
        localElixirFill.setPrefHeight(ELIXIR_BAR_HEIGHT);
        localElixirFill.setStyle("-fx-background-color: linear-gradient(to right, #b259ff, #7a4dff); -fx-background-radius: 10;");

        // Create tick marks for 10 segments
        HBox ticks = new HBox();
        ticks.setAlignment(Pos.CENTER_LEFT);
        double segmentWidth = ELIXIR_BAR_WIDTH / 10.0;
        for (int i = 0; i < 9; i++) {
            Region spacer = new Region();
            spacer.setPrefWidth(segmentWidth - 1);
            spacer.setMinWidth(segmentWidth - 1);
            Region divider = new Region();
            divider.setPrefSize(1, ELIXIR_BAR_HEIGHT);
            divider.setStyle("-fx-background-color: rgba(255,255,255,0.15);");
            ticks.getChildren().addAll(spacer, divider);
        }
        ticks.setMouseTransparent(true);
        ticks.setMaxWidth(ELIXIR_BAR_WIDTH);

        StackPane bar = new StackPane(track, localElixirFill, ticks);
        bar.setMaxWidth(ELIXIR_BAR_WIDTH);
        StackPane.setAlignment(localElixirFill, Pos.CENTER_LEFT);
        StackPane.setAlignment(ticks, Pos.CENTER_LEFT);

        localElixirValue = new Label("0");
        localElixirValue.setTextFill(Color.web("#d18bff"));
        localElixirValue.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        localElixirValue.setEffect(new javafx.scene.effect.DropShadow(5, Color.BLACK));

        HBox barWithLabel = new HBox(12, bar, localElixirValue);
        barWithLabel.setAlignment(Pos.CENTER_LEFT);

        // P1/P2 elixir labels below
        p1ElixirLabel.setTextFill(Color.web("#8f94a3"));
        p2ElixirLabel.setTextFill(Color.web("#8f94a3"));
        HBox playerLabels = new HBox(20, p1ElixirLabel, p2ElixirLabel);
        playerLabels.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(8, barWithLabel, playerLabels);
        panel.setAlignment(Pos.CENTER_LEFT);
        return panel;
    }

    private void initializeHand() {
        drawPile.clear();
        handCards.clear();
        // Add all deck cards to the draw pile
        if (localDeckCards != null) {
            drawPile.addAll(localDeckCards);
        }
        // Draw initial hand of 4 cards
        for (int i = 0; i < 4; i++) {
            handCards.add(drawPile.pollFirst());
        }
        // Next card is the top of the draw pile
        nextCard = drawPile.peekFirst();
    }

    private void refreshDeckUI() {
        deckBox.getChildren().clear();
        HBox row = new HBox(6); // Reduced spacing
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
        // Spells can target anywhere, troops/buildings must be on your side
        boolean isSpell = card.getType() == kuroyale.domain.CardType.SPELL;
        if (!isSpell && !isValidSide(pos)) {
            statusLabel.setTextFill(Color.web("#f05a5b"));
            statusLabel.setText("You can only deploy troops/buildings on your side.");
            return;
        }
        var result = controller.requestDeploy(card, pos);
        if (result != null && !result.isSuccess()) {
            statusLabel.setTextFill(Color.web("#f05a5b"));
            statusLabel.setText(result.getMessage() != null ? result.getMessage() : "Deploy failed.");
            return;
        }
        if (card.getType() != null && card.getType() != kuroyale.domain.CardType.SPELL) {
            kuroyale.domain.TowerOwner owner = controller.getLocalPlayerId() == 1
                    ? kuroyale.domain.TowerOwner.PLAYER
                    : kuroyale.domain.TowerOwner.OPPONENT;
            arenaBoard.recordDeployEffect(owner, pos);
        }
        statusLabel.setTextFill(Color.web("#9be564"));
        statusLabel.setText("Requested deploy: " + card.getName() + " at (" + pos.getX() + "," + pos.getY() + ")");
        cycleCard(selectedIndex);
        selectedIndex = -1;
        refreshDeckUI();
    }

    private void cycleCard(int index) {
        if (index < 0 || index >= handCards.size()) {
            return;
        }
        Card played = handCards.get(index);
        if (played == null) {
            return;
        }
        // Add played card back to the draw pile
        drawPile.addLast(played);
        // Draw replacement card
        Card replacement = drawPile.pollFirst();
        handCards.set(index, replacement);
        // Update next card preview
        nextCard = drawPile.peekFirst();
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

        double localElixir = controller != null && controller.getLocalPlayerId() == 2
                ? snap.player2Elixir
                : snap.player1Elixir;
        updateLocalElixirHud(localElixir);
        updateDeckLoadingState(localElixir);

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

        if (snap.finished && !finishedOverlayShown) {
            finishedOverlayShown = true;
            showFinishedOverlay(snap);
        }
    }

    private void updateLocalElixirHud(double currentElixir) {
        if (localElixirValue == null || localElixirFill == null) {
            return;
        }
        localElixirValue.setText(String.valueOf((int) currentElixir));
        double ratio = clamp01(currentElixir / 10.0);
        double width = ELIXIR_BAR_WIDTH * ratio;
        localElixirFill.setPrefWidth(width);
        localElixirFill.setMinWidth(width);
        localElixirFill.setMaxWidth(width);
    }

    private void updateDeckLoadingState(double currentElixir) {
        if (deckBox.getChildren().isEmpty()) {
            return;
        }
        var rowNode = deckBox.getChildren().get(0);
        if (!(rowNode instanceof HBox)) {
            return;
        }
        HBox deckRow = (HBox) rowNode;
        
        // Structure: [VBox nextCol, StackPane slot0, StackPane slot1, StackPane slot2, StackPane slot3]
        // Skip first child (next column), iterate over hand card slots
        int handIndex = 0;
        for (int i = 1; i < deckRow.getChildren().size() && handIndex < handCards.size(); i++) {
            var child = deckRow.getChildren().get(i);
            if (child instanceof StackPane) {
                Card card = handCards.get(handIndex);
                if (card != null) {
                    double cost = card.getElixirCost();
                    double progress = (cost <= 0) ? 1.0 : (currentElixir / cost);
                    StartGameUiBits.updateCardLoading((StackPane) child, progress);
                } else {
                    StartGameUiBits.updateCardLoading((StackPane) child, 0.0);
                }
                handIndex++;
            }
        }
    }

    private void handleEmoteSelected(EmoteType type) {
        if (type == null || controller == null) {
            return;
        }
        long now = System.currentTimeMillis();
        var result = emoteLimiter.tryConsume(now);
        if (!result.isAllowed()) {
            statusLabel.setTextFill(Color.web("#f05a5b"));
            statusLabel.setText("Emote blocked: " + result.getReason());
            return;
        }
        emoteBubbles.showForBottom(type);
        EmoteSound.play();
        controller.sendEmote(type);
    }

    private void handleRemoteEmote(EmoteEvent event) {
        if (event == null || event.getEmoteType() == null || controller == null) {
            return;
        }
        boolean isLocal = event.getPlayerId() == controller.getLocalPlayerId();
        if (!isLocal && EmoteSettings.isMuteOpponentEmotes()) {
            return;
        }
        if (isLocal) {
            emoteBubbles.showForBottom(event.getEmoteType());
        } else {
            emoteBubbles.showForTop(event.getEmoteType());
        }
        EmoteSound.play();
    }

    private void showFinishedOverlay(NetworkSnapshot snap) {
        int localId = controller != null ? controller.getLocalPlayerId() : 1;

        Integer p1Crowns = snap.player1Crowns;
        Integer p2Crowns = snap.player2Crowns;
        if (p1Crowns == null || p2Crowns == null) {
            // Legacy fallback: derive from layout tower HP (counts destroyed crown towers)
            int p1 = 0;
            int p2 = 0;
            for (Tower t : layout.getTowers()) {
                if (t == null || t.getType() == null || t.getOwner() == null) continue;
                if (t.getType() != TowerType.CROWN) continue;
                if (t.getHp() > 0) continue;
                if (t.getOwner() == TowerOwner.OPPONENT) {
                    p1++; // player1 destroyed opponent crown
                } else if (t.getOwner() == TowerOwner.PLAYER) {
                    p2++; // player2 destroyed opponent crown
                }
            }
            p1Crowns = p1;
            p2Crowns = p2;
        }

        int localCrowns = localId == 1 ? p1Crowns : p2Crowns;
        int opponentCrowns = localId == 1 ? p2Crowns : p1Crowns;

        String headline = "Draw";
        boolean localWins = false;
        if (snap.winnerPlayerId != null) {
            localWins = snap.winnerPlayerId == localId;
            headline = localWins ? "Victory" : "Defeat";
        }

        // Update quest and achievement progress for Network match
        if (navigator != null) {
            var questService = navigator.getQuestService();
            var achievementService = navigator.getAchievementService();
            
            if (questService != null) {
                // Track win/loss for streak
                questService.recordMatchResult(localWins);
                
                if (localWins) {
                    questService.updateProgress(kuroyale.domain.QuestType.WIN_MATCHES, 1);
                    questService.updateProgress(kuroyale.domain.QuestType.WIN_PVP_MATCH, 1);
                    questService.updateProgress(kuroyale.domain.QuestType.WIN_NETWORK_MATCH, 1);
                    
                    // Win without losing crown tower
                    if (opponentCrowns == 0) {
                        questService.updateProgress(kuroyale.domain.QuestType.WIN_WITHOUT_LOSING_CROWN, 1);
                    }
                    
                    // Check for win using only common rarity cards
                    boolean allCommon = true;
                    if (localDeckCards != null && !localDeckCards.isEmpty()) {
                        for (Card card : localDeckCards) {
                            if (card != null) {
                                kuroyale.domain.Rarity rarity = kuroyale.domain.CardRarityCatalog.rarityForCardId(card.getId());
                                if (rarity != kuroyale.domain.Rarity.COMMON) {
                                    allCommon = false;
                                    break;
                                }
                            }
                        }
                        if (allCommon) {
                            questService.updateProgress(kuroyale.domain.QuestType.WIN_ONLY_COMMON, 1);
                        }
                    }
                }
                
                if (localCrowns > 0) {
                    questService.updateProgress(kuroyale.domain.QuestType.DESTROY_CROWN_TOWERS, localCrowns);
                }
                
                if (localCrowns >= 3) {
                    questService.updateProgress(kuroyale.domain.QuestType.DESTROY_ENEMY_KING, 1);
                }
                
                // Track card plays from match statistics (host only - has access to Match object)
                if (controller != null && controller.isHost()) {
                    var hostMatch = controller.getHostMatch();
                    if (hostMatch != null) {
                        int spellsPlayed = hostMatch.getPlayerSpellsPlayed();
                        int troopsDeployed = hostMatch.getPlayerTroopsDeployed();
                        int buildingsPlayed = hostMatch.getPlayerBuildingsPlayed();
                        int elixirSpent = hostMatch.getPlayerElixirSpent();
                        int totalCardsPlayed = hostMatch.getPlayerCardsPlayed();
                        int spellDamage = hostMatch.getPlayerSpellDamageDealt();
                        
                        if (spellsPlayed > 0) {
                            questService.updateProgress(kuroyale.domain.QuestType.PLAY_SPELL_CARDS, spellsPlayed);
                        }
                        if (troopsDeployed > 0) {
                            questService.updateProgress(kuroyale.domain.QuestType.DEPLOY_TROOP_CARDS, troopsDeployed);
                        }
                        if (buildingsPlayed > 0) {
                            questService.updateProgress(kuroyale.domain.QuestType.PLAY_BUILDING_CARDS, buildingsPlayed);
                        }
                        if (elixirSpent > 0) {
                            questService.updateProgress(kuroyale.domain.QuestType.SPEND_ELIXIR, elixirSpent);
                        }
                        if (totalCardsPlayed >= 20) {
                            questService.updateProgress(kuroyale.domain.QuestType.PLAY_20_CARDS_SINGLE_MATCH, 1);
                        }
                        if (spellDamage > 0) {
                            questService.updateProgress(kuroyale.domain.QuestType.DEAL_SPELL_DAMAGE, spellDamage);
                        }
                    }
                }
            }
            
            if (achievementService != null) {
                if (localWins) {
                    achievementService.updateProgress(kuroyale.domain.AchievementType.WIN_TOTAL_MATCHES, 1);
                }
                if (localCrowns > 0) {
                    achievementService.updateProgress(kuroyale.domain.AchievementType.TOTAL_CROWNS, localCrowns);
                }
            }
        }

        MatchEndOverlay.show(
                overlayLayer,
                "Opponent",
                opponentCrowns,
                "You",
                localCrowns,
                headline,
                () -> {
                    stopHostTicker();
                    stopBootstrapTicker();
                    if (controller != null) {
                        controller.close();
                    }
                    if (navigator != null) {
                        navigator.showNetworkMenuScreen();
                    }
                },
                () -> {
                    stopHostTicker();
                    stopBootstrapTicker();
                    if (controller != null) {
                        controller.close();
                    }
                    if (navigator != null) {
                        navigator.showWelcomeScreen();
                    }
                });
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

    private double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
