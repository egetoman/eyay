package kuroyale;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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

    private final BorderPane root;
    private final MatchController controller;
    private final Match match;
    private final Player bottomPlayer;
    private final Player topPlayer;
    private Player activePlayer;
    private final ArenaBoard arenaBoard;
    private final Label timerLabel;
    private final Label phaseLabel;
    private final Label turnLabel;
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

    public LocalPvPGameView(ScreenNavigator navigator, MatchController controller, ArenaLayout selectedLayout) {
        this.navigator = navigator;
        this.controller = controller;
        this.match = controller != null ? controller.getMatch() : null;
        this.bottomPlayer = controller != null ? controller.getPlayer() : null;
        this.topPlayer = controller != null ? controller.getOpponent() : null;
        this.activePlayer = bottomPlayer;
        this.bottomHand = new HandState(resolveDeckCards(bottomPlayer));
        this.topHand = new HandState(resolveDeckCards(topPlayer));

        root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #0f1216;");

        Label title = new Label("Local PvP");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#f5f5f5"));

        turnLabel = new Label();
        turnLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        turnLabel.setTextFill(Color.web("#ffd54f"));

        timerLabel = createMetricLabel(formatTime(match != null ? match.getRemainingSeconds() : 0));
        phaseLabel = createMetricLabel(formatPhase(match != null ? match.getCurrentElixirPhase() : ElixirPhase.DOUBLE));

        HBox metricsRow = new HBox(16,
                buildMetricPill("Turn", turnLabel),
                buildMetricPill("Phase", phaseLabel),
                buildMetricPill("Time", timerLabel));
        metricsRow.setAlignment(Pos.CENTER_RIGHT);

        VBox header = new VBox(6, title, metricsRow);
        header.setPadding(new Insets(10, 10, 15, 10));
        root.setTop(header);

        arenaBoard = new ArenaBoard(selectedLayout);
        arenaBoard.setOnTileSelected(this::handleTileSelection);
        overlayLayer.setVisible(false);
        overlayLayer.setMouseTransparent(true);
        StackPane boardLayer = new StackPane(arenaBoard.getView(), overlayLayer);
        root.setCenter(boardLayer);

        if (match != null && match.getArena() != null) {
            arenaBoard.render(match.getArena());
        }

        VBox hud = buildHudSection();
        root.setBottom(hud);

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
        panel.getChildren().addAll(name, deckRow, elixir);
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
        matchTicker = new Timeline(new KeyFrame(Duration.seconds(0.5), e -> {
            if (!paused) {
                if (controller != null) {
                    controller.advanceTime(0.5);
                } else {
                    match.advanceTime(0.5);
                }
                updateElixirHud();
                updateClockHud();
                if (match.getArena() != null) {
                    arenaBoard.render(match.getArena());
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
        if (bottomPlayer != null && bottomElixirLabel != null && bottomElixirFill != null) {
            bottomElixirLabel.setText("Elixir: " + bottomPlayer.getCurrentElixir());
            double ratio = bottomPlayer.getMaxElixir() == 0 ? 0
                    : (double) bottomPlayer.getCurrentElixir() / bottomPlayer.getMaxElixir();
            bottomElixirFill.setPrefWidth(ELIXIR_BAR_WIDTH * clamp01(ratio));
        }
        if (topPlayer != null && topElixirLabel != null && topElixirFill != null) {
            topElixirLabel.setText("Elixir: " + topPlayer.getCurrentElixir());
            double ratio = topPlayer.getMaxElixir() == 0 ? 0
                    : (double) topPlayer.getCurrentElixir() / topPlayer.getMaxElixir();
            topElixirFill.setPrefWidth(ELIXIR_BAR_WIDTH * clamp01(ratio));
        }
    }

    private void updateClockHud() {
        if (match == null) {
            timerLabel.setText("00:00");
            phaseLabel.setText(formatPhase(ElixirPhase.DOUBLE));
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
        return phase == ElixirPhase.TRIPLE ? "Triple" : "Double";
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

    private void showGameOverOverlay() {
        overlayLayer.setVisible(true);
        overlayLayer.setMouseTransparent(false);
        overlayLayer.getChildren().clear();
        Rectangle dim = new Rectangle();
        dim.widthProperty().bind(overlayLayer.widthProperty());
        dim.heightProperty().bind(overlayLayer.heightProperty());
        dim.setFill(Color.color(0, 0, 0, 0.65));

        MatchOutcome outcome = match != null ? match.getOutcome() : null;
        String winnerText = "Draw";
        if (outcome != null && outcome.getWinner() != null) {
            winnerText = outcome.getWinner() == kuroyale.domain.TowerOwner.PLAYER
                    ? (bottomPlayer != null ? bottomPlayer.getName() : "Player 1")
                    : (topPlayer != null ? topPlayer.getName() : "Player 2");
        }

        Label over = new Label("Game Over");
        over.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        over.setTextFill(Color.WHITE);

        Label winner = new Label("Winner: " + winnerText);
        winner.setTextFill(Color.web("#ffd54f"));
        winner.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Button exitButton = new Button("Return to Main Menu");
        exitButton.setOnAction(e -> {
            overlayLayer.getChildren().clear();
            overlayLayer.setVisible(false);
            overlayLayer.setMouseTransparent(true);
            navigator.showWelcomeScreen();
        });

        VBox overlay = new VBox(16, over, winner, exitButton);
        overlay.setAlignment(Pos.CENTER);

        overlayLayer.getChildren().addAll(dim, overlay);
        StackPane.setAlignment(overlay, Pos.CENTER);
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
