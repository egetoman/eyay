package kuroyale;

import application.QuestService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import kuroyale.domain.DailyQuestSet;
import kuroyale.domain.Quest;
import kuroyale.domain.QuestProgress;
import kuroyale.domain.QuestStatus;

public class DailyQuestView {

    private final BorderPane root;
    private final QuestService questService;
    private final VBox questsPane = new VBox(15);
    private final Label resetLabel = new Label();

    public DailyQuestView(ScreenNavigator navigator, QuestService questService) {
        this.questService = questService;
        
        root = new BorderPane();
        root.setPadding(new Insets(20));

        Label title = new Label("Daily Quests");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));

        resetLabel.setTextFill(Color.web("#8f94a3"));
        resetLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        VBox header = new VBox(6, title, resetLabel);
        header.setAlignment(Pos.CENTER);
        root.setTop(header);
        BorderPane.setAlignment(header, Pos.CENTER);
        BorderPane.setMargin(header, new Insets(0, 0, 20, 0));

        questsPane.setPadding(new Insets(10));
        questsPane.setAlignment(Pos.TOP_CENTER);

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(questsPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        root.setCenter(scrollPane);

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> navigator.showWelcomeScreen());
        HBox bottomBar = new HBox(backButton);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(10));
        root.setBottom(bottomBar);

        refreshQuests();
    }

    private void refreshQuests() {
        questsPane.getChildren().clear();
        updateResetCountdown();
        DailyQuestSet questSet = questService.getTodayQuests();
        
        if (questSet == null || questSet.getQuests().isEmpty()) {
            Label noQuestsLabel = new Label("No quests available");
            questsPane.getChildren().add(noQuestsLabel);
            return;
        }
        
        for (Quest quest : questSet.getQuests()) {
            QuestProgress progress = questSet.getProgressForQuest(quest.getId());
            if (progress == null) {
                progress = new QuestProgress(quest.getId(), 0, QuestStatus.IN_PROGRESS);
            }
            questsPane.getChildren().add(createQuestTile(quest, progress));
        }
    }

    private void updateResetCountdown() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime nextReset = now.toLocalDate().plusDays(1).atStartOfDay();
        java.time.Duration remaining = java.time.Duration.between(now, nextReset);
        long hours = Math.max(0, remaining.toHours());
        long minutes = Math.max(0, remaining.minusHours(hours).toMinutes());
        resetLabel.setText(String.format("Resets in %02d:%02d", hours, minutes));
    }

    private VBox createQuestTile(Quest quest, QuestProgress progress) {
        Label description = new Label(quest.getDescription());
        description.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        double progressRatio = quest.getTargetValue() > 0 
            ? Math.min(1.0, (double) progress.getCurrentProgress() / quest.getTargetValue())
            : 0.0;
        
        ProgressBar progressBar = new ProgressBar(progressRatio);
        progressBar.setPrefWidth(400);
        
        Label progressText = new Label(progress.getCurrentProgress() + " / " + quest.getTargetValue());
        progressText.setFont(Font.font("Arial", 12));
        
        Label rewardLabel = new Label("Reward: " + quest.getRewardGold() + " gold");
        rewardLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        rewardLabel.setTextFill(Color.GOLD);
        
        Button claimButton = new Button("Claim Reward");
        claimButton.setDisable(!progress.isCompleted() || progress.isClaimed());
        
        if (progress.isClaimed()) {
            claimButton.setText("Claimed");
            claimButton.setDisable(true);
        } else if (progress.isCompleted()) {
            claimButton.setOnAction(e -> claimReward(quest.getId()));
        } else {
            claimButton.setText("Not Completed");
        }
        
        Label statusLabel = new Label();
        if (progress.isClaimed()) {
            statusLabel.setText("Status: Claimed");
            statusLabel.setTextFill(Color.GREEN);
        } else if (progress.isCompleted()) {
            statusLabel.setText("Status: Completed - Ready to Claim!");
            statusLabel.setTextFill(Color.LIGHTGREEN);
        } else {
            statusLabel.setText("Status: In Progress");
            statusLabel.setTextFill(Color.ORANGE);
        }
        
        VBox tile = new VBox(10, description, progressBar, progressText, rewardLabel, statusLabel, claimButton);
        tile.setPadding(new Insets(15));
        tile.setStyle("-fx-border-color: #2d2f36; -fx-border-radius: 6; -fx-background-color: #1c1f26;");
        tile.setPrefWidth(500);
        return tile;
    }

    private void claimReward(String questId) {
        var result = questService.claimReward(questId);
        
        if (result.isSuccess()) {
            Alert alert = new Alert(AlertType.INFORMATION, 
                "Reward claimed! You received " + result.getData() + " gold.");
            if (root.getScene() != null) {
                alert.initOwner(root.getScene().getWindow());
            }
            alert.showAndWait();
            refreshQuests();
        } else {
            Alert alert = new Alert(AlertType.ERROR, result.getMessage());
            if (root.getScene() != null) {
                alert.initOwner(root.getScene().getWindow());
            }
            alert.showAndWait();
        }
    }

    public Parent getRoot() {
        return root;
    }
}

