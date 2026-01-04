package kuroyale;

import application.challenge.ChallengeService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Phase 2 Feature 4: Challenge Mode.
 */
public class ChallengeModeView {
    private final BorderPane root;
    private final ChallengeService challengeService;
    private final VBox listBox = new VBox(12);

    public ChallengeModeView(ScreenNavigator navigator, ChallengeService challengeService) {
        this.challengeService = challengeService;
        root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0f1216;");

        Label title = new Label("Challenge Mode (Phase 2)");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("Complete challenges in order to unlock the next one. Earn stars and gold.");
        subtitle.setTextFill(Color.web("#cbd0d6"));

        VBox header = new VBox(6, title, subtitle);
        header.setAlignment(Pos.CENTER);
        root.setTop(header);

        listBox.setPadding(new Insets(20, 10, 20, 10));
        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        root.setCenter(scroll);

        Button back = new Button("Back");
        back.setOnAction(e -> navigator.showWelcomeScreen());
        HBox bottom = new HBox(back);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(10, 0, 0, 0));
        root.setBottom(bottom);

        refresh(navigator);
    }

    private void refresh(ScreenNavigator navigator) {
        listBox.getChildren().clear();
        if (challengeService == null) {
            Label error = new Label("Challenge service is not available.");
            error.setTextFill(Color.web("#f05a5b"));
            listBox.getChildren().add(error);
            return;
        }
        for (var card : challengeService.listChallenges()) {
            var def = card.getDefinition();
            var prog = card.getProgress();

            Label name = new Label(def.getId() + ". " + def.getName());
            name.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            name.setTextFill(Color.WHITE);

            Label rules = new Label(def.getRulesSummary());
            rules.setTextFill(Color.web("#cbd0d6"));
            rules.setWrapText(true);

            boolean unlocked = prog != null && prog.isUnlocked();
            int stars = prog != null ? prog.getBestStars() : 0;

            Label meta = new Label((unlocked ? "Unlocked" : "Locked")
                + " • Reward: " + def.getRewardGold() + "g"
                + " • Best Stars: " + stars
                + (prog != null && prog.getBestTimeSeconds() > 0 ? String.format(" • Best Time: %.1fs", prog.getBestTimeSeconds()) : ""));
            meta.setTextFill(Color.web("#8f94a3"));

            Button start = new Button(unlocked ? "Start" : "Locked");
            start.setDisable(!unlocked);
            start.setOnAction(e -> {
                var started = challengeService.startChallenge(def.getId());
                if (!started.isSuccess()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Challenge");
                    alert.setHeaderText("Cannot start challenge");
                    alert.setContentText(started.getMessage());
                    alert.showAndWait();
                    return;
                }
                navigator.showChallengeMatchScreen(started.getData());
            });

            VBox tile = new VBox(6, name, rules, meta, start);
            tile.setPadding(new Insets(14));
            tile.setStyle("-fx-background-color: #1c1f2a; -fx-background-radius: 10; -fx-border-color: #2d2f36; -fx-border-radius: 10;");
            listBox.getChildren().add(tile);
        }
    }

    public Parent getRoot() {
        return root;
    }
}


