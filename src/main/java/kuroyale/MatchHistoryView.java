package kuroyale;

import application.MatchHistoryService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import kuroyale.domain.MatchRecord;
import kuroyale.domain.MatchStats;

public class MatchHistoryView {

    private final BorderPane root;
    private final ScreenNavigator navigator;
    private final MatchHistoryService historyService;
    private final VBox historyPane = new VBox(10);
    private final VBox statsPane = new VBox(15);

    public MatchHistoryView(ScreenNavigator navigator, MatchHistoryService historyService) {
        this.navigator = navigator;
        this.historyService = historyService;
        
        root = new BorderPane();
        root.setPadding(new Insets(16)); // UI-only change: spacing
        root.getStyleClass().add("history-root"); // UI-only change: visual hierarchy

        Label title = new Label("Match History & Stats");
        title.getStyleClass().add("history-title");
        Label subtitle = new Label("Review recent battles and performance");
        subtitle.getStyleClass().add("history-subtitle");
        VBox header = new VBox(4, title, subtitle);
        header.getStyleClass().add("history-header"); // UI-only change: visual hierarchy
        root.setTop(header);
        BorderPane.setAlignment(header, Pos.CENTER_LEFT);
        BorderPane.setMargin(header, new Insets(0, 0, 12, 0));

        // Create tabs for History and Stats
        Tab historyTab = new Tab("History");
        historyTab.setClosable(false);
        ScrollPane historyScroll = new ScrollPane(historyPane);
        historyScroll.setFitToWidth(true);
        historyScroll.setFitToHeight(true);
        historyScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        historyScroll.getStyleClass().add("history-scroll");
        historyTab.setContent(historyScroll);

        Tab statsTab = new Tab("Stats");
        statsTab.setClosable(false);
        ScrollPane statsScroll = new ScrollPane(statsPane);
        statsScroll.setFitToWidth(true);
        statsScroll.setFitToHeight(true);
        statsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        statsScroll.getStyleClass().add("history-scroll");
        statsTab.setContent(statsScroll);

        TabPane tabPane = new TabPane(historyTab, statsTab);
        tabPane.getStyleClass().add("history-tabs"); // UI-only change: visual hierarchy
        root.setCenter(tabPane);

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("history-back-button");
        backButton.setOnAction(e -> navigator.showWelcomeScreen());
        HBox bottomBar = new HBox(backButton);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(8, 0, 4, 0)); // UI-only change: spacing
        bottomBar.getStyleClass().add("history-bottom");
        root.setBottom(bottomBar);

        refreshHistory();
        refreshStats();
    }

    private void refreshHistory() {
        historyPane.getChildren().clear();
        java.util.List<MatchRecord> records = historyService.getMatchHistory();
        
        if (records.isEmpty()) {
            Label noMatchesLabel = new Label("No matches recorded yet");
            noMatchesLabel.getStyleClass().add("history-empty");
            historyPane.getChildren().add(noMatchesLabel);
            return;
        }
        
        for (MatchRecord record : records) {
            historyPane.getChildren().add(createMatchRecordTile(record));
        }
    }

    private VBox createMatchRecordTile(MatchRecord record) {
        Label dateLabel = new Label(formatDateTime(record.getDateTime()));
        dateLabel.getStyleClass().add("history-meta");
        
        Label opponentLabel = new Label("Opponent: " + (record.getOpponentType() != null ? record.getOpponentType() : "Unknown"));
        opponentLabel.getStyleClass().add("history-meta");
        
        Label resultLabel = new Label("Result: " + (record.getResult() != null ? record.getResult() : "Unknown"));
        resultLabel.getStyleClass().add("history-result");
        if ("Win".equalsIgnoreCase(record.getResult())) {
            resultLabel.getStyleClass().add("history-result-win");
        } else if ("Loss".equalsIgnoreCase(record.getResult())) {
            resultLabel.getStyleClass().add("history-result-loss");
        }
        
        Label crownsLabel = new Label("Crowns: " + record.getCrowns());
        Label goldLabel = new Label("Gold: " + (record.getGoldChange() >= 0 ? "+" : "") + record.getGoldChange());
        crownsLabel.getStyleClass().add("history-primary");
        goldLabel.getStyleClass().add("history-primary");
        goldLabel.getStyleClass().add(record.getGoldChange() >= 0 ? "history-gold-up" : "history-gold-down");
        
        Label arenaLabel = new Label("Arena: " + (record.getArenaName() != null ? record.getArenaName() : "Unknown"));
        arenaLabel.getStyleClass().add("history-meta");

        Button replayButton = new Button("Watch Replay");
        replayButton.getStyleClass().add("history-replay-button");
        replayButton.setDisable(record == null || !record.hasReplay());
        replayButton.setOnAction(e -> {
            if (record == null || !record.hasReplay()) {
                return;
            }
            navigator.showReplayScreen(record);
        });

        HBox topRow = new HBox(10, resultLabel, dateLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.getStyleClass().add("history-row");

        HBox metaRow = new HBox(12, opponentLabel, arenaLabel);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        metaRow.getStyleClass().add("history-row");

        HBox statsRow = new HBox(12, crownsLabel, goldLabel);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.getStyleClass().add("history-row");

        VBox tile = new VBox(8, topRow, metaRow, statsRow, replayButton);
        tile.setPadding(new Insets(14)); // UI-only change: spacing
        tile.getStyleClass().add("history-card");
        tile.setPrefWidth(600);
        return tile;
    }

    private void refreshStats() {
        statsPane.getChildren().clear();
        MatchStats stats = historyService.getMatchStats();
        
        if (stats.getTotalMatches() == 0) {
            Label noStatsLabel = new Label("No statistics available yet");
            noStatsLabel.getStyleClass().add("history-empty");
            statsPane.getChildren().add(noStatsLabel);
            return;
        }
        
        Label totalMatchesLabel = createStatLabel("Total Matches", String.valueOf(stats.getTotalMatches()));
        Label winsLabel = createStatLabel("Wins", String.valueOf(stats.getWins()));
        Label lossesLabel = createStatLabel("Losses", String.valueOf(stats.getLosses()));
        Label winRateLabel = createStatLabel("Win Rate", String.format("%.1f%%", stats.getWinRate()));
        Label totalCrownsLabel = createStatLabel("Total Crowns", String.valueOf(stats.getTotalCrowns()));
        Label totalGoldLabel = createStatLabel("Total Gold Earned", String.valueOf(stats.getTotalGoldEarned()));
        Label avgCrownsLabel = createStatLabel("Average Crowns per Match", String.format("%.2f", stats.getAverageCrownsPerMatch()));

        winsLabel.getStyleClass().add("history-stat-positive");
        lossesLabel.getStyleClass().add("history-stat-muted");
        winRateLabel.getStyleClass().add("history-stat-accent");

        Label overviewTitle = new Label("Overview");
        overviewTitle.getStyleClass().add("history-section-title");
        FlowPane overviewGrid = new FlowPane(12, 12, totalMatchesLabel, winsLabel, lossesLabel, winRateLabel);
        overviewGrid.getStyleClass().add("history-stat-grid"); // UI-only change: stat grouping

        Label performanceTitle = new Label("Performance");
        performanceTitle.getStyleClass().add("history-section-title");
        FlowPane performanceGrid = new FlowPane(12, 12, totalCrownsLabel, avgCrownsLabel);
        performanceGrid.getStyleClass().add("history-stat-grid");

        Label economyTitle = new Label("Economy");
        economyTitle.getStyleClass().add("history-section-title");
        FlowPane economyGrid = new FlowPane(12, 12, totalGoldLabel);
        economyGrid.getStyleClass().add("history-stat-grid");

        VBox statsContent = new VBox(14, overviewTitle, overviewGrid, performanceTitle, performanceGrid, economyTitle, economyGrid);
        statsContent.getStyleClass().add("history-stats-content"); // UI-only change: visual hierarchy

        statsPane.getChildren().add(statsContent);
        statsPane.setPadding(new Insets(16)); // UI-only change: spacing
        statsPane.setAlignment(Pos.TOP_LEFT);
    }

    private Label createStatLabel(String label, String value) {
        Label title = new Label(label);
        title.getStyleClass().add("history-stat-title");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("history-stat-value");
        HBox row = new HBox(8, title, valueLabel);
        row.getStyleClass().add("history-stat-row"); // UI-only change: visual hierarchy
        Label statLabel = new Label();
        statLabel.setGraphic(row);
        statLabel.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        statLabel.getStyleClass().add("history-stat-tile");
        return statLabel;
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Unknown date";
        }
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public Parent getRoot() {
        return root;
    }
}

