package kuroyale;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import kuroyale.infrastructure.GameEventLogger;

public class WelcomeView {

    private final BorderPane root;
    private final ScreenNavigator navigator;
    private final StackPane centerContent;

    // Styles are defined in src/main/resources/main_menu.css
    // We will assume the stylesheet is loaded by the Scene or Parent.

    public WelcomeView(ScreenNavigator navigator) {
        this.navigator = navigator;
        root = new BorderPane();
        root.getStyleClass().add("main-background");

        // Ensure CSS is loaded
        root.getStylesheets().add(getClass().getResource("/main_menu.css").toExternalForm());

        // --- Top Section: Resources ---
        HBox topBar = createTopBar();
        root.setTop(topBar);

        // --- Center Section: Dynamic Content ---
        centerContent = new StackPane();
        centerContent.setAlignment(Pos.CENTER);
        root.setCenter(centerContent);

        // --- Bottom Section: Navigation ---
        HBox navBar = createNavBar();
        root.setBottom(navBar);

        // Ensure log file exists so it is visible from the main menu
        GameEventLogger.ensureLogFile();

        // Default view: Battle
        showBattleTab();
    }

    private HBox createTopBar() {
        HBox topBar = new HBox();
        topBar.getStyleClass().add("resource-bar");
        topBar.setAlignment(Pos.CENTER);

        Button logBtn = new Button("LOG");
        logBtn.getStyleClass().add("secondary-button");
        logBtn.setStyle("-fx-padding: 4 10 4 10; -fx-font-size: 10;");
        logBtn.setOpacity(0.0);
        logBtn.setPickOnBounds(true);
        logBtn.setOnAction(e -> {
            GameEventLogger.ensureLogFile();
            GameEventLogger.openLogFile();
            GameEventLogger.log("LOG_OPENED from=main_menu");
        });

        // Mock User Profile
        Label levelLabel = new Label("Level 13");
        levelLabel.getStyleClass().add("resource-item");

        Label nameLabel = new Label("Challenger"); // Could fetch from PlayerProfile
        nameLabel.getStyleClass().add("resource-item");

        // Mock Resources
        Label goldLabel = new Label("53,200 Gold");
        goldLabel.getStyleClass().addAll("resource-item", "gold-text");

        Label gemLabel = new Label("120 Gems");
        gemLabel.getStyleClass().addAll("resource-item", "gem-text");

        topBar.getChildren().addAll(logBtn, levelLabel, nameLabel, createSpacer(), goldLabel, gemLabel);
        return topBar;
    }

    private HBox createNavBar() {
        HBox navBar = new HBox();
        navBar.getStyleClass().add("nav-bar");
        navBar.setAlignment(Pos.CENTER);

        ToggleGroup navGroup = new ToggleGroup();

        ToggleButton shopTab = createNavButton("Shop", navGroup);
        shopTab.setOnAction(e -> showShopTab());

        ToggleButton cardsTab = createNavButton("Cards", navGroup);
        cardsTab.setOnAction(e -> showCardsTab());

        ToggleButton battleTab = createNavButton("Battle", navGroup);
        battleTab.setSelected(true); // Default
        battleTab.setOnAction(e -> showBattleTab());

        ToggleButton socialTab = createNavButton("Social", navGroup);
        socialTab.setOnAction(e -> showSocialTab());

        ToggleButton eventsTab = createNavButton("Events", navGroup);
        eventsTab.setOnAction(e -> showEventsTab());

        // Badges
        applyBadgeIfNeeded(eventsTab, navigator.getQuestService()); // Quests are in Events now
        // applyBadgeIfNeeded(socialTab, navigator.getAchievementService()); //
        // Achievements can be in Social or Events

        navBar.getChildren().addAll(shopTab, cardsTab, battleTab, socialTab, eventsTab);
        return navBar;
    }

    private ToggleButton createNavButton(String text, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.getStyleClass().add("nav-button");
        btn.setPrefWidth(100);
        return btn;
    }

    // --- Content Switchers ---

    private void showBattleTab() {
        VBox battleView = new VBox(20);
        battleView.getStyleClass().add("battle-section");
        battleView.setAlignment(Pos.CENTER);

        Label arenaLabel = new Label("Arena 1: Training Camp");
        arenaLabel.getStyleClass().add("arena-title");

        Button battleBtn = new Button("BATTLE");
        battleBtn.getStyleClass().add("battle-button");
        battleBtn.setOnAction(e -> navigator.showArenaSelection()); // Or start game directly? Standard flow is
                                                                    // selection.

        HBox subModes = new HBox(15);
        subModes.setAlignment(Pos.CENTER);

        Button pvpBtn = new Button("2v2 / Local");
        pvpBtn.getStyleClass().add("secondary-button");
        pvpBtn.setOnAction(e -> navigator.showLocalPvPSetupScreen());

        Button networkBtn = new Button("Network");
        networkBtn.getStyleClass().add("secondary-button");
        networkBtn.setOnAction(e -> navigator.showNetworkMenuScreen());

        Button quitBtn = new Button("Quit");
        quitBtn.getStyleClass().add("secondary-button");
        quitBtn.setOnAction(e -> Platform.exit());

        subModes.getChildren().addAll(pvpBtn, networkBtn, quitBtn);

        battleView.getChildren().addAll(arenaLabel, battleBtn, subModes);
        centerContent.getChildren().clear();
        centerContent.getChildren().add(battleView);
    }

    private void showCardsTab() {
        VBox cardsView = new VBox(15);
        cardsView.setAlignment(Pos.CENTER);

        Label title = new Label("Collection");
        title.getStyleClass().add("arena-title");

        Button deckBuilderBtn = new Button("Edit Deck");
        deckBuilderBtn.getStyleClass().add("secondary-button");
        deckBuilderBtn.setOnAction(e -> navigator.showDeckBuilderScreen());

        Button upgradeBtn = new Button("Upgrade Cards");
        upgradeBtn.getStyleClass().add("secondary-button");
        upgradeBtn.setOnAction(e -> navigator.showUpgradeCardScreen());

        Button comboBtn = new Button("Combo Library");
        comboBtn.getStyleClass().add("secondary-button");
        comboBtn.setOnAction(e -> navigator.showComboLibraryScreen());

        cardsView.getChildren().addAll(title, deckBuilderBtn, upgradeBtn, comboBtn);
        centerContent.getChildren().clear();
        centerContent.getChildren().add(cardsView);
    }

    private void showEventsTab() {
        VBox eventsView = new VBox(15);
        eventsView.setAlignment(Pos.CENTER);

        Label title = new Label("Events & Quests");
        title.getStyleClass().add("arena-title");

        Button dailyQuestBtn = new Button("Daily Quests");
        dailyQuestBtn.getStyleClass().add("secondary-button");
        dailyQuestBtn.setOnAction(e -> navigator.showDailyQuestScreen());

        Button challengeBtn = new Button("Challenge Mode");
        challengeBtn.getStyleClass().add("secondary-button");
        challengeBtn.setOnAction(e -> navigator.showChallengeModeScreen());

        eventsView.getChildren().addAll(title, dailyQuestBtn, challengeBtn);
        centerContent.getChildren().clear();
        centerContent.getChildren().add(eventsView);
    }

    private void showSocialTab() {
        VBox socialView = new VBox(15);
        socialView.setAlignment(Pos.CENTER);

        Label title = new Label("Social");
        title.getStyleClass().add("arena-title");

        Button historyBtn = new Button("Match History");
        historyBtn.getStyleClass().add("secondary-button");
        historyBtn.setOnAction(e -> navigator.showMatchHistoryScreen());

        Button achievBtn = new Button("Achievements");
        achievBtn.getStyleClass().add("secondary-button");
        achievBtn.setOnAction(e -> navigator.showAchievementScreen());

        Button settingsBtn = new Button("Settings");
        settingsBtn.getStyleClass().add("secondary-button");
        settingsBtn.setOnAction(e -> navigator.showSettingsScreen());

        socialView.getChildren().addAll(title, historyBtn, achievBtn, settingsBtn);
        centerContent.getChildren().clear();
        centerContent.getChildren().add(socialView);
    }

    private void showShopTab() {
        VBox shopView = new VBox(15);
        shopView.setAlignment(Pos.CENTER);

        Label title = new Label("Shop & Design");
        title.getStyleClass().add("arena-title");

        Button layoutLibBtn = new Button("Saved Arenas");
        layoutLibBtn.getStyleClass().add("secondary-button");
        layoutLibBtn.setOnAction(e -> navigator.showLayoutLibrary());

        Button designerBtn = new Button("Design Arena");
        designerBtn.getStyleClass().add("secondary-button");
        designerBtn.setOnAction(e -> navigator.showArenaDesignerScreen());

        Button demoBtn = new Button("Layout Preview");
        demoBtn.getStyleClass().add("secondary-button");
        demoBtn.setOnAction(e -> navigator.showArenaDemoScreen());

        shopView.getChildren().addAll(title, layoutLibBtn, designerBtn, demoBtn);
        centerContent.getChildren().clear();
        centerContent.getChildren().add(shopView);
    }

    private HBox createSpacer() {
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return new HBox(spacer);
    }

    // Original helper methods adapted if needed
    private void applyBadgeIfNeeded(ToggleButton button, application.QuestService questService) {
        if (button == null || questService == null)
            return;
        int count = questService.countUnclaimedRewards();
        if (count > 0)
            button.setText(button.getText() + " (" + count + ")");
    }

    // Overloaded for Button if needed in sub-views, though not used in navbar
    private void applyBadgeIfNeeded(Button button, application.QuestService questService) {
        if (button == null || questService == null)
            return;
        int count = questService.countUnclaimedRewards();
        if (count > 0)
            button.setText(button.getText() + " (" + count + ")");
    }

    public Parent getRoot() {
        return root;
    }
}