package kuroyale;

import application.ArenaLayoutService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.Card;
import kuroyale.domain.MatchRecord;
import kuroyale.domain.MatchReplay;
import kuroyale.domain.Position;
import kuroyale.domain.ReplayFrame;
import kuroyale.domain.Tower;
import kuroyale.domain.TowerOwner;
import kuroyale.domain.Unit;
import kuroyale.infrastructure.CardCatalogRepository;

/**
 * Bonus: Watch Match Replay screen.
 * Plays back a recorded {@link MatchReplay} snapshot timeline.
 */
public class ReplayView {
    private final BorderPane root;
    private final ArenaLayout layout;
    private final MatchReplay replay;
    private final ArenaBoard arenaBoard;
    private final Label phaseLabel = new Label("—");
    private final Label timeLabel = new Label("—");
    private final Label elixirLabel = new Label("—");
    private final Label statusLabel = new Label();
    private Timeline ticker;
    private int frameIndex = 0;
    private double speed = 1.0;

    private final Map<String, Card> cardById = new HashMap<>();
    private final Map<String, Integer> originalTowerHp = new HashMap<>(); // Store original HP to restore on exit

    public ReplayView(ScreenNavigator navigator, ArenaLayoutService arenaLayoutService, MatchRecord record) {
        root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #0f1216;");

        if (record == null || record.getReplay() == null || !record.hasReplay()) {
            replay = null;
            layout = arenaLayoutService != null ? arenaLayoutService.getActiveLayout() : kuroyale.domain.ArenaLayout.defaultLayout();
            arenaBoard = new ArenaBoard(layout);
            root.setCenter(arenaBoard.getView());
            statusLabel.setTextFill(Color.web("#f05a5b"));
            statusLabel.setText("Replay is missing or empty.");
            root.setBottom(new VBox(10, statusLabel, buildBottomButtons(navigator)));
            return;
        }

        this.replay = record.getReplay();
        ArenaLayout resolved = null;
        if (arenaLayoutService != null && record.getArenaLayoutId() != null) {
            resolved = arenaLayoutService.findById(record.getArenaLayoutId()).orElse(null);
        }
        if (resolved == null) {
            resolved = arenaLayoutService != null ? arenaLayoutService.getActiveLayout() : kuroyale.domain.ArenaLayout.defaultLayout();
        }
        this.layout = resolved;

        for (Card c : new CardCatalogRepository().findAll()) {
            if (c != null && c.getId() != null) {
                cardById.put(c.getId(), c);
            }
        }
        
        // Save original tower HP values to restore when exiting replay
        for (Tower t : this.layout.getTowers()) {
            if (t != null && t.getPosition() != null && t.getOwner() != null && t.getType() != null) {
                String key = buildTowerKey(t);
                originalTowerHp.put(key, t.getHp());
            }
        }

        Label title = new Label("Replay");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#f5f5f5"));

        styleMetric(phaseLabel);
        styleMetric(timeLabel);
        styleMetric(elixirLabel);
        HBox metrics = new HBox(14,
            pill("Phase", phaseLabel),
            pill("Time", timeLabel),
            pill("Elixir", elixirLabel)
        );
        metrics.setAlignment(Pos.CENTER_RIGHT);

        VBox header = new VBox(6, title, metrics);
        header.setPadding(new Insets(10, 10, 15, 10));
        root.setTop(header);

        arenaBoard = new ArenaBoard(layout);
        root.setCenter(arenaBoard.getView());

        statusLabel.setTextFill(Color.web("#cbd0d6"));
        statusLabel.setWrapText(true);

        VBox bottom = new VBox(10, buildControls(), statusLabel, buildBottomButtons(navigator));
        bottom.setPadding(new Insets(12));
        bottom.setStyle("-fx-background-color: #181b22; -fx-border-color: #2d2f36; -fx-border-width: 2 0 0 0;");
        root.setBottom(bottom);

        applyFrameSafe(0);
        start();
    }

    private static final double[] SPEED_OPTIONS = {1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0};
    private int speedIndex = 0;

    private Parent buildControls() {
        Button playPause = new Button("Pause");
        Button speedBtn = new Button("Speed: 1x");

        playPause.setOnAction(e -> {
            if (ticker == null) {
                start();
                playPause.setText("Pause");
            } else {
                stop();
                playPause.setText("Play");
            }
        });

        speedBtn.setOnAction(e -> {
            // Cycle through speed options: 1x -> 2x -> 4x -> 8x -> 16x -> 1x
            speedIndex = (speedIndex + 1) % SPEED_OPTIONS.length;
            speed = SPEED_OPTIONS[speedIndex];
            speedBtn.setText("Speed: " + formatSpeed(speed));
            if (ticker != null) {
                stop();
                start();
            }
        });

        Button restart = new Button("Restart");
        restart.setOnAction(e -> {
            frameIndex = 0;
            applyFrameSafe(frameIndex);
        });

        HBox row = new HBox(10, playPause, speedBtn, restart);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private String formatSpeed(double spd) {
        if (spd == (int) spd) {
            return (int) spd + "x";
        }
        return spd + "x";
    }

    private Parent buildBottomButtons(ScreenNavigator navigator) {
        Button back = new Button("Back");
        back.setOnAction(e -> {
            cleanup(); // Restore tower HP before leaving
            navigator.showMatchHistoryScreen();
        });
        HBox bar = new HBox(back);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void start() {
        if (replay == null) {
            return;
        }
        int tick = replay.getTickMillis() > 0 ? replay.getTickMillis() : 500;
        double scaledMillis = tick / speed;
        ticker = new Timeline(new KeyFrame(Duration.millis(scaledMillis), e -> {
            frameIndex++;
            if (!applyFrameSafe(frameIndex)) {
                stop();
                statusLabel.setText("Replay finished.");
            }
        }));
        ticker.setCycleCount(Timeline.INDEFINITE);
        ticker.play();
    }

    private void stop() {
        if (ticker != null) {
            ticker.stop();
            ticker = null;
        }
    }

    private boolean applyFrameSafe(int index) {
        if (replay == null) {
            return false;
        }
        List<ReplayFrame> frames = replay.getFrames();
        if (frames == null || index < 0 || index >= frames.size()) {
            return false;
        }
        ReplayFrame f = frames.get(index);
        if (f == null) {
            return false;
        }
        phaseLabel.setText(f.getPhase() != null ? f.getPhase() : "—");
        timeLabel.setText(formatTime(f.getRemainingSeconds()));
        elixirLabel.setText("P1 " + f.getPlayerElixir() + " / P2 " + f.getOpponentElixir());

        // Update tower HP on layout towers
        for (ReplayFrame.TowerSnapshot ts : f.getTowers()) {
            for (Tower t : layout.getTowers()) {
                if (t == null || t.getPosition() == null || t.getOwner() == null || t.getType() == null) {
                    continue;
                }
                int ownerId = t.getOwner() == TowerOwner.PLAYER ? 1 : 2;
                if (ownerId == ts.owner
                    && t.getType().name().equals(ts.type)
                    && t.getPosition().getX() == ts.x
                    && t.getPosition().getY() == ts.y) {
                    t.setHp(ts.hp);
                }
            }
        }

        List<Unit> units = new ArrayList<>();
        for (ReplayFrame.UnitSnapshot us : f.getUnits()) {
            Card card = us.cardId != null ? cardById.get(us.cardId) : null;
            if (card == null) {
                continue;
            }
            TowerOwner owner = us.owner == 1 ? TowerOwner.PLAYER : TowerOwner.OPPONENT;
            Unit u = new Unit(card, new Position((int) Math.round(us.x), (int) Math.round(us.y)), us.hp, owner);
            u.setPrecisePosition(us.x, us.y);
            units.add(u);
        }
        arenaBoard.renderUnits(units);

        if (f.isFinished()) {
            stop();
            statusLabel.setText("Replay finished.");
        } else {
            statusLabel.setText("Frame " + (index + 1) + "/" + frames.size());
        }
        return true;
    }

    private VBox pill(String caption, Label value) {
        Label cap = new Label(caption.toUpperCase());
        cap.setTextFill(Color.web("#8f94a3"));
        cap.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        VBox v = new VBox(2, cap, value);
        v.setPadding(new Insets(8, 12, 8, 12));
        v.setStyle("-fx-background-color: #1c1f2a; -fx-background-radius: 8;");
        return v;
    }

    private void styleMetric(Label l) {
        l.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        l.setTextFill(Color.WHITE);
    }

    private String formatTime(double remainingSeconds) {
        int total = (int) Math.max(0, Math.ceil(remainingSeconds));
        int min = total / 60;
        int sec = total % 60;
        return String.format("%02d:%02d", min, sec);
    }

    public Parent getRoot() {
        return root;
    }
    
    /**
     * Restores tower HP to original values. Call this when exiting the replay.
     */
    public void cleanup() {
        stop();
        // Restore original tower HP values
        if (layout != null) {
            for (Tower t : layout.getTowers()) {
                if (t != null && t.getPosition() != null && t.getOwner() != null && t.getType() != null) {
                    String key = buildTowerKey(t);
                    Integer originalHp = originalTowerHp.get(key);
                    if (originalHp != null) {
                        t.setHp(originalHp);
                    }
                }
            }
        }
    }
    
    private String buildTowerKey(Tower tower) {
        return tower.getOwner() + "|" + tower.getType() + "|" + 
               tower.getPosition().getX() + "|" + tower.getPosition().getY();
    }
}


