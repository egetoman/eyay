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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import kuroyale.domain.MatchRecord;
import kuroyale.domain.MatchStats;

public class MatchHistoryView {

    private final BorderPane root;
    private final MatchHistoryService historyService;
    private final VBox historyPane = new VBox(10);
    private final VBox statsPane = new VBox(15);

    public MatchHistoryView(ScreenNavigator navigator, MatchHistoryService historyService) {
        this.historyService = historyService;
        
        root = new BorderPane();
        root.setPadding(new Insets(20));

        Label title = new Label("Match History & Stats");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        root.setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(0, 0, 20, 0));

        // Create tabs for History and Stats
        Tab historyTab = new Tab("History");
        historyTab.setClosable(false);
        ScrollPane historyScroll = new ScrollPane(historyPane);
        historyScroll.setFitToWidth(true);
        historyScroll.setFitToHeight(true);
        historyScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        historyTab.setContent(historyScroll);

        Tab statsTab = new Tab("Stats");
        statsTab.setClosable(false);
        ScrollPane statsScroll = new ScrollPane(statsPane);
        statsScroll.setFitToWidth(true);
        statsScroll.setFitToHeight(true);
        statsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        statsTab.setContent(statsScroll);

        TabPane tabPane = new TabPane(historyTab, statsTab);
        root.setCenter(tabPane);

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> navigator.showWelcomeScreen());
        HBox bottomBar = new HBox(backButton);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(10));
        root.setBottom(bottomBar);

        refreshHistory();
        refreshStats();
    }

    private void refreshHistory() {
        historyPane.getChildren().clear();
        java.util.List<MatchRecord> records = historyService.getMatchHistory();
        
        if (records.isEmpty()) {
            Label noMatchesLabel = new Label("No matches recorded yet");
            noMatchesLabel.setFont(Font.font("Arial", 16));
            historyPane.getChildren().add(noMatchesLabel);
            return;
        }
        
        for (MatchRecord record : records) {
            historyPane.getChildren().add(createMatchRecordTile(record));
        }
    }

    private VBox createMatchRecordTile(MatchRecord record) {
        Label dateLabel = new Label(formatDateTime(record.getDateTime()));
        dateLabel.setFont(Font.font("Arial", 12));
        dateLabel.setTextFill(Color.GRAY);
        
        Label opponentLabel = new Label("Opponent: " + (record.getOpponentType() != null ? record.getOpponentType() : "Unknown"));
        
        Label resultLabel = new Label("Result: " + (record.getResult() != null ? record.getResult() : "Unknown"));
        resultLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        if ("Win".equalsIgnoreCase(record.getResult())) {
            resultLabel.setTextFill(Color.GREEN);
        } else if ("Loss".equalsIgnoreCase(record.getResult())) {
            resultLabel.setTextFill(Color.RED);
        }
        
        Label crownsLabel = new Label("Crowns: " + record.getCrowns());
        Label goldLabel = new Label("Gold: " + (record.getGoldChange() >= 0 ? "+" : "") + record.getGoldChange());
        goldLabel.setTextFill(record.getGoldChange() >= 0 ? Color.GOLD : Color.RED);
        
        Label arenaLabel = new Label("Arena: " + (record.getArenaName() != null ? record.getArenaName() : "Unknown"));
        
        VBox tile = new VBox(5, dateLabel, opponentLabel, resultLabel, crownsLabel, goldLabel, arenaLabel);
        tile.setPadding(new Insets(15));
        tile.setStyle("-fx-border-color: #2d2f36; -fx-border-radius: 6; -fx-background-color: #1c1f26;");
        tile.setPrefWidth(600);
        return tile;
    }

    private void refreshStats() {
        statsPane.getChildren().clear();
        MatchStats stats = historyService.getMatchStats();
        
        if (stats.getTotalMatches() == 0) {
            Label noStatsLabel = new Label("No statistics available yet");
            noStatsLabel.setFont(Font.font("Arial", 16));
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
        
        statsPane.getChildren().addAll(
            totalMatchesLabel, winsLabel, lossesLabel, winRateLabel,
            totalCrownsLabel, totalGoldLabel, avgCrownsLabel
        );
        statsPane.setPadding(new Insets(20));
        statsPane.setAlignment(Pos.TOP_LEFT);
    }

    private Label createStatLabel(String label, String value) {
        Label statLabel = new Label(label + ": " + value);
        statLabel.setFont(Font.font("Arial", 16));
        statLabel.setPadding(new Insets(5));
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

