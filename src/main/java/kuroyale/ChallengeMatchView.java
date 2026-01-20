package kuroyale;

import application.MatchController;
import application.MatchService;
import application.challenge.ChallengeService;
import application.challenge.ChallengeSession;
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
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import kuroyale.domain.Arena;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.CardType;
import kuroyale.domain.ElixirPhase;
import kuroyale.domain.Match;
import kuroyale.domain.Player;
import kuroyale.domain.Position;
import kuroyale.support.Result;
import kuroyale.emote.EmoteBubbleManager;
import kuroyale.emote.EmoteLimiter;
import kuroyale.emote.EmotePanel;
import kuroyale.emote.EmoteSound;
import kuroyale.emote.EmoteType;

/**
 * Challenge match UI (Phase 2 Feature 4).
 * Similar to StartGameView but adds challenge rules and star/reward overlay on
 * completion.
 */
public class ChallengeMatchView {
    private final ScreenNavigator navigator;
    private final ChallengeService challengeService;
    private final MatchController controller;
    private final ChallengeSession session;
    private final Match match;
    private final Player player;
    private final ArenaBoard arenaBoard;
    private final BorderPane root;

    private final Label timerLabel = new Label("—");
    private final Label phaseLabel = new Label("—");
    private final Label statusLabel = new Label();
    private Timeline ticker;
    private boolean completedHandled = false;
    private final StackPane overlayLayer = new StackPane();
    private final Pane emoteLayer = new Pane();
    private final EmotePanel emotePanel;
    private final StackPane emotePanelLayer = new StackPane();
    private final EmoteLimiter emoteLimiter = EmoteLimiter.defaultLimiter();
    private final EmoteBubbleManager emoteBubbles;

    private final StartGameDeckUi deckUi;

    public ChallengeMatchView(ScreenNavigator navigator,
            ChallengeService challengeService,
            MatchService matchService,
            ChallengeSession session,
            ArenaLayout layout) {
        this.navigator = navigator;
        this.challengeService = challengeService;
        this.session = session;
        this.match = session != null ? session.getMatch() : null;
        this.controller = new MatchController(matchService, match);
        this.player = controller.getPlayer();

        root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #0f1216;");

        String challengeId = session != null ? session.getChallengeId() : "";
        var def = challengeService != null ? challengeService.getDefinition(challengeId) : null;
        String challengeName = def != null ? def.getName() : ("Challenge " + challengeId);
        Label title = new Label(challengeName);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#f5f5f5"));

        String rulesText = def != null ? def.getRulesSummary() : "Complete the challenge and earn stars + gold.";
        Label rules = new Label(rulesText);
        rules.setTextFill(Color.web("#cbd0d6"));
        rules.setWrapText(true);

        styleMetric(timerLabel);
        styleMetric(phaseLabel);
        HBox metrics = new HBox(14,
                metricPill("Phase", phaseLabel),
                metricPill("Time", timerLabel));
        metrics.setAlignment(Pos.CENTER_RIGHT);

        VBox header = new VBox(6, title, rules, metrics);
        header.setPadding(new Insets(10, 10, 15, 10));
        root.setTop(header);

        arenaBoard = new ArenaBoard(layout);
        arenaBoard.setOnTileSelected(this::handleTileSelection);
        overlayLayer.setVisible(false);
        overlayLayer.setMouseTransparent(true);
        emoteLayer.setMouseTransparent(true);
        emoteLayer.prefWidthProperty().bind(root.widthProperty());
        emoteLayer.prefHeightProperty().bind(root.heightProperty());
        root.setCenter(new StackPane(arenaBoard.getView(), emoteLayer, overlayLayer));

        deckUi = new StartGameDeckUi(player != null ? player.getDeck() : null);
        emotePanel = new EmotePanel(this::handleEmoteSelected);
        emotePanelLayer.getChildren().add(emotePanel.getView());
        StackPane.setAlignment(emotePanel.getView(), Pos.BOTTOM_RIGHT);
        StackPane.setMargin(emotePanel.getView(), new Insets(0, 24, 120, 0));
        root.getChildren().add(emotePanelLayer);
        emoteBubbles = new EmoteBubbleManager(emoteLayer);

        Button emoteButton = new Button("Emotes");
        emoteButton.setOnAction(e -> emotePanel.toggle());
        HBox controls = new HBox(10, emoteButton);
        controls.setAlignment(Pos.CENTER_RIGHT);

        VBox hud = new VBox(10, deckUi.getView(), statusLabel, controls);
        hud.setPadding(new Insets(12));
        hud.setStyle("-fx-background-color: #181b22; -fx-border-color: #2d2f36; -fx-border-width: 2 0 0 0;");
        root.setBottom(hud);

        statusLabel.setTextFill(Color.web("#d2d7e5"));
        statusLabel.setWrapText(true);
        statusLabel.setText("Select a card and click to deploy.");

        updateHud();
        startTicker();
    }

    private void handleTileSelection(Position tile) {
        if (match == null || player == null) {
            showStatus("Match is not ready.", true);
            return;
        }
        var selected = deckUi.getSelectedCard();
        if (selected == null) {
            showStatus("Select a card first.", true);
            return;
        }
        Result<?> result = controller.deployCard(player, selected, tile);
        if (!result.isSuccess()) {
            showStatus(result.getMessage(), true);
            return;
        }
        
        // Record spell cast for visual effects
        if (selected.getType() == CardType.SPELL) {
            arenaBoard.recordSpellCast(selected.getId(), tile);
        } else {
            arenaBoard.recordDeployEffect(kuroyale.domain.TowerOwner.PLAYER, tile);
        }
        
        deckUi.onCardPlayed();
        showStatus("Deployed " + selected.getName() + ".", false);
        renderArena();
        updateHud();
    }

    private void startTicker() {
        if (match == null) {
            return;
        }
        ticker = new Timeline(new KeyFrame(Duration.seconds(0.5), e -> {
            controller.advanceTime(0.5);
            updateHud();
            renderArena();
            if (match.isOver() && !completedHandled) {
                completedHandled = true;
                showCompletionOverlay();
            }
        }));
        ticker.setCycleCount(Timeline.INDEFINITE);
        ticker.play();
    }

    private void stopTicker() {
        if (ticker != null) {
            ticker.stop();
            ticker = null;
        }
    }

    private void renderArena() {
        Arena arena = match != null ? match.getArena() : null;
        if (arena != null) {
            arenaBoard.renderUnits(arena.getUnits());
        }
    }

    private void updateHud() {
        if (match == null) {
            timerLabel.setText("—");
            phaseLabel.setText("—");
            return;
        }
        timerLabel.setText(formatTime(match.getRemainingSeconds()));
        ElixirPhase phase = match.getCurrentElixirPhase();
        if (phase == ElixirPhase.TRIPLE) {
            phaseLabel.setText("Triple");
        } else if (phase == ElixirPhase.DOUBLE) {
            phaseLabel.setText("Double");
        } else {
            phaseLabel.setText("Normal");
        }
        deckUi.refresh();
    }

    private void showCompletionOverlay() {
        stopTicker();
        var completion = challengeService.completeChallenge(session);
        String message;
        String starsText = "Stars: 0/3";
        String goldText = "Gold: +0";
        if (completion.isSuccess() && completion.getData() != null) {
            var data = completion.getData();
            message = data.isWin() ? "Challenge Completed!" : "Challenge Failed";
            starsText = "Stars: " + data.getStars() + "/3";
            goldText = "Gold: +" + data.getGoldAwarded();
        } else {
            message = "Challenge Ended";
        }

        overlayLayer.setVisible(true);
        overlayLayer.setMouseTransparent(false);
        overlayLayer.getChildren().clear();

        Rectangle dim = new Rectangle();
        dim.widthProperty().bind(overlayLayer.widthProperty());
        dim.heightProperty().bind(overlayLayer.heightProperty());
        dim.setFill(Color.color(0, 0, 0, 0.65));

        Label headline = new Label(message);
        headline.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        headline.setTextFill(Color.WHITE);

        Label stars = new Label(starsText);
        stars.setTextFill(Color.web("#ffd54f"));
        stars.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Label gold = new Label(goldText);
        gold.setTextFill(Color.web("#cbd0d6"));

        Button back = new Button("Back to Challenges");
        back.setOnAction(e -> {
            overlayLayer.getChildren().clear();
            overlayLayer.setVisible(false);
            overlayLayer.setMouseTransparent(true);
            navigator.showChallengeModeScreen();
        });

        VBox box = new VBox(14, headline, stars, gold, back);
        box.setAlignment(Pos.CENTER);

        overlayLayer.getChildren().addAll(dim, box);
        StackPane.setAlignment(box, Pos.CENTER);
    }

    private void showStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setTextFill(error ? Color.web("#f05a5b") : Color.web("#9be564"));
    }

    private void handleEmoteSelected(EmoteType type) {
        if (type == null) {
            return;
        }
        long now = System.currentTimeMillis();
        var result = emoteLimiter.tryConsume(now);
        if (!result.isAllowed()) {
            showStatus("Emote blocked: " + result.getReason(), true);
            return;
        }
        emoteBubbles.showForBottom(type);
        EmoteSound.play();
    }

    private void styleMetric(Label l) {
        l.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        l.setTextFill(Color.WHITE);
    }

    private VBox metricPill(String caption, Label value) {
        Label cap = new Label(caption.toUpperCase());
        cap.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        cap.setTextFill(Color.web("#8f94a3"));
        VBox pill = new VBox(2, cap, value);
        pill.setAlignment(Pos.CENTER_LEFT);
        pill.setPadding(new Insets(8, 12, 8, 12));
        pill.setStyle("-fx-background-color: #1c1f2a; -fx-background-radius: 8;");
        return pill;
    }

    private String formatTime(double remainingSeconds) {
        int total = (int) Math.ceil(Math.max(0, remainingSeconds));
        int min = total / 60;
        int sec = total % 60;
        return String.format("%02d:%02d", min, sec);
    }

    public Parent getRoot() {
        return root;
    }

    /**
     * Minimal deck UI for challenge matches: 4-card hand + next.
     */
    private static final class StartGameDeckUi {
        private final VBox view = new VBox(6);
        private final java.util.Deque<kuroyale.domain.Card> drawPile = new java.util.ArrayDeque<>();
        private final java.util.List<kuroyale.domain.Card> hand = new java.util.ArrayList<>();
        private kuroyale.domain.Card next;
        private int selectedIndex = -1;
        private final java.util.List<StackPane> slotViews = new java.util.ArrayList<>();

        private StartGameDeckUi(kuroyale.domain.Deck deck) {
            if (deck != null) {
                drawPile.addAll(deck.getCards());
            }
            for (int i = 0; i < 4; i++) {
                hand.add(drawPile.pollFirst());
            }
            next = drawPile.peekFirst();
            refresh();
        }

        Parent getView() {
            return view;
        }

        kuroyale.domain.Card getSelectedCard() {
            if (selectedIndex < 0 || selectedIndex >= hand.size()) {
                return null;
            }
            return hand.get(selectedIndex);
        }

        void onCardPlayed() {
            if (selectedIndex < 0 || selectedIndex >= hand.size()) {
                return;
            }
            var played = hand.get(selectedIndex);
            if (played != null) {
                drawPile.addLast(played);
            }
            hand.set(selectedIndex, drawPile.pollFirst());
            next = drawPile.peekFirst();
            selectedIndex = -1;
            refresh();
        }

        void refresh() {
            view.getChildren().clear();
            slotViews.clear();
            HBox row = new HBox(8);
            VBox nextCol = new VBox(4);
            nextCol.setAlignment(Pos.CENTER);
            Label nextLabel = new Label("Next");
            nextLabel.setTextFill(Color.web("#8f94a3"));
            nextCol.getChildren().addAll(nextLabel, StartGameUiBits.createCardSlot(next, false, false));
            row.getChildren().add(nextCol);

            for (int i = 0; i < 4; i++) {
                var c = i < hand.size() ? hand.get(i) : null;
                StackPane slot = StartGameUiBits.createCardSlot(c, true, i == selectedIndex);
                final int idx = i;
                slot.setOnMouseClicked(e -> {
                    if (idx >= 0 && idx < hand.size() && hand.get(idx) != null) {
                        selectedIndex = idx;
                        Platform.runLater(this::refresh);
                    }
                });
                slotViews.add(slot);
                row.getChildren().add(slot);
            }
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().add(spacer);
            view.getChildren().add(row);
        }
    }
}
