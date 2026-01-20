package kuroyale;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import kuroyale.infrastructure.GameEventLogger;
import java.util.ArrayList;
import java.util.List;

public class WelcomeView {

    private final StackPane root;
    private final BorderPane layout;
    private final ScreenNavigator navigator;
    private final StackPane centerContent;
    private Timeline backgroundAnim;
    private Timeline trophyRain;
    private Pane trophyLayer;
    private StackPane modalLayer;

    // Styles are defined in src/main/resources/main_menu.css
    // We will assume the stylesheet is loaded by the Scene or Parent.

    public WelcomeView(ScreenNavigator navigator) {
        this.navigator = navigator;
        root = new StackPane();
        layout = new BorderPane();
        layout.getStyleClass().add("main-background");
        root.getChildren().add(layout);
        addBackgroundEffects();

        // Ensure CSS is loaded
        layout.getStylesheets().add(getClass().getResource("/main_menu.css").toExternalForm());

        // --- Top Section: Resources ---
        HBox topBar = createTopBar();
        layout.setTop(topBar);

        // --- Center Section: Dynamic Content ---
        centerContent = new StackPane();
        centerContent.setAlignment(Pos.CENTER);
        layout.setCenter(centerContent);

        // --- Bottom Section: Navigation ---
        HBox navBar = createNavBar();
        layout.setBottom(navBar);

        modalLayer = new StackPane();
        modalLayer.setVisible(false);
        modalLayer.setMouseTransparent(true);
        root.getChildren().add(modalLayer);

        // Ensure log file exists so it is visible from the main menu
        GameEventLogger.ensureLogFile();

        // Default view: Battle
        showBattleTab();
    }

    private void addBackgroundEffects() {
        Pane layer = new Pane();
        layer.setMouseTransparent(true);
        layer.prefWidthProperty().bind(layout.widthProperty());
        layer.prefHeightProperty().bind(layout.heightProperty());

        Circle glowLeft = new Circle(220, Color.web("#3b82f6", 0.2));
        glowLeft.centerXProperty().bind(layer.widthProperty().multiply(0.2));
        glowLeft.centerYProperty().bind(layer.heightProperty().multiply(0.25));

        Circle glowRight = new Circle(260, Color.web("#f97316", 0.16));
        glowRight.centerXProperty().bind(layer.widthProperty().multiply(0.8));
        glowRight.centerYProperty().bind(layer.heightProperty().multiply(0.15));

        Circle glowBottom = new Circle(240, Color.web("#8b5cf6", 0.18));
        glowBottom.centerXProperty().bind(layer.widthProperty().multiply(0.65));
        glowBottom.centerYProperty().bind(layer.heightProperty().multiply(0.85));

        layer.getChildren().addAll(glowLeft, glowRight, glowBottom);
        layout.getChildren().add(0, layer);

        trophyLayer = new Pane();
        trophyLayer.setMouseTransparent(true);
        trophyLayer.setPickOnBounds(false);
        trophyLayer.prefWidthProperty().bind(root.widthProperty());
        trophyLayer.prefHeightProperty().bind(root.heightProperty());
        root.getChildren().add(1, trophyLayer);
        addTrophyRain(trophyLayer);

        backgroundAnim = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glowLeft.translateXProperty(), -40),
                        new KeyValue(glowLeft.opacityProperty(), 0.55),
                        new KeyValue(glowRight.translateXProperty(), 30),
                        new KeyValue(glowRight.opacityProperty(), 0.45),
                        new KeyValue(glowBottom.translateYProperty(), 20),
                        new KeyValue(glowBottom.opacityProperty(), 0.5)),
                new KeyFrame(Duration.seconds(16),
                        new KeyValue(glowLeft.translateXProperty(), 40),
                        new KeyValue(glowLeft.opacityProperty(), 0.75),
                        new KeyValue(glowRight.translateXProperty(), -30),
                        new KeyValue(glowRight.opacityProperty(), 0.7),
                        new KeyValue(glowBottom.translateYProperty(), -20),
                        new KeyValue(glowBottom.opacityProperty(), 0.7))
        );
        backgroundAnim.setAutoReverse(true);
        backgroundAnim.setCycleCount(Timeline.INDEFINITE);
        backgroundAnim.play();
    }

    private void addTrophyRain(Pane layer) {
        List<Label> trophies = new ArrayList<>();
        int count = 10;
        for (int i = 0; i < count; i++) {
            Label trophy = new Label("🏆");
            trophy.setStyle("-fx-font-size: 18px;");
            trophy.setTextFill(Color.web("#f5c542", 0.7));
            trophy.setTranslateX(Math.random() * 800);
            trophy.setTranslateY(-Math.random() * 600);
            trophies.add(trophy);
            layer.getChildren().add(trophy);
        }

        trophyRain = new Timeline(new KeyFrame(Duration.millis(40), e -> {
            double width = layer.getWidth();
            double height = layer.getHeight();
            for (int i = 0; i < trophies.size(); i++) {
                Label t = trophies.get(i);
                double speed = 0.6 + (i % 5) * 0.25;
                t.setTranslateY(t.getTranslateY() + speed);
                t.setTranslateX(t.getTranslateX() + Math.sin((t.getTranslateY() + i * 30) * 0.01) * 0.4);
                if (t.getTranslateY() > height + 30) {
                    t.setTranslateY(-20 - Math.random() * 200);
                    t.setTranslateX(Math.random() * Math.max(200, width - 40));
                }
            }
        }));
        trophyRain.setCycleCount(Timeline.INDEFINITE);
        trophyRain.play();
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
        quitBtn.setOnAction(e -> confirmQuit());

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

    private void confirmQuit() {
        modalLayer.getChildren().clear();
        modalLayer.setVisible(true);
        modalLayer.setMouseTransparent(false);

        Rectangle dim = new Rectangle();
        dim.widthProperty().bind(root.widthProperty());
        dim.heightProperty().bind(root.heightProperty());
        dim.setFill(Color.color(0, 0, 0, 0.6));

        Label title = new Label("Exit Game");
        title.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold;");
        Label body = new Label("Your current session will be closed.");
        body.setStyle("-fx-text-fill: #cbd0d6;");

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("secondary-button");
        cancel.setOnAction(e -> {
            modalLayer.setVisible(false);
            modalLayer.setMouseTransparent(true);
        });

        Button exit = new Button("Exit");
        exit.getStyleClass().add("danger-button");
        exit.setOnAction(e -> Platform.exit());

        HBox actions = new HBox(12, cancel, exit);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(12, title, body, actions);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(360);
        card.setStyle("-fx-background-color: rgba(20, 22, 28, 0.96); -fx-background-radius: 14; -fx-padding: 16; -fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 14;");

        StackPane wrapper = new StackPane(card);
        wrapper.setMaxWidth(400);

        modalLayer.getChildren().addAll(dim, wrapper);
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