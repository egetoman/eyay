package kuroyale;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import application.ComboDetector;
import application.ComboEffectApplier;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
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
import application.replay.ReplayRecorder;
import kuroyale.domain.Arena;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.Card;
import kuroyale.domain.CardType;
import kuroyale.domain.ElixirPhase;
import kuroyale.domain.Match;
import kuroyale.domain.MatchOutcome;
import kuroyale.domain.Player;
import kuroyale.domain.Position;
import kuroyale.domain.Unit;
import kuroyale.infrastructure.PlayerProfileRepository;
import kuroyale.infrastructure.GameEventLogger;
import kuroyale.support.Result;
import kuroyale.emote.EmoteBubbleManager;
import kuroyale.emote.EmoteLimiter;
import kuroyale.emote.EmotePanel;
import kuroyale.emote.EmoteSound;
import kuroyale.emote.EmoteType;

public class StartGameView {

    private static final double ELIXIR_BAR_WIDTH = 460;
    private static final double ELIXIR_BAR_HEIGHT = 18;
    private static final double CROWN_ANIM_DURATION_SECONDS = 0.7;
    private static final double CROWN_ICON_SIZE = 26;

    private final StackPane root;
    private final BorderPane content;
    private final Pane effectLayer;
    private final MatchController controller;
    private final Match match;
    private final Player player;
    private final ArenaBoard arenaBoard;
    private final Label timerLabel;
    private final Label phaseLabel;
    private final Label playerCrownLabel;
    private final Label opponentCrownLabel;
    private final StackPane playerCrownTarget;
    private final StackPane opponentCrownTarget;
    private final StackPane playerCrownPill;
    private final StackPane opponentCrownPill;
    private final Label statusLabel = new Label();
    private final VBox deckSectionContainer = new VBox();
    private final Deque<Card> drawPile = new ArrayDeque<>();
    private final List<Card> handCards = new ArrayList<>();
    private Timeline matchTicker;
    private Label elixirValueLabel;
    private Rectangle elixirFill;
    private Card nextCard;
    private int selectedHandIndex = -1;
    private boolean paused = false;
    private final StackPane overlayLayer = new StackPane();
    private final ScreenNavigator navigator;
    private final ArenaLayout selectedLayout;
    private final ReplayRecorder replayRecorder;
    private boolean matchRecorded = false;
    private final ComboDetector comboDetector;
    private Label comboCountLabel;
    private Timeline comboMessageTimer;
    private final Set<String> destroyedTowerKeys = new HashSet<>();
    private int playerCrowns = 0;
    private int opponentCrowns = 0;
    private final EmoteBubbleManager emoteBubbles;
    private final EmoteLimiter playerEmoteLimiter = EmoteLimiter.defaultLimiter();
    private final EmotePanel emotePanel;
    private final StackPane emotePanelLayer = new StackPane();

    public StartGameView(ScreenNavigator navigator, MatchController controller, ArenaLayout selectedLayout) {
        this.navigator = navigator;
        this.controller = controller;
        this.match = controller != null ? controller.getMatch() : null;
        this.player = controller != null ? controller.getPlayer() : null;
        this.selectedLayout = selectedLayout;
        this.replayRecorder = new ReplayRecorder(selectedLayout != null ? selectedLayout.getId() : null, 500);
        this.comboDetector = new ComboDetector();
        this.comboDetector.setListener((comboType, comboName, effectDescription) -> {
            showComboMessage(comboName, effectDescription);
            GameEventLogger.log("COMBO_TRIGGERED type=" + comboType + " name=\"" + comboName + "\"");
        });
        initializeDeckState(resolveDeckCards());
        GameEventLogger.resetForMatch();
        GameEventLogger.log("MATCH_START arena=" + (selectedLayout != null ? selectedLayout.getName() : "unknown"));

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
        emotePanelLayer.setMouseTransparent(false);
        emotePanelLayer.getChildren().add(emotePanel.getView());
        StackPane.setAlignment(emotePanel.getView(), Pos.BOTTOM_RIGHT);
        StackPane.setMargin(emotePanel.getView(), new Insets(0, 24, 120, 0));
        root.getChildren().add(emotePanelLayer);

        emoteBubbles = new EmoteBubbleManager(effectLayer);

        Label title = new Label("Battlefield: " + selectedLayout.getName());
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#f5f5f5"));

        Label subtitle = new Label("Select a card and click the arena to deploy it.");
        subtitle.setTextFill(Color.web("#cbd0d6"));

        timerLabel = createMetricLabel(formatTime(match != null ? match.getRemainingSeconds() : 0));
        phaseLabel = createMetricLabel(formatPhase(match != null ? match.getCurrentElixirPhase() : ElixirPhase.NORMAL));

        comboCountLabel = createMetricLabel("Combos: 0");

        opponentCrownLabel = createMetricLabel("0");
        opponentCrownTarget = createCrownTarget();
        opponentCrownPill = buildCrownPill("Enemy", opponentCrownLabel, opponentCrownTarget, "#b63a4c");

        playerCrownLabel = createMetricLabel("0");
        playerCrownTarget = createCrownTarget();
        playerCrownPill = buildCrownPill("You", playerCrownLabel, playerCrownTarget, "#2a6fd2");

        HBox metricsRow = new HBox(14,
                buildMetricPill("Phase", phaseLabel),
                buildMetricPill("Time", timerLabel),
                buildMetricPill("Combos", comboCountLabel));
        metricsRow.setAlignment(Pos.CENTER_RIGHT);

        Region crownSpacer = new Region();
        HBox.setHgrow(crownSpacer, Priority.ALWAYS);
        HBox crownRow = new HBox(14, opponentCrownPill, crownSpacer, metricsRow, playerCrownPill);
        crownRow.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(6, title, subtitle, crownRow);
        header.setPadding(new Insets(10, 10, 15, 10));
        content.setTop(header);

        arenaBoard = new ArenaBoard(selectedLayout);
        arenaBoard.setOnTileSelected(this::handleTileSelection);
        overlayLayer.setVisible(false);
        overlayLayer.setMouseTransparent(true);
        StackPane boardLayer = new StackPane(arenaBoard.getView(), overlayLayer);
        content.setCenter(boardLayer);
        if (match != null) {
            Arena arena = match.getArena();
            if (arena != null) {
                arenaBoard.render(arena);
            }
        }

        VBox hud = buildHudSection();
        content.setBottom(hud);

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

        Button emoteButton = new Button("Emotes");
        emoteButton.setOnAction(e -> emotePanel.toggle());

        HBox controls = new HBox();
        controls.setAlignment(Pos.CENTER_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        controls.getChildren().addAll(spacer, emoteButton, pauseButton);

        container.getChildren().addAll(deckSectionContainer, elixirPanel, statusLabel, controls);
        return container;
    }

    private void refreshDeckSection() {
        deckSectionContainer.getChildren().clear();

        HBox handRow = new HBox(8);
        handRow.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < 4; i++) {
            Card card = i < handCards.size() ? handCards.get(i) : null;
            StackPane slot = StartGameUiBits.createCardSlot(card, true, i == selectedHandIndex);
            final int index = i;
            slot.setOnMouseClicked(event -> selectHandIndex(index, slot));
            handRow.getChildren().add(slot);
        }

        StackPane nextSlot = StartGameUiBits.createCardSlot(nextCard, false, false);
        VBox nextColumn = new VBox(4);
        nextColumn.setAlignment(Pos.CENTER);
        Label nextLabel = new Label("Next");
        nextLabel.setTextFill(Color.web("#8f94a3"));
        nextLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        nextColumn.getChildren().addAll(nextLabel, nextSlot);

        HBox deckRow = new HBox(12, nextColumn, handRow);
        deckRow.setAlignment(Pos.CENTER_LEFT);

        deckSectionContainer.getChildren().add(deckRow);
    }

    private VBox buildElixirPanel() {
        elixirFill = new Rectangle(0, ELIXIR_BAR_HEIGHT);
        elixirFill.setArcWidth(20);
        elixirFill.setArcHeight(20);
        elixirFill.setStyle("-fx-fill: linear-gradient(to right, #b259ff, #7a4dff);");

        Region track = new Region();
        track.setPrefSize(ELIXIR_BAR_WIDTH, ELIXIR_BAR_HEIGHT);
        track.setMaxWidth(ELIXIR_BAR_WIDTH); // Fix: Prevent track expansion
        track.setStyle(
                "-fx-background-color: #141724; -fx-border-color: #3b3f55; -fx-border-radius: 10; -fx-background-radius: 10;");

        HBox ticks = new HBox();
        ticks.setPrefSize(ELIXIR_BAR_WIDTH, ELIXIR_BAR_HEIGHT);
        ticks.setMaxWidth(ELIXIR_BAR_WIDTH); // Fix: Prevent ticks expansion
        ticks.setAlignment(Pos.CENTER_LEFT);
        double segmentWidth = ELIXIR_BAR_WIDTH / 10.0;

        for (int i = 0; i < 9; i++) {
            Region spacer = new Region();
            spacer.setPrefWidth(segmentWidth - 1);
            Region divider = new Region();
            divider.setPrefSize(1, ELIXIR_BAR_HEIGHT);
            divider.setStyle("-fx-background-color: rgba(255,255,255,0.2);");
            ticks.getChildren().addAll(spacer, divider);
        }
        ticks.setMouseTransparent(true);

        StackPane bar = new StackPane(track, elixirFill, ticks);
        bar.setMaxWidth(ELIXIR_BAR_WIDTH); // Fix: Prevent container expansion
        StackPane.setAlignment(elixirFill, Pos.CENTER_LEFT);
        StackPane.setAlignment(ticks, Pos.CENTER_LEFT);

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

    private void selectHandIndex(int index, StackPane slot) {
        if (index < 0 || index >= handCards.size() || handCards.get(index) == null) {
            selectedHandIndex = -1;
            refreshDeckSection();
            return;
        }
        Card candidate = handCards.get(index);
        if (candidate != null && !canAfford(candidate)) {
            showStatus("Not enough elixir.", true);
            shakeNode(slot);
            return;
        }
        selectedHandIndex = index;
        refreshDeckSection();
        Card selected = handCards.get(index);
        if (selected != null) {
            showStatus(selected.getName() + " selected.", false);
        }
    }

    private boolean canAfford(Card card) {
        if (card == null || player == null) {
            return false;
        }
        double current = resolveCurrentElixir();
        return current + 0.001 >= card.getElixirCost();
    }

    private double resolveCurrentElixir() {
        double preciseElixir = player != null ? player.getCurrentElixir() : 0;
        if (match != null) {
            preciseElixir += match.getPlayerElixirFraction();
        }
        return preciseElixir;
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
        Result<Unit> result = controller != null ? controller.deployCard(player, card, tile)
                : Result.fail("Match controller missing.");
        if (!result.isSuccess()) {
            showStatus(result.getMessage(), true);
            return;
        }

        Unit deployedUnit = result.getData();
        GameEventLogger.log("CARD_PLAY id=" + card.getId() + " name=\"" + card.getName() + "\" pos=(" + tile.getX() + "," + tile.getY() + ")");

        // Record spell cast for visual effects
        if (card.getType() == CardType.SPELL) {
            arenaBoard.recordSpellCast(card.getId(), tile);
        } else {
            arenaBoard.recordDeployEffect(kuroyale.domain.TowerOwner.PLAYER, tile);
        }

        // Record card play for combo detection
        double currentTime = match != null ? match.getElapsedSeconds() : 0.0;
        var triggeredCombo = comboDetector.recordCardPlay(card, currentTime);
        updateComboCounter();
        
        // Apply combo effects + Spell Synergy refund immediately on trigger
        if (triggeredCombo != null && match != null && match.getArena() != null) {
            ComboEffectApplier.apply(
                    triggeredCombo,
                    match.getArena(),
                    kuroyale.domain.TowerOwner.PLAYER,
                    card,
                    deployedUnit,
                    player);
            if (triggeredCombo == kuroyale.domain.ComboType.SPELL_SYNERGY) {
                GameEventLogger.log("ELIXIR_REFUND amount=1 reason=SPELL_SYNERGY");
                updateElixirHud(); // immediate bar update for refund
            }
        }

        cycleCard(selectedHandIndex);
        showStatus(card.getName() + " deployed at (" + tile.getX() + ", " + tile.getY() + ").", false);
        selectedHandIndex = -1;
        refreshDeckSection();
        Arena arena = match.getArena();
        if (arena != null) {
            arenaBoard.render(arena);
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
        // Run at ~30 FPS for smooth UI updates
        double tickDuration = 0.033;
        matchTicker = new Timeline(new KeyFrame(Duration.seconds(tickDuration), e -> {
            if (!paused) {
                if (controller != null) {
                    controller.advanceTime(tickDuration);
                } else {
                    match.advanceTime(tickDuration);
                }
                replayRecorder.capture(match);
                comboDetector.updateTime(match != null ? match.getElapsedSeconds() : 0.0);
                updateElixirHud();
                updateClockHud();
                Arena arena = match.getArena();
                if (arena != null) {
                    arenaBoard.render(arena);
                    detectTowerDestruction(arena);
                }
            }
            if (match.isFinished()) {
                stopTicker();
                showGameOverOverlay();
                recordMatchOnce();
            }
        }));
        matchTicker.setCycleCount(Timeline.INDEFINITE);
        matchTicker.play();
    }

    private void recordMatchOnce() {
        if (matchRecorded) {
            return;
        }
        matchRecorded = true;
        if (navigator != null && match != null) {
            try {
                navigator.recordMatchWithReplay(match, "AI", selectedLayout, replayRecorder.build());
            } catch (Exception e) {
                // Never let persistence failures break the match end UI.
                System.err.println("Failed to record match: " + e.getMessage());
            }
        }
    }

    private void showGameOverOverlay() {
        if (match == null) {
            return;
        }
        MatchOutcome outcome = match.getOutcome();
        int playerCrowns = outcome != null ? outcome.getPlayerCrowns() : 0;
        int opponentCrowns = outcome != null ? outcome.getOpponentCrowns() : 0;

        String headline = "Draw";
        if (outcome != null && outcome.getWinner() != null) {
            headline = outcome.getWinner() == kuroyale.domain.TowerOwner.PLAYER ? "Victory" : "Defeat";
        }

        String topName = (match.getOpponent() != null && match.getOpponent().getName() != null)
                ? match.getOpponent().getName()
                : "Opponent";
        String bottomName = (player != null && player.getName() != null) ? player.getName() : "You";

        int comboCount = comboDetector.getTriggeredComboCount(); // unique combos triggered in match
        int comboGold = comboCount * 10;
        // Persist rewards to player profile
        try {
            PlayerProfileRepository repo = new PlayerProfileRepository();
            var profile = repo.load();
            profile.addGold(comboGold);
            repo.save(profile);
        } catch (Exception e) {
            System.err.println("Failed to award combo gold: " + e.getMessage());
        }
        GameEventLogger.log("MATCH_END outcome=" + headline + " combos=" + comboCount + " comboGold=" + comboGold);

        MatchEndOverlay.show(
                overlayLayer,
                topName,
                opponentCrowns,
                bottomName,
                playerCrowns,
                headline,
                () -> {
                    // Play Again -> go back to match preview so user can start another match
                    // quickly.
                    if (navigator != null) {
                        navigator.showStartGameScreen();
                    }
                },
                () -> {
                    if (navigator != null) {
                        navigator.showWelcomeScreen();
                    }
                },
                comboCount,
                comboGold);
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
            elixirFill.setWidth(0);
            return;
        }

        double preciseElixir = player.getCurrentElixir();
        if (match != null) {
            preciseElixir += match.getPlayerElixirFraction();
        }

        elixirValueLabel.setText(String.valueOf((int) preciseElixir));
        double ratio = player.getMaxElixir() == 0 ? 0 : preciseElixir / player.getMaxElixir();
        ratio = Math.min(1.0, Math.max(0.0, ratio));

        double width = ELIXIR_BAR_WIDTH * ratio;
        elixirFill.setWidth(width); // Changed from setPrefWidth/setMaxWidth

        updateDeckLoadingState(preciseElixir);
    }

    private void updateDeckLoadingState(double currentElixir) {
        if (deckSectionContainer.getChildren().isEmpty())
            return;

        var rowNode = deckSectionContainer.getChildren().get(0);
        if (!(rowNode instanceof HBox))
            return;
        HBox deckRow = (HBox) rowNode; // nextColumn(0), handRow(1)

        if (deckRow.getChildren().size() < 2)
            return;
        var handRowNode = deckRow.getChildren().get(1);
        if (!(handRowNode instanceof HBox))
            return;
        HBox handRow = (HBox) handRowNode;

        for (int i = 0; i < handRow.getChildren().size(); i++) {
            var child = handRow.getChildren().get(i);
            if (child instanceof StackPane && i < handCards.size()) {
                Card card = handCards.get(i);
                if (card != null) {
                    double cost = card.getElixirCost();
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

    private String formatTime(double remainingSeconds) {
        int totalSeconds = (int) Math.ceil(remainingSeconds);
        totalSeconds = Math.max(0, totalSeconds);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String formatPhase(ElixirPhase phase) {
        if (phase == ElixirPhase.TRIPLE) {
            return "Triple Elixir";
        }
        return phase == ElixirPhase.DOUBLE ? "Double Elixir" : "Normal Elixir";
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

    private void showComboMessage(String comboName, String effectDescription) {
        // Stop any existing combo message timer
        if (comboMessageTimer != null) {
            comboMessageTimer.stop();
        }

        // Don't show combo message if paused (pause overlay takes priority)
        if (paused) {
            return;
        }

        // Create combo message overlay
        StackPane comboOverlay = new StackPane();
        comboOverlay.setMouseTransparent(true);

        VBox comboBox = new VBox(8);
        comboBox.setAlignment(Pos.CENTER);
        comboBox.setPadding(new Insets(20));

        Label comboLabel = new Label("COMBO!");
        comboLabel.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        comboLabel.setTextFill(Color.web("#ffd54f"));

        Label nameLabel = new Label(comboName);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        nameLabel.setTextFill(Color.web("#9be564"));

        Label effectLabel = new Label(effectDescription);
        effectLabel.setFont(Font.font("Arial", 16));
        effectLabel.setTextFill(Color.web("#cbd0d6"));

        comboBox.getChildren().addAll(comboLabel, nameLabel, effectLabel);
        comboOverlay.getChildren().add(comboBox);
        StackPane.setAlignment(comboBox, Pos.CENTER);

        // Add to overlay layer (temporarily)
        overlayLayer.getChildren().add(comboOverlay);
        if (!paused) {
            overlayLayer.setVisible(true);
        }

        // Remove after 2 seconds
        comboMessageTimer = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            overlayLayer.getChildren().remove(comboOverlay);
            // Only hide overlay if there are no other overlays and not paused
            if (overlayLayer.getChildren().isEmpty() && !paused) {
                overlayLayer.setVisible(false);
            }
        }));
        comboMessageTimer.play();
    }

    private void handleEmoteSelected(EmoteType type) {
        if (type == null) {
            return;
        }
        long now = System.currentTimeMillis();
        var result = playerEmoteLimiter.tryConsume(now);
        if (!result.isAllowed()) {
            showStatus("Emote blocked: " + result.getReason(), true);
            return;
        }
        emoteBubbles.showForBottom(type);
        EmoteSound.play();
    }

    private void updateComboCounter() {
        if (comboCountLabel != null) {
            int count = comboDetector.getTriggeredComboCount();
            comboCountLabel.setText("Combos: " + count);
        }
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
                ? playerCrownPill
                : opponentCrownPill;
        StackPane targetIcon = destroyedTower.getOwner() == kuroyale.domain.TowerOwner.OPPONENT
                ? playerCrownTarget
                : opponentCrownTarget;

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
                playerCrowns = Math.min(3, playerCrowns + 1);
                playerCrownLabel.setText(String.valueOf(playerCrowns));
            } else {
                opponentCrowns = Math.min(3, opponentCrowns + 1);
                opponentCrownLabel.setText(String.valueOf(opponentCrowns));
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

}
