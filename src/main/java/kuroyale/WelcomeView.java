package kuroyale;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class WelcomeView {

    private final VBox root;
    //Shows the welcome screen of the game.
    public WelcomeView(ScreenNavigator navigator) {
        root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        Label title = new Label("KU Royale (Work In Progress)");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));

        Label subtitle = new Label("A Single-Player Strategy Battle Game");
        subtitle.setFont(Font.font("Arial", 18));

        Button startGameButton = new Button("Start Game");
        startGameButton.setOnAction(event -> navigator.showArenaSelection());

        Button deckBuilderButton = new Button("Deck Builder");
        deckBuilderButton.setOnAction(event -> navigator.showDeckBuilderScreen());

        Button designerButton = new Button("Design Arena");
        designerButton.setOnAction(event -> navigator.showArenaDesignerScreen());

        Button savedLayoutsButton = new Button("Saved Layouts");
        savedLayoutsButton.setOnAction(event -> navigator.showLayoutLibrary());

        Button arenaDemoButton = new Button("Arena Layout Preview");
        arenaDemoButton.setOnAction(event -> navigator.showArenaDemoScreen());

        Button upgradeCardButton = new Button("Upgrade Card");
        upgradeCardButton.setOnAction(event -> navigator.showUpgradeCardScreen());

        Button dailyQuestButton = new Button("Daily Quests");
        dailyQuestButton.setOnAction(event -> navigator.showDailyQuestScreen());

        Button matchHistoryButton = new Button("Match History & Stats");
        matchHistoryButton.setOnAction(event -> navigator.showMatchHistoryScreen());

        Button achievementButton = new Button("Achievements");
        achievementButton.setOnAction(event -> navigator.showAchievementScreen());

        Button quitButton = new Button("Quit");
        quitButton.setOnAction(event -> Platform.exit());

        root.getChildren().addAll(title, subtitle, startGameButton, deckBuilderButton, designerButton, 
            savedLayoutsButton, arenaDemoButton, upgradeCardButton, dailyQuestButton, matchHistoryButton, 
            achievementButton, quitButton);
    }

    public Parent getRoot() {
        return root;
    }
}