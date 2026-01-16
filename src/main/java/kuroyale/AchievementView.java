package kuroyale;

import application.AchievementService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import kuroyale.domain.Achievement;
import kuroyale.domain.AchievementProgress;

public class AchievementView {

    private final BorderPane root;
    private final AchievementService achievementService;
    private final VBox achievementsPane = new VBox(15);

    public AchievementView(ScreenNavigator navigator, AchievementService achievementService) {
        this.achievementService = achievementService;
        
        root = new BorderPane();
        root.setPadding(new Insets(20));

        Label title = new Label("Achievements");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        root.setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(0, 0, 20, 0));

        achievementsPane.setPadding(new Insets(10));
        achievementsPane.setAlignment(Pos.TOP_CENTER);

        ScrollPane scrollPane = new ScrollPane(achievementsPane);
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

        refreshAchievements();
    }

    private void refreshAchievements() {
        achievementsPane.getChildren().clear();
        java.util.List<Achievement> achievements = achievementService.getAllAchievements();
        java.util.Map<String, AchievementProgress> progressMap = achievementService.getProgress();
        
        for (Achievement achievement : achievements) {
            AchievementProgress progress = progressMap.get(achievement.getId());
            if (progress == null) {
                progress = new AchievementProgress(achievement.getId(), 0, false);
            }
            achievementsPane.getChildren().add(createAchievementTile(achievement, progress));
        }
    }

    private VBox createAchievementTile(Achievement achievement, AchievementProgress progress) {
        boolean unlocked = progress.isUnlocked();
        Label name = new Label(unlocked ? achievement.getName() : "Locked Achievement");
        name.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        
        Label description = new Label(unlocked ? achievement.getDescription() : "???");
        description.setFont(Font.font("Arial", 14));
        
        double progressRatio = achievement.getTargetValue() > 0 
            ? Math.min(1.0, (double) progress.getCurrentProgress() / achievement.getTargetValue())
            : 0.0;
        
        ProgressBar progressBar = new ProgressBar(progressRatio);
        progressBar.setPrefWidth(500);
        
        Label progressText = new Label(progress.getCurrentProgress() + " / " + achievement.getTargetValue());
        progressText.setFont(Font.font("Arial", 12));
        
        Label rewardLabel = new Label("Reward: " + achievement.getRewardGold() + " gold");
        rewardLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        rewardLabel.setTextFill(Color.GOLD);
        
        Label statusLabel = new Label();
        if (unlocked && progress.isClaimed()) {
            statusLabel.setText("✓ Claimed");
            statusLabel.setTextFill(Color.web("#9be564"));
            statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        } else if (unlocked) {
            statusLabel.setText("Unlocked");
            statusLabel.setTextFill(Color.web("#cbd0d6"));
        } else {
            statusLabel.setText("Locked");
            statusLabel.setTextFill(Color.web("#8f94a3"));
        }

        Button claimButton = new Button("Claim Reward");
        claimButton.setDisable(!unlocked || progress.isClaimed());
        claimButton.setOnAction(e -> {
            var result = achievementService.claimAchievementReward(achievement.getId());
            if (result.isSuccess()) {
                refreshAchievements();
            }
        });
        
        VBox tile = new VBox(10, name, description, progressBar, progressText, rewardLabel, statusLabel, claimButton);
        tile.setPadding(new Insets(15));
        
        if (unlocked) {
            tile.setStyle("-fx-border-color: #4CAF50; -fx-border-width: 2; -fx-border-radius: 6; -fx-background-color: #1c1f26;");
        } else {
            tile.setStyle("-fx-border-color: #2d2f36; -fx-border-radius: 6; -fx-background-color: #171a20;");
            name.setTextFill(Color.web("#8f94a3"));
            description.setTextFill(Color.web("#8f94a3"));
        }
        
        tile.setPrefWidth(600);
        return tile;
    }

    public Parent getRoot() {
        return root;
    }
}

