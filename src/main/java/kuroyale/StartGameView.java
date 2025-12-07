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
import kuroyale.domain.Arena;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.Card;
import kuroyale.domain.ElixirPhase;
import kuroyale.domain.Match;
import kuroyale.domain.Player;
import kuroyale.domain.Position;
import kuroyale.support.Result;

public class StartGameView {

    private static final double ELIXIR_BAR_WIDTH = 460;
    private static final double ELIXIR_BAR_HEIGHT = 18;

    private final BorderPane root;
    private final Match match;
    private final Player player;
    private final ArenaBoard arenaBoard;
    private final Label timerLabel;
    private final Label phaseLabel;
    private final Label statusLabel = new Label();
    private final VBox deckSectionContainer = new VBox();
    private final Deque<Card> drawPile = new ArrayDeque<>();
    private final List<Card> handCards = new ArrayList<>();
    private final List<StackPane> handSlotViews = new ArrayList<>();
    private Timeline matchTicker;
    private Label elixirValueLabel;
    private Region elixirFill;
    private Card nextCard;
    private int selectedHandIndex = -1;
    private boolean paused = false;
    private final StackPane overlayLayer = new StackPane();
    private final ScreenNavigator navigator;

    public StartGameView(ScreenNavigator navigator, Match match, ArenaLayout selectedLayout) {
        this.navigator = navigator;
        this.match = match;
        this.player = match != null ? match.getPlayer() : null;
        initializeDeckState(resolveDeckCards());

        root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #0f1216;");

        Label title = new Label("Battlefield: " + selectedLayout.getName());
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#f5f5f5"));

        Label subtitle = new Label("Select a card and click the arena to deploy it.");
        subtitle.setTextFill(Color.web("#cbd0d6"));

        timerLabel = createMetricLabel(formatTime(match != null ? match.getRemainingSeconds() : 0));
        phaseLabel = createMetricLabel(formatPhase(match != null ? match.getCurrentElixirPhase() : ElixirPhase.DOUBLE));

        HBox metricsRow = new HBox(20,
            buildMetricPill("Phase", phaseLabel),
            buildMetricPill("Time", timerLabel)
        );
        metricsRow.setAlignment(Pos.CENTER_RIGHT);

        VBox header = new VBox(6, title, subtitle, metricsRow);
        header.setPadding(new Insets(10, 10, 15, 10));
        root.setTop(header);

        arenaBoard = new ArenaBoard(selectedLayout);
        arenaBoard.setOnTileSelected(this::handleTileSelection);
        overlayLayer.setVisible(false);
        overlayLayer.setMouseTransparent(true);
        StackPane boardLayer = new StackPane(arenaBoard.getView(), overlayLayer);
        root.setCenter(boardLayer);
        if (match != null) {
            Arena arena = match.getArena();
            if (arena != null) {
                arenaBoard.renderUnits(arena.getUnits());
            }
        }

        VBox hud = buildHudSection();
        root.setBottom(hud);

        updateElixirHud();
        updateClockHud();
        startTicker();
    }

    private void initializeDeckState(List<Card> deckCards) {
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

    private Label createMetricLabel(String value) {
        Label label = new Label(value);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 18));
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

    private VBox buildHudSection() {
        VBox container = new VBox(14);
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: #181b22; -fx-border-color: #2d2f36; -fx-border-width: 2 0 0 0;");

        deckSectionContainer.setSpacing(6);
        refreshDeckSection();

        VBox elixirPanel = buildElixirPanel();

        statusLabel.setTextFill(Color.web("#d2d7e5"));
        statusLabel.setWrapText(true);
        statusLabel.setText("Select a card and click a tile on your side of the arena.");

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> togglePause());

        HBox controls = new HBox();
        controls.setAlignment(Pos.CENTER_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        controls.getChildren().addAll(spacer, pauseButton);

        container.getChildren().addAll(deckSectionContainer, elixirPanel, statusLabel, controls);
        return container;
    }

    private void refreshDeckSection() {
        handSlotViews.clear();
        deckSectionContainer.getChildren().clear();

        HBox handRow = new HBox(8);
        handRow.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < 4; i++) {
            Card card = i < handCards.size() ? handCards.get(i) : null;
            StackPane slot = createCardSlot(card, true);
            final int index = i;
            slot.setOnMouseClicked(event -> selectHandIndex(index));
            handSlotViews.add(slot);
            handRow.getChildren().add(slot);
        }

        StackPane nextSlot = createCardSlot(nextCard, false);
        VBox nextColumn = new VBox(4);
        nextColumn.setAlignment(Pos.CENTER);
        Label nextLabel = new Label("Next");
        nextLabel.setTextFill(Color.web("#8f94a3"));
        nextLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        nextColumn.getChildren().addAll(nextLabel, nextSlot);

        HBox deckRow = new HBox(12, nextColumn, handRow);
        deckRow.setAlignment(Pos.CENTER_LEFT);

        deckSectionContainer.getChildren().add(deckRow);
        updateHandSelection();
    }

    private VBox buildElixirPanel() {
        elixirFill = new Region();
        elixirFill.setPrefSize(0, ELIXIR_BAR_HEIGHT);
        elixirFill.setStyle("-fx-background-color: linear-gradient(to right, #b259ff, #7a4dff); -fx-background-radius: 10;");

        Region track = new Region();
        track.setPrefSize(ELIXIR_BAR_WIDTH, ELIXIR_BAR_HEIGHT);
        track.setStyle("-fx-background-color: #141724; -fx-border-color: #3b3f55; -fx-border-radius: 10; -fx-background-radius: 10;");

        StackPane bar = new StackPane(track, elixirFill);
        StackPane.setAlignment(elixirFill, Pos.CENTER_LEFT);

        elixirValueLabel = new Label("0");
        elixirValueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        elixirValueLabel.setTextFill(Color.WHITE);

        int maxElixir = player != null ? player.getMaxElixir() : 10;
        Label maxLabel = new Label("Max: " + maxElixir);
        maxLabel.setTextFill(Color.web("#8f94a3"));

        HBox values = new HBox(10, elixirValueLabel, maxLabel);
        values.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(6, bar, values);
        panel.setAlignment(Pos.CENTER_LEFT);
        return panel;
    }

    private StackPane createCardSlot(Card card, boolean large) {
        double width = large ? 70 : 54;
        double height = large ? 90 : 70;

        StackPane tile = new StackPane();
        tile.setPrefSize(width, height);
        applySlotStyle(tile, large, false);

        VBox content = new VBox(4);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(6));

        Label name = new Label(card != null ? card.getName() : "Empty");
        name.setWrapText(true);
        name.setAlignment(Pos.CENTER);
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font("Arial", FontWeight.BOLD, large ? 11 : 9));

        Label hp = new Label(card != null && card.getStats() != null ? "HP " + card.getStats().getHp() : "HP —");
        hp.setTextFill(Color.web("#a6ffcb"));
        hp.setFont(Font.font("Arial", large ? 10 : 8));

        StackPane costChip = buildCostChip(card != null ? card.getElixirCost() : -1, large);
        StackPane.setAlignment(costChip, Pos.BOTTOM_CENTER);
        StackPane.setMargin(costChip, new Insets(0, 0, 4, 0));

        content.getChildren().addAll(name, hp);

        tile.getChildren().addAll(content, costChip);
        return tile;
    }

    private void applySlotStyle(StackPane tile, boolean large, boolean highlighted) {
        StringBuilder style = new StringBuilder();
        style.append("-fx-background-color: linear-gradient(#2a2f45, #1c1f2e);");
        if (large) {
            style.append("-fx-background-radius: 10; -fx-border-radius: 10;");
        } else {
            style.append("-fx-background-radius: 8; -fx-border-radius: 8;");
        }
        if (highlighted) {
            style.append("-fx-border-color: #ffd54f; -fx-border-width: 3;");
        } else {
            style.append("-fx-border-color: #404459; -fx-border-width: 1;");
        }
        tile.setStyle(style.toString());
    }

    private StackPane buildCostChip(int cost, boolean large) {
        String text = cost >= 0 ? String.valueOf(cost) : "-";
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, large ? 14 : 12));
        label.setTextFill(Color.WHITE);

        StackPane chip = new StackPane(label);
        chip.setPadding(new Insets(4, 10, 4, 10));
        chip.setStyle("-fx-background-color: #b259ff; -fx-background-radius: 20;");
        return chip;
    }

    private void selectHandIndex(int index) {
        if (index < 0 || index >= handCards.size() || handCards.get(index) == null) {
            selectedHandIndex = -1;
            updateHandSelection();
            return;
        }
        selectedHandIndex = index;
        updateHandSelection();
        Card selected = handCards.get(index);
        if (selected != null) {
            showStatus(selected.getName() + " selected.", false);
        }
    }

    private void updateHandSelection() {
        for (int i = 0; i < handSlotViews.size(); i++) {
            StackPane slot = handSlotViews.get(i);
            applySlotStyle(slot, true, i == selectedHandIndex);
        }
    }

    private void handleTileSelection(Position tile) {
        if (match == null || player == null) {
            showStatus("Match is not ready yet.", true);
            return;
        }
        if (selectedHandIndex < 0 || selectedHandIndex >= handCards.size()) {
            showStatus("Select a card before deploying.", true);
            return;
        }
        Card card = handCards.get(selectedHandIndex);
        if (card == null) {
            showStatus("Selected slot is empty.", true);
            return;
        }
        Result<?> result = match.deployCard(player, card, tile);
        if (!result.isSuccess()) {
            showStatus(result.getMessage(), true);
            return;
        }
        cycleCard(selectedHandIndex);
        showStatus(card.getName() + " deployed at (" + tile.getX() + ", " + tile.getY() + ").", false);
        selectedHandIndex = -1;
        updateHandSelection();
        Arena arena = match.getArena();
        if (arena != null) {
            arenaBoard.renderUnits(arena.getUnits());
        }
        updateElixirHud();
    }

    private void cycleCard(int index) {
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
        refreshDeckSection();
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setTextFill(error ? Color.web("#f05a5b") : Color.web("#9be564"));
    }

    private List<Card> resolveDeckCards() {
        if (player == null) {
            return Collections.emptyList();
        }
        if (player.getDeck() == null) {
            return Collections.emptyList();
        }
        return player.getDeck().getCards();
    }

    private void startTicker() {
        if (match == null) {
            return;
        }
        matchTicker = new Timeline(new KeyFrame(Duration.seconds(0.5), e -> {
            if (!paused) {
                match.advanceTime(0.5);
                updateElixirHud();
                updateClockHud();
                Arena arena = match.getArena();
                if (arena != null) {
                    arenaBoard.renderUnits(arena.getUnits());
                }
            }
            if (match.isFinished()) {
                stopTicker();
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
        if (player == null) {
            elixirValueLabel.setText("0");
            elixirFill.setPrefWidth(0);
            return;
        }
        elixirValueLabel.setText(String.valueOf(player.getCurrentElixir()));
        double ratio = player.getMaxElixir() == 0 ? 0 : (double) player.getCurrentElixir() / player.getMaxElixir();
        ratio = Math.min(1.0, Math.max(0.0, ratio));
        elixirFill.setPrefWidth(ELIXIR_BAR_WIDTH * ratio);
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

    private String formatTime(double remainingSeconds) {
        int totalSeconds = (int) Math.ceil(remainingSeconds);
        totalSeconds = Math.max(0, totalSeconds);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String formatPhase(ElixirPhase phase) {
        return phase == ElixirPhase.TRIPLE ? "Triple Elixir" : "Double Elixir";
    }

    public Parent getRoot() {
        return root;
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
}
