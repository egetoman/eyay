package kuroyale;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.animation.KeyFrame;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
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
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import application.MatchController;
import kuroyale.domain.Arena;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.Card;
import kuroyale.domain.CardType;
import kuroyale.domain.ElixirPhase;
import kuroyale.domain.Match;
import kuroyale.domain.MatchOutcome;
import kuroyale.domain.Player;
import kuroyale.domain.Position;
import kuroyale.support.Result;
import kuroyale.emote.EmoteBubbleManager;
import kuroyale.emote.EmoteLimiter;
import kuroyale.emote.EmotePanel;
import kuroyale.emote.EmoteSettings;
import kuroyale.emote.EmoteSound;
import kuroyale.emote.EmoteType;

/**
 * Phase 2 Feature 1: Local Player vs Player - Turn-based match screen.
 * <p>
 * Simplest implementation: each successful deploy ends the current player's
 * turn.
 * Both players' hands are shown side-by-side; only the active player can
 * select/deploy.
 */
public class LocalPvPGameView {

    private static final double ELIXIR_BAR_WIDTH = 220;
    private static final double ELIXIR_BAR_HEIGHT = 14;
    private static final double CROWN_ANIM_DURATION_SECONDS = 0.7;
    private static final double CROWN_ICON_SIZE = 26;

    private final StackPane root;
    private final BorderPane content;
    private final Pane effectLayer;
    private final MatchController controller;
    private final Match match;
    private final Player bottomPlayer;
    private final Player topPlayer;
    private Player activePlayer;
    private final ArenaBoard arenaBoard;
    private final Label timerLabel;
    private final Label phaseLabel;
    private final Label turnLabel;
    private final Label topCrownLabel;
    private final Label bottomCrownLabel;
    private final StackPane topCrownTarget;
    private final StackPane bottomCrownTarget;
    private final StackPane topCrownPill;
    private final StackPane bottomCrownPill;
    private final Label statusLabel = new Label();
    private final VBox dashboardContainer = new VBox();
    private Timeline matchTicker;

    private final HandState bottomHand;
    private final HandState topHand;
    private int selectedBottomIndex = -1;
    private int selectedTopIndex = -1;

    private Label draggingCardLabel;
    private Label activeElixirLabel;
    private Region activeElixirFill;
    private StackPane nextCardSlot;
    private HBox handCardContainer;

    private boolean paused = false;
    private final StackPane overlayLayer = new StackPane();
    private final ScreenNavigator navigator;
    private final Set<String> destroyedTowerKeys = new HashSet<>();
    private int bottomCrowns = 0;
    private int topCrowns = 0;
    private final EmoteBubbleManager emoteBubbles;
    private final EmoteLimiter bottomEmoteLimiter = EmoteLimiter.defaultLimiter();
    private final EmoteLimiter topEmoteLimiter = EmoteLimiter.defaultLimiter();
    private final EmotePanel emotePanel;
    private final StackPane emotePanelLayer = new StackPane();
    private boolean emoteSenderBottom = true;

    public LocalPvPGameView(ScreenNavigator navigator, MatchController controller, ArenaLayout selectedLayout) {
        this.navigator = navigator;
        this.controller = controller;
        this.match = controller != null ? controller.getMatch() : null;
        this.bottomPlayer = controller != null ? controller.getPlayer() : null;
        this.topPlayer = controller != null ? controller.getOpponent() : null;
        this.activePlayer = bottomPlayer;
        this.bottomHand = new HandState(resolveDeckCards(bottomPlayer));
        this.topHand = new HandState(resolveDeckCards(topPlayer));

        root = new StackPane();
        content = new BorderPane();
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: #0f1216;");
        effectLayer = new Pane();
        effectLayer.setMouseTransparent(true);
        effectLayer.prefWidthProperty().bind(root.widthProperty());
        effectLayer.prefHeightProperty().bind(root.heightProperty());
        root.getChildren().addAll(content, effectLayer);

        emotePanel = new EmotePanel(this::handleEmoteSelected);
        emotePanelLayer.setPickOnBounds(false);
        emotePanelLayer.getChildren().add(emotePanel.getView());
        StackPane.setAlignment(emotePanel.getView(), Pos.BOTTOM_RIGHT);
        StackPane.setMargin(emotePanel.getView(), new Insets(0, 24, 120, 0));
        root.getChildren().add(emotePanelLayer);

        emoteBubbles = new EmoteBubbleManager(effectLayer);

        Label title = new Label("Local PvP");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#f5f5f5"));

        turnLabel = new Label();
        turnLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        turnLabel.setTextFill(Color.web("#ffd54f"));

        timerLabel = createMetricLabel(formatTime(match != null ? match.getRemainingSeconds() : 0));
        phaseLabel = createMetricLabel(formatPhase(match != null ? match.getCurrentElixirPhase() : ElixirPhase.NORMAL));

        topCrownLabel = createMetricLabel("0");
        topCrownTarget = createCrownTarget();
        topCrownPill = buildCrownPill(topPlayer != null ? topPlayer.getName() : "Player 2",
                topCrownLabel, topCrownTarget, "#b63a4c");

        bottomCrownLabel = createMetricLabel("0");
        bottomCrownTarget = createCrownTarget();
        bottomCrownPill = buildCrownPill(bottomPlayer != null ? bottomPlayer.getName() : "Player 1",
                bottomCrownLabel, bottomCrownTarget, "#2a6fd2");

        HBox metricsRow = new HBox(16,
                buildMetricPill("Turn", turnLabel),
                buildMetricPill("Phase", phaseLabel),
                buildMetricPill("Time", timerLabel));
        metricsRow.setAlignment(Pos.CENTER_RIGHT);

        // Top Left Stats (Enemy Crowns + My Crowns)
        HBox leftStats = new HBox(8, topCrownPill, bottomCrownPill);
        leftStats.setAlignment(Pos.CENTER_LEFT);
        leftStats.setPickOnBounds(false);

        // Top Right Stats (Metrics)
        HBox rightStats = new HBox(8,
                buildMetricPill("Turn", turnLabel),
                buildMetricPill("Phase", phaseLabel),
                buildMetricPill("Time", timerLabel));
        rightStats.setAlignment(Pos.CENTER_RIGHT);
        rightStats.setPickOnBounds(false);

        BorderPane topOverlay = new BorderPane();
        topOverlay.setLeft(leftStats);
        topOverlay.setRight(rightStats);
        topOverlay.setPadding(new Insets(10));
        topOverlay.setPickOnBounds(false);

        arenaBoard = new ArenaBoard(selectedLayout);
        arenaBoard.setOnTileSelected(this::handleTileSelection);
        overlayLayer.setVisible(false);
        overlayLayer.setMouseTransparent(true);
        // Arena is added to root below

        if (match != null && match.getArena() != null) {
            arenaBoard.render(match.getArena());
        }

        VBox hud = buildDashboard();
        // Overlay HUD on top of the board for main arena space
        StackPane.setAlignment(hud, Pos.BOTTOM_CENTER);

        // Re-structure: Root is StackPane. Layer 1: Arena. Layer 2: UI Overlay
        root.getChildren().clear();
        root.getChildren().add(new StackPane(arenaBoard.getView(), overlayLayer));

        BorderPane uiLayer = new BorderPane();
        uiLayer.setPickOnBounds(false); // Let clicks pass through empty parts
        uiLayer.setTop(topOverlay);
        uiLayer.setBottom(hud);
        uiLayer.setRight(emotePanelLayer);
        uiLayer.setRight(emotePanelLayer); // Move emote layer into UI structure

        root.getChildren().add(uiLayer);
        root.getChildren().add(effectLayer);

        refreshTurnHud();
        refreshDashboard();
        updateElixirHud();
        updateClockHud();
        startTicker();
    }

    private VBox buildDashboard() {
        VBox dashboard = new VBox(0);
        dashboard.setPickOnBounds(true); // Catch clicks on the dashboard itself
        dashboard.setMaxHeight(Region.USE_PREF_SIZE);
        dashboard.setStyle("-fx-background-color: #181b22; -fx-border-color: #2d2f36; -fx-border-width: 2 0 0 0;");
        dashboard.setPadding(new Insets(10, 16, 10, 16));

        // Top Row: Next Card (Left) | Hand (Center) | Status + Controls (Right)

        // Next Card Section
        VBox nextSection = new VBox(4);
        nextSection.setAlignment(Pos.CENTER);
        Label nextLabel = new Label("Next");
        nextLabel.setTextFill(Color.web("#8f94a3"));
        nextLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));

        nextCardSlot = new StackPane(); // Placeholder, filled in refreshDashboard
        nextCardSlot.setPrefSize(44, 58);

        nextSection.getChildren().addAll(nextLabel, nextCardSlot);

        // Hand Section (Center)
        handCardContainer = new HBox(12);
        handCardContainer.setAlignment(Pos.CENTER);

        // Controls / Status (Right)
        VBox controlsBox = new VBox(8);
        controlsBox.setAlignment(Pos.CENTER_RIGHT);

        Button emoteButton = new Button("Emotes");
        emoteButton.setOnAction(e -> openEmotePanel(activePlayer == bottomPlayer));

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> togglePause());

        HBox buttonRow = new HBox(8, emoteButton, pauseButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        statusLabel.setTextFill(Color.web("#d2d7e5"));
        statusLabel.setWrapText(false);
        statusLabel.setFont(Font.font("Arial", 11));
        statusLabel.setText("Ready.");

        controlsBox.getChildren().addAll(statusLabel, buttonRow);

        // Assemble Top Row
        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER);

        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        topRow.getChildren().addAll(nextSection, leftSpacer, handCardContainer, rightSpacer, controlsBox);

        // Bottom Row: Elixir Bar
        // We'll make a nice full-width or centered elixir bar
        VBox elixirSection = buildUnifiedElixirBar();

        dashboard.getChildren().addAll(topRow, elixirSection);
        return dashboard;
    }

    private VBox buildUnifiedElixirBar() {
        // Clash Royale style: long bar at bottom
        Region fill = new Region();
        fill.setPrefHeight(ELIXIR_BAR_HEIGHT + 4);
        fill.setStyle("-fx-background-color: linear-gradient(to right, #b259ff, #7a4dff); -fx-background-radius: 10;");

        Region track = new Region();
        track.setPrefHeight(ELIXIR_BAR_HEIGHT + 4);
        track.setStyle(
                "-fx-background-color: #0d1016; -fx-background-radius: 10; -fx-border-color: #3b3f55; -fx-border-radius: 10;");

        HBox ticks = new HBox();
        ticks.setAlignment(Pos.CENTER_LEFT);
        // We want the bar to stretch, so we bind widths later or use a dedicated layout
        // For simplicity, let's fix the width to be wide but reasonable
        double barWidth = 600;
        track.setMaxWidth(barWidth);
        track.setMinWidth(barWidth);

        // Create ticks
        double segmentWidth = barWidth / 10.0;
        for (int i = 0; i < 9; i++) {
            Region spacer = new Region();
            spacer.setPrefWidth(segmentWidth - 1);
            spacer.setMinWidth(segmentWidth - 1);
            Region divider = new Region();
            divider.setPrefSize(1, ELIXIR_BAR_HEIGHT + 4);
            divider.setStyle("-fx-background-color: rgba(255,255,255,0.15);");
            ticks.getChildren().addAll(spacer, divider);
        }
        ticks.setMouseTransparent(true);
        ticks.setMaxWidth(barWidth);

        StackPane barStack = new StackPane(track, fill, ticks);
        barStack.setMaxWidth(barWidth);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        StackPane.setAlignment(ticks, Pos.CENTER_LEFT);

        Label label = new Label("0");
        label.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        label.setTextFill(Color.web("#d18bff"));
        label.setEffect(new javafx.scene.effect.DropShadow(5, Color.BLACK));

        activeElixirLabel = label;
        activeElixirFill = fill;

        // Container with label dropping on top or side?
        // Let's put label inside the bar on the right? Or separate.
        // Standard is number on the bar or next to it.
        StackPane.setAlignment(label, Pos.CENTER);
        barStack.getChildren().add(label);

        VBox container = new VBox(6, barStack);
        container.setPadding(new Insets(8, 0, 4, 0));
        container.setAlignment(Pos.CENTER);
        return container;
    }

    private void refreshDashboard() {
        if (activePlayer == null || nextCardSlot == null || handCardContainer == null) {
            return;
        }

        boolean isBottom = (activePlayer == bottomPlayer);
        HandState hand = isBottom ? bottomHand : topHand;
        int selectedIndex = isBottom ? selectedBottomIndex : selectedTopIndex;

        // Update Next Card
        nextCardSlot.getChildren().clear();
        StackPane nextCard = StartGameUiBits.createCardSlot(hand.nextCard, false, false);
        nextCardSlot.getChildren().add(nextCard);

        // Update Hand Cards
        handCardContainer.getChildren().clear();
        for (int i = 0; i < 4; i++) {
            Card card = i < hand.handCards.size() ? hand.handCards.get(i) : null;
            boolean highlighted = (i == selectedIndex);

            // Use LARGE slots for the active hand
            StackPane slot = StartGameUiBits.createCardSlot(card, true, highlighted);

            // Interaction
            final int index = i;
            slot.setOnMouseClicked(event -> selectHandIndex(activePlayer, isBottom, index, slot));

            handCardContainer.getChildren().add(slot);
        }

        // Update Elixir Bar Label immediately (optional, mainly handled by ticker)
        updateElixirHud();
    }

    private void selectHandIndex(Player clickedPlayer, boolean bottomSide, int index, StackPane slot) {
        if (clickedPlayer == null || activePlayer == null) {
            showStatus("Match is not ready yet.", true);
            return;
        }
        if (clickedPlayer != activePlayer) {
            showStatus("It's " + activePlayer.getName() + "'s turn.", true);
            return;
        }
        HandState hand = bottomSide ? bottomHand : topHand;
        if (index < 0 || index >= hand.handCards.size() || hand.handCards.get(index) == null) {
            if (bottomSide) {
                selectedBottomIndex = -1;
            } else {
                selectedTopIndex = -1;
            }
            refreshDashboard();
            return;
        }
        Card candidate = hand.handCards.get(index);
        if (candidate != null && !canAfford(clickedPlayer, candidate)) {
            showStatus("Not enough elixir.", true);
            shakeNode(slot);
            return;
        }
        if (bottomSide) {
            selectedBottomIndex = index;
            selectedTopIndex = -1;
        } else {
            selectedTopIndex = index;
            selectedBottomIndex = -1;
        }
        refreshDashboard();
        Card selected = hand.handCards.get(index);
        if (selected != null) {
            showStatus(activePlayer.getName() + " selected: " + selected.getName(), false);
        }
    }

    private boolean canAfford(Player player, Card card) {
        if (player == null || card == null) {
            return false;
        }
        double current = resolveCurrentElixir(player);
        return current + 0.001 >= card.getElixirCost();
    }

    private double resolveCurrentElixir(Player player) {
        double base = player.getCurrentElixir();
        if (match == null) {
            return base;
        }
        if (player == bottomPlayer) {
            return base + match.getPlayerElixirFraction();
        }
        return base + match.getOpponentElixirFraction();
    }

    private void shakeNode(StackPane node) {
        if (node == null) {
            return;
        }
        TranslateTransition shake = new TranslateTransition(Duration.millis(40), node);
        shake.setFromX(-3);
        shake.setToX(3);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.playFromStart();
    }

    private void handleTileSelection(Position tile) {
        if (match == null || activePlayer == null) {
            showStatus("Match is not ready yet.", true);
            return;
        }
        int selectedIndex = activePlayer == bottomPlayer ? selectedBottomIndex : selectedTopIndex;
        if (selectedIndex < 0) {
            showStatus("Select a card before deploying.", true);
            return;
        }
        HandState hand = activePlayer == bottomPlayer ? bottomHand : topHand;
        if (selectedIndex >= hand.handCards.size()) {
            showStatus("Select a card before deploying.", true);
            return;
        }
        Card card = hand.handCards.get(selectedIndex);
        if (card == null) {
            showStatus("Selected slot is empty.", true);
            return;
        }

        if (card.getType() != CardType.SPELL) {
            Result<?> sideCheck = validateDeploySide(activePlayer, tile);
            if (!sideCheck.isSuccess()) {
                showStatus(sideCheck.getMessage(), true);
                return;
            }
        }

        Result<?> result = controller != null ? controller.deployCard(activePlayer, card, tile)
                : Result.fail("Match controller missing.");
        if (!result.isSuccess()) {
            showStatus(result.getMessage(), true);
            return;
        }

        // Record spell cast for visual effects
        if (card.getType() == CardType.SPELL) {
            arenaBoard.recordSpellCast(card.getId(), tile);
        } else {
            arenaBoard.recordDeployEffect(resolveOwner(activePlayer), tile);
        }

        hand.cycle(selectedIndex);
        selectedBottomIndex = -1;
        selectedTopIndex = -1;
        refreshDashboard();
        refreshTurnHud();

        showStatus(activePlayer.getName() + " deployed " + card.getName() + " at (" + tile.getX() + ", " + tile.getY()
                + ").", false);

        Arena arena = match.getArena();
        if (arena != null) {
            arenaBoard.render(arena);
        }
        updateElixirHud();

        // End turn after a successful deploy
        activePlayer = (activePlayer == bottomPlayer) ? topPlayer : bottomPlayer;
        refreshTurnHud();
        refreshDashboard();
    }

    /**
     * Validates that a deployment position is valid for the given player's side.
     * 
     * Requires:
     * - acting != null
     * - tile != null
     * - match != null
     * - match.getArena() != null
     * - match.getArena().getHeight() > 0 (arena must have valid dimensions)
     * - acting must be either bottomPlayer or topPlayer (player must be part of
     * this match)
     * 
     * Modifies:
     * - None (pure validation method)
     * 
     * Effects:
     * - Returns Result.ok(null) if:
     * * All parameters are non-null
     * * (Note: spells are handled separately and may be placed anywhere)
     * * Position is not on the river (riverTop or riverBottom rows)
     * * Bottom player (Player 1) deploys on bottom side (Y <= riverTop - 1)
     * * Top player (Player 2) deploys on top side (Y >= riverBottom + 1)
     * - Returns Result.fail("Invalid deployment.") if:
     * * acting == null OR tile == null OR match == null OR match.getArena() == null
     * - Returns Result.fail("Cannot deploy on the river.") if:
     * * tile.getY() == riverTop OR tile.getY() == riverBottom
     * * where riverTop = height / 2 - 1, riverBottom = riverTop + 1
     * - Returns Result.fail("Player 1 can only deploy on the bottom side.") if:
     * * acting == bottomPlayer AND tile.getY() > riverTop - 1
     * - Returns Result.fail("Player 2 can only deploy on the top side.") if:
     * * acting == topPlayer AND tile.getY() < riverBottom + 1
     * 
     * @param acting The player attempting to deploy
     * @param tile   The position where deployment is attempted
     * @return Result indicating success or specific failure reason
     */

    public Result<?> validateDeploySide(Player acting, Position tile) {
        if (acting == null || tile == null || match == null || match.getArena() == null) {
            return Result.fail("Invalid deployment.");
        }
        int height = match.getArena().getHeight();
        int riverTop = height / 2 - 1;
        int riverBottom = riverTop + 1;
        if (tile.getY() == riverTop || tile.getY() == riverBottom) {
            return Result.fail("Cannot deploy on the river.");
        }
        boolean isBottom = acting == bottomPlayer;
        // bottomPlayer = TowerOwner.PLAYER with towers at NORTH (Y=2-7, low Y)
        // topPlayer = TowerOwner.OPPONENT with towers at SOUTH (Y=24-29, high Y)
        // So: bottomPlayer deploys on NORTH (Y < riverBottom), topPlayer deploys on
        // SOUTH (Y > riverTop)
        if (isBottom && tile.getY() >= riverBottom) {
            return Result.fail("Player 1 can only deploy on their side (north).");
        }
        if (!isBottom && tile.getY() <= riverTop) {
            return Result.fail("Player 2 can only deploy on their side (south).");
        }
        return Result.ok(null);
    }

    private void startTicker() {
        if (match == null) {
            return;
        }
        // Run at ~30 FPS for smooth UI updates
        double tickDuration = 0.033;
        matchTicker = new Timeline(new KeyFrame(Duration.seconds(tickDuration), e -> {
            if (!paused) {
                if (controller != null) {
                    controller.advanceTime(tickDuration);
                } else {
                    match.advanceTime(tickDuration);
                }
                updateElixirHud();
                updateClockHud();
                if (match.getArena() != null) {
                    arenaBoard.render(match.getArena());
                    detectTowerDestruction(match.getArena());
                }
            }
            if (match.isFinished()) {
                stopTicker();
                showGameOverOverlay();
            }
        }));
        matchTicker.setCycleCount(Timeline.INDEFINITE);
        matchTicker.play();
    }

    private void stopTicker() {
        if (matchTicker != null) {
            matchTicker.stop();
            matchTicker = null;
        }
    }

    private void updateElixirHud() {
        if (activePlayer == null || activeElixirLabel == null || activeElixirFill == null) {
            return;
        }

        // Only update current player's elixir
        double currentElixir;
        double maxElixir = activePlayer.getMaxElixir();

        if (activePlayer == bottomPlayer) {
            currentElixir = bottomPlayer.getCurrentElixir() + (match != null ? match.getPlayerElixirFraction() : 0);
        } else {
            currentElixir = topPlayer.getCurrentElixir() + (match != null ? match.getOpponentElixirFraction() : 0);
        }

        activeElixirLabel.setText(String.valueOf((int) currentElixir));
        double ratio = (maxElixir == 0) ? 0 : currentElixir / maxElixir;

        // 600 matches buildUnifiedElixirBar width
        double totalWidth = 600;
        double width = totalWidth * clamp01(ratio);
        activeElixirFill.setPrefWidth(width);
        activeElixirFill.setMinWidth(width);
        activeElixirFill.setMaxWidth(width);

        updateDeckLoadingState(activePlayer == bottomPlayer, currentElixir);
    }

    private void updateDeckLoadingState(boolean isBottom, double currentElixir) {
        if (handCardContainer == null)
            return;

        HandState hand = isBottom ? bottomHand : topHand;

        for (int i = 0; i < handCardContainer.getChildren().size(); i++) {
            var child = handCardContainer.getChildren().get(i);
            if (child instanceof StackPane && i < hand.handCards.size()) {
                Card card = hand.handCards.get(i);
                if (card != null) {
                    int cost = card.getElixirCost();
                    double progress = (cost <= 0) ? 1.0 : (currentElixir / cost);
                    StartGameUiBits.updateCardLoading((StackPane) child, progress);
                } else {
                    StartGameUiBits.updateCardLoading((StackPane) child, 0.0);
                }
            }
        }
    }

    private void updateClockHud() {
        if (match == null) {
            timerLabel.setText("00:00");
            phaseLabel.setText(formatPhase(ElixirPhase.NORMAL));
            return;
        }
        timerLabel.setText(formatTime(match.getRemainingSeconds()));
        phaseLabel.setText(formatPhase(match.getCurrentElixirPhase()));
    }

    private void refreshTurnHud() {
        if (activePlayer != null) {
            turnLabel.setText(activePlayer.getName());
        } else {
            turnLabel.setText("—");
        }
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setTextFill(error ? Color.web("#f05a5b") : Color.web("#9be564"));
    }

    private Label createMetricLabel(String value) {
        Label label = new Label(value);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        label.setTextFill(Color.WHITE);
        return label;
    }

    private VBox buildMetricPill(String caption, Label valueLabel) {
        Label captionLabel = new Label(caption.toUpperCase());
        captionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        captionLabel.setTextFill(Color.web("#8f94a3"));
        VBox pill = new VBox(2, captionLabel, valueLabel);
        pill.setAlignment(Pos.CENTER_LEFT);
        pill.setPadding(new Insets(8, 12, 8, 12));
        pill.setStyle("-fx-background-color: #1c1f2a; -fx-background-radius: 8;");
        return pill;
    }

    private String formatTime(double remainingSeconds) {
        int totalSeconds = (int) Math.ceil(remainingSeconds);
        totalSeconds = Math.max(0, totalSeconds);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String formatPhase(ElixirPhase phase) {
        if (phase == ElixirPhase.TRIPLE) {
            return "Triple";
        }
        return phase == ElixirPhase.DOUBLE ? "Double" : "Normal";
    }

    private double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private kuroyale.domain.TowerOwner resolveOwner(Player player) {
        if (player == null) {
            return kuroyale.domain.TowerOwner.PLAYER;
        }
        return player == bottomPlayer ? kuroyale.domain.TowerOwner.PLAYER : kuroyale.domain.TowerOwner.OPPONENT;
    }

    private List<Card> resolveDeckCards(Player p) {
        if (p == null || p.getDeck() == null) {
            return Collections.emptyList();
        }
        return p.getDeck().getCards();
    }

    private void togglePause() {
        paused = !paused;
        if (paused) {
            showPauseOverlay();
        } else {
            overlayLayer.getChildren().clear();
            overlayLayer.setVisible(false);
            overlayLayer.setMouseTransparent(true);
        }
    }

    private void showPauseOverlay() {
        overlayLayer.setVisible(true);
        overlayLayer.setMouseTransparent(false);
        overlayLayer.getChildren().clear();
        Rectangle dim = new Rectangle();
        dim.widthProperty().bind(overlayLayer.widthProperty());
        dim.heightProperty().bind(overlayLayer.heightProperty());
        dim.setFill(Color.color(0, 0, 0, 0.55));

        Label pausedLabel = new Label("Paused");
        pausedLabel.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        pausedLabel.setTextFill(Color.WHITE);

        Button resumeButton = new Button("Resume");
        resumeButton.setOnAction(e -> togglePause());

        Button exitButton = new Button("Return to Main Menu");
        exitButton.setOnAction(e -> {
            stopTicker();
            navigator.showWelcomeScreen();
        });

        HBox buttons = new HBox(12, resumeButton, exitButton);
        buttons.setAlignment(Pos.CENTER);

        VBox overlay = new VBox(20, pausedLabel, buttons);
        overlay.setAlignment(Pos.CENTER);

        overlayLayer.getChildren().addAll(dim, overlay);
        StackPane.setAlignment(overlay, Pos.CENTER);
    }

    private void openEmotePanel(boolean bottomSide) {
        emoteSenderBottom = bottomSide;
        emotePanel.toggle();
    }

    private void handleEmoteSelected(EmoteType type) {
        if (type == null) {
            return;
        }
        long now = System.currentTimeMillis();
        EmoteLimiter limiter = emoteSenderBottom ? bottomEmoteLimiter : topEmoteLimiter;
        var result = limiter.tryConsume(now);
        if (!result.isAllowed()) {
            showStatus("Emote blocked: " + result.getReason(), true);
            return;
        }
        if (!emoteSenderBottom && EmoteSettings.isMuteOpponentEmotes()) {
            return;
        }
        if (emoteSenderBottom) {
            emoteBubbles.showForBottom(type);
        } else {
            emoteBubbles.showForTop(type);
        }
        EmoteSound.play();
    }

    private StackPane buildCrownPill(String caption, Label valueLabel, StackPane crownTarget, String accentColor) {
        Label cap = new Label(caption.toUpperCase());
        cap.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        cap.setTextFill(Color.web("#8f94a3"));

        Label icon = new Label("♛");
        icon.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        icon.setTextFill(Color.web(accentColor));
        icon.setStyle("-fx-text-fill: " + accentColor + ";");
        valueLabel.setTextFill(Color.web(accentColor));
        crownTarget.getChildren().add(icon);

        HBox row = new HBox(6, crownTarget, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane pill = new StackPane(new VBox(2, cap, row));
        pill.setPadding(new Insets(8, 12, 8, 12));
        pill.setStyle("-fx-background-color: #1c1f2a; -fx-background-radius: 8;");
        return pill;
    }

    private StackPane createCrownTarget() {
        StackPane target = new StackPane();
        target.setMinSize(16, 16);
        target.setPrefSize(16, 16);
        return target;
    }

    private void detectTowerDestruction(Arena arena) {
        if (arena == null || arena.getTowers() == null) {
            return;
        }
        for (var tower : arena.getTowers()) {
            if (tower == null || tower.getPosition() == null || tower.getOwner() == null) {
                continue;
            }
            String key = tower.getOwner() + "|" + tower.getType() + "|" + tower.getPosition().getX() + "|"
                    + tower.getPosition().getY();
            if (tower.isDestroyed() && !destroyedTowerKeys.contains(key)) {
                destroyedTowerKeys.add(key);
                triggerCrownAnimation(tower);
            }
        }
    }

    private void triggerCrownAnimation(kuroyale.domain.Tower destroyedTower) {
        if (destroyedTower == null || destroyedTower.getPosition() == null) {
            return;
        }
        StackPane targetPill = destroyedTower.getOwner() == kuroyale.domain.TowerOwner.OPPONENT
                ? bottomCrownPill
                : topCrownPill;
        StackPane targetIcon = destroyedTower.getOwner() == kuroyale.domain.TowerOwner.OPPONENT
                ? bottomCrownTarget
                : topCrownTarget;

        Point2D startTile = arenaBoard.getTileCenterPx(destroyedTower.getPosition());
        if (startTile == null) {
            return;
        }
        Point2D startScene = arenaBoard.getOverlayLayer().localToScene(startTile);
        Point2D start = effectLayer.sceneToLocal(startScene);

        var targetBounds = targetIcon.localToScene(targetIcon.getBoundsInLocal());
        Point2D endScene = new Point2D(
                (targetBounds.getMinX() + targetBounds.getMaxX()) / 2.0,
                (targetBounds.getMinY() + targetBounds.getMaxY()) / 2.0);
        Point2D end = effectLayer.sceneToLocal(endScene);

        StackPane crown = createFlyingCrown();
        crown.setTranslateX(-CROWN_ICON_SIZE / 2.0);
        crown.setTranslateY(-CROWN_ICON_SIZE / 2.0);
        effectLayer.getChildren().add(crown);

        Path path = new Path();
        path.getElements().add(new MoveTo(start.getX(), start.getY()));
        double controlX = (start.getX() + end.getX()) / 2.0;
        double controlY = Math.min(start.getY(), end.getY()) - 80;
        path.getElements().add(new QuadCurveTo(controlX, controlY, end.getX(), end.getY()));

        PathTransition move = new PathTransition(Duration.seconds(CROWN_ANIM_DURATION_SECONDS), path, crown);
        move.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(120), crown);
        scaleUp.setFromX(0.7);
        scaleUp.setFromY(0.7);
        scaleUp.setToX(1.2);
        scaleUp.setToY(1.2);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(80), crown);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);

        TranslateTransition bounce = new TranslateTransition(Duration.millis(120), crown);
        bounce.setByY(-10);
        bounce.setAutoReverse(true);
        bounce.setCycleCount(2);

        SequentialTransition popScale = new SequentialTransition(scaleUp, scaleDown);
        ParallelTransition pop = new ParallelTransition(popScale, bounce);
        SequentialTransition full = new SequentialTransition(pop, move);

        full.setOnFinished(e -> {
            effectLayer.getChildren().remove(crown);
            if (destroyedTower.getOwner() == kuroyale.domain.TowerOwner.OPPONENT) {
                bottomCrowns = Math.min(3, bottomCrowns + 1);
                bottomCrownLabel.setText(String.valueOf(bottomCrowns));
            } else {
                topCrowns = Math.min(3, topCrowns + 1);
                topCrownLabel.setText(String.valueOf(topCrowns));
            }
            pulseCounter(targetPill);
        });
        full.play();
    }

    private StackPane createFlyingCrown() {
        Label crownIcon = new Label("♛");
        crownIcon.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 22));
        crownIcon.setTextFill(Color.web("#ffd54f"));
        StackPane crown = new StackPane(crownIcon);
        crown.setPrefSize(CROWN_ICON_SIZE, CROWN_ICON_SIZE);
        crown.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 6, 0.2, 0, 2);");
        return crown;
    }

    private void pulseCounter(StackPane pill) {
        if (pill == null) {
            return;
        }
        ScaleTransition up = new ScaleTransition(Duration.millis(120), pill);
        up.setToX(1.1);
        up.setToY(1.1);
        ScaleTransition down = new ScaleTransition(Duration.millis(120), pill);
        down.setToX(1.0);
        down.setToY(1.0);
        new SequentialTransition(up, down).play();
    }

    private void showGameOverOverlay() {
        MatchOutcome outcome = match != null ? match.getOutcome() : null;
        int bottomCrowns = outcome != null ? outcome.getPlayerCrowns() : 0;
        int topCrowns = outcome != null ? outcome.getOpponentCrowns() : 0;

        String headline = "Draw";
        boolean bottomWins = false;
        if (outcome != null && outcome.getWinner() != null) {
            bottomWins = outcome.getWinner() == kuroyale.domain.TowerOwner.PLAYER;
            headline = bottomWins ? "Victory" : "Defeat";
        } else if (match != null && match.isFinished()) {
            headline = "Draw";
        }

        // Update quest and achievement progress for Local PvP
        // Bottom player (Player 1) is tracked as the main player for quests
        if (navigator != null) {
            var questService = navigator.getQuestService();
            var achievementService = navigator.getAchievementService();
            application.QuestUpdateHelper.updateAfterMatch(questService, achievementService, match, bottomWins,
                    bottomCrowns);
        }

        String topName = topPlayer != null ? topPlayer.getName() : "Player 2";
        String bottomName = bottomPlayer != null ? bottomPlayer.getName() : "Player 1";

        MatchEndOverlay.show(
                overlayLayer,
                topName,
                topCrowns,
                bottomName,
                bottomCrowns,
                headline,
                () -> {
                    overlayLayer.getChildren().clear();
                    overlayLayer.setVisible(false);
                    overlayLayer.setMouseTransparent(true);
                    stopTicker();
                    if (navigator != null) {
                        navigator.showLocalPvPSetupScreen();
                    }
                },
                () -> {
                    overlayLayer.getChildren().clear();
                    overlayLayer.setVisible(false);
                    overlayLayer.setMouseTransparent(true);
                    stopTicker();
                    if (navigator != null) {
                        navigator.showWelcomeScreen();
                    }
                });
    }

    public Parent getRoot() {
        return root;
    }

    private static final class HandState {
        private final Deque<Card> drawPile = new ArrayDeque<>();
        private final List<Card> handCards = new ArrayList<>();
        private Card nextCard;

        private HandState(List<Card> deckCards) {
            drawPile.clear();
            handCards.clear();
            if (deckCards != null) {
                drawPile.addAll(deckCards);
            }
            for (int i = 0; i < 4; i++) {
                handCards.add(drawPile.pollFirst());
            }
            nextCard = drawPile.peekFirst();
        }

        private void cycle(int index) {
            if (index < 0 || index >= handCards.size()) {
                return;
            }
            Card played = handCards.get(index);
            if (played == null) {
                return;
            }
            drawPile.addLast(played);
            Card replacement = drawPile.pollFirst();
            handCards.set(index, replacement);
            nextCard = drawPile.peekFirst();
        }
    }
}
