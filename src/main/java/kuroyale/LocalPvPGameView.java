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
    private final VBox deckSectionContainer = new VBox();
    private Timeline matchTicker;

    private final HandState bottomHand;
    private final HandState topHand;
    private int selectedBottomIndex = -1;
    private int selectedTopIndex = -1;

    private Label bottomElixirLabel;
    private Region bottomElixirFill;
    private Label topElixirLabel;
    private Region topElixirFill;

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

        Region crownSpacer = new Region();
        HBox.setHgrow(crownSpacer, Priority.ALWAYS);
        HBox crownRow = new HBox(14, topCrownPill, crownSpacer, metricsRow, bottomCrownPill);
        crownRow.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(6, title, crownRow);
        header.setPadding(new Insets(10, 10, 15, 10));
        content.setTop(header);

        arenaBoard = new ArenaBoard(selectedLayout);
        arenaBoard.setOnTileSelected(this::handleTileSelection);
        overlayLayer.setVisible(false);
        overlayLayer.setMouseTransparent(true);
        StackPane boardLayer = new StackPane(arenaBoard.getView(), overlayLayer);
        content.setCenter(boardLayer);

        if (match != null && match.getArena() != null) {
            arenaBoard.render(match.getArena());
        }

        VBox hud = buildHudSection();
        content.setBottom(hud);

        refreshTurnHud();
        updateElixirHud();
        updateClockHud();
        startTicker();
    }

    private VBox buildHudSection() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: #181b22; -fx-border-color: #2d2f36; -fx-border-width: 2 0 0 0;");

        deckSectionContainer.setSpacing(6);
        refreshDeckSection();

        statusLabel.setTextFill(Color.web("#d2d7e5"));
        statusLabel.setWrapText(true);
        statusLabel.setText("Select a card, then click a valid tile on your side.");

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> togglePause());

        HBox controls = new HBox();
        controls.setAlignment(Pos.CENTER_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        controls.getChildren().addAll(spacer, pauseButton);

        container.getChildren().addAll(deckSectionContainer, statusLabel, controls);
        return container;
    }

    private VBox buildElixirPanel(Player p, boolean bottom) {
        Region fill = new Region();
        fill.setPrefSize(0, ELIXIR_BAR_HEIGHT);
        fill.setStyle("-fx-background-color: linear-gradient(to right, #b259ff, #7a4dff); -fx-background-radius: 10;");

        Region track = new Region();
        track.setPrefSize(ELIXIR_BAR_WIDTH, ELIXIR_BAR_HEIGHT);
        track.setStyle(
                "-fx-background-color: #141724; -fx-border-color: #3b3f55; -fx-border-radius: 10; -fx-background-radius: 10;");

        StackPane bar = new StackPane(track, fill);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        Label name = new Label(p != null ? p.getName() : (bottom ? "Player 1" : "Player 2"));
        name.setTextFill(Color.web("#cbd0d6"));
        name.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        Label value = new Label("0");
        value.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        value.setTextFill(Color.WHITE);

        if (bottom) {
            bottomElixirFill = fill;
            bottomElixirLabel = value;
        } else {
            topElixirFill = fill;
            topElixirLabel = value;
        }

        VBox panel = new VBox(4, name, bar, value);
        panel.setAlignment(Pos.CENTER_LEFT);
        return panel;
    }

    private void refreshDeckSection() {
        deckSectionContainer.getChildren().clear();
        VBox bottomPanel = buildDeckPanel(bottomPlayer, bottomHand, true, activePlayer == bottomPlayer,
                selectedBottomIndex);
        VBox topPanel = buildDeckPanel(topPlayer, topHand, false, activePlayer == topPlayer, selectedTopIndex);
        HBox row = new HBox(18, bottomPanel, topPanel);
        row.setAlignment(Pos.CENTER_LEFT);
        deckSectionContainer.getChildren().add(row);
    }

    private VBox buildDeckPanel(Player player,
            HandState hand,
            boolean bottomSide,
            boolean isActive,
            int selectedIndex) {
        VBox panel = new VBox(8);
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(360);
        panel.setStyle(isActive
                ? "-fx-background-color: #1c1f2a; -fx-background-radius: 10; -fx-border-color: #ffd54f; -fx-border-radius: 10; -fx-border-width: 2;"
                : "-fx-background-color: #1c1f2a; -fx-background-radius: 10; -fx-border-color: #2d2f36; -fx-border-radius: 10; -fx-border-width: 1;");

        String displayName = player != null ? player.getName() : (bottomSide ? "Player 1" : "Player 2");
        Label name = new Label(displayName);
        name.setTextFill(Color.web("#cbd0d6"));
        name.setFont(Font.font("Arial", FontWeight.BOLD, 13));

        Button emoteButton = new Button("Emote");
        emoteButton.setOnAction(e -> openEmotePanel(bottomSide));

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(8, name, headerSpacer, emoteButton);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox handRow = new HBox(8);
        handRow.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < 4; i++) {
            Card card = i < hand.handCards.size() ? hand.handCards.get(i) : null;
            boolean highlighted = isActive && i == selectedIndex;
            StackPane slot = StartGameUiBits.createCardSlot(card, true, highlighted);
            final int index = i;
            slot.setOnMouseClicked(event -> selectHandIndex(player, bottomSide, index));
            handRow.getChildren().add(slot);
        }

        StackPane nextSlot = StartGameUiBits.createCardSlot(hand.nextCard, false, false);
        VBox nextColumn = new VBox(4);
        nextColumn.setAlignment(Pos.CENTER);
        Label nextLabel = new Label("Next");
        nextLabel.setTextFill(Color.web("#8f94a3"));
        nextLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        nextColumn.getChildren().addAll(nextLabel, nextSlot);

        HBox deckRow = new HBox(12, nextColumn, handRow);
        deckRow.setAlignment(Pos.CENTER_LEFT);

        VBox elixir = buildElixirPanel(player, bottomSide);
        panel.getChildren().addAll(header, deckRow, elixir);
        return panel;
    }

    private void selectHandIndex(Player clickedPlayer, boolean bottomSide, int index) {
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
            refreshDeckSection();
            return;
        }
        if (bottomSide) {
            selectedBottomIndex = index;
            selectedTopIndex = -1;
        } else {
            selectedTopIndex = index;
            selectedBottomIndex = -1;
        }
        refreshDeckSection();
        Card selected = hand.handCards.get(index);
        if (selected != null) {
            showStatus(activePlayer.getName() + " selected: " + selected.getName(), false);
        }
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

        Result<?> sideCheck = validateDeploySide(activePlayer, tile);
        if (!sideCheck.isSuccess()) {
            showStatus(sideCheck.getMessage(), true);
            return;
        }

        Result<?> result = controller != null ? controller.deployCard(activePlayer, card, tile)
                : Result.fail("Match controller missing.");
        if (!result.isSuccess()) {
            showStatus(result.getMessage(), true);
            return;
        }

        hand.cycle(selectedIndex);
        selectedBottomIndex = -1;
        selectedTopIndex = -1;
        refreshDeckSection();
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
        refreshDeckSection();
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
        // Precise Elixir Calculation
        double p1Elixir = (bottomPlayer != null) ? bottomPlayer.getCurrentElixir() : 0;
        double p2Elixir = (topPlayer != null) ? topPlayer.getCurrentElixir() : 0;

        if (match != null) {
            p1Elixir += match.getPlayerElixirFraction();
            p2Elixir += match.getOpponentElixirFraction();
        }

        // Update Bottom HUD
        if (bottomPlayer != null && bottomElixirLabel != null && bottomElixirFill != null) {
            bottomElixirLabel.setText("Elixir: " + (int) p1Elixir); // Show integer part
            double ratio = bottomPlayer.getMaxElixir() == 0 ? 0 : p1Elixir / bottomPlayer.getMaxElixir();
            bottomElixirFill.setPrefWidth(ELIXIR_BAR_WIDTH * clamp01(ratio));

            // Update Card Loading Masks
            updateDeckLoadingState(true, p1Elixir);
        }

        // Update Top HUD
        if (topPlayer != null && topElixirLabel != null && topElixirFill != null) {
            topElixirLabel.setText("Elixir: " + (int) p2Elixir);
            double ratio = topPlayer.getMaxElixir() == 0 ? 0 : p2Elixir / topPlayer.getMaxElixir();
            topElixirFill.setPrefWidth(ELIXIR_BAR_WIDTH * clamp01(ratio));

            updateDeckLoadingState(false, p2Elixir);
        }
    }

    private void updateDeckLoadingState(boolean isBottom, double currentElixir) {
        // Traverse to find card slots
        // Structure: deckSectionContainer(VBox) -> Row(HBox) -> Panel(VBox) ->
        // DeckRow(HBox) -> HandRow(HBox) -> Slots(StackPane)
        if (deckSectionContainer.getChildren().isEmpty())
            return;

        var rowNode = deckSectionContainer.getChildren().get(0);
        if (!(rowNode instanceof HBox))
            return;
        HBox row = (HBox) rowNode;
        if (row.getChildren().size() < 2)
            return;

        VBox panel = (VBox) row.getChildren().get(isBottom ? 0 : 1);
        if (panel.getChildren().size() < 2)
            return; // Name, DeckRow, Elixir

        var deckRowNode = panel.getChildren().get(1);
        if (!(deckRowNode instanceof HBox))
            return;
        HBox deckRow = (HBox) deckRowNode;

        if (deckRow.getChildren().size() < 2)
            return;
        var handRowNode = deckRow.getChildren().get(1); // 0 is Next, 1 is Hand
        if (!(handRowNode instanceof HBox))
            return;
        HBox handRow = (HBox) handRowNode;

        HandState hand = isBottom ? bottomHand : topHand;

        for (int i = 0; i < handRow.getChildren().size(); i++) {
            var child = handRow.getChildren().get(i);
            if (child instanceof StackPane && i < hand.handCards.size()) {
                Card card = hand.handCards.get(i);
                if (card != null) {
                    int cost = card.getElixirCost();
                    // Avoid division by zero
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
        if (outcome != null && outcome.getWinner() != null) {
            headline = outcome.getWinner() == kuroyale.domain.TowerOwner.PLAYER ? "Victory" : "Defeat";
        } else if (match != null && match.isFinished()) {
            headline = "Draw";
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
