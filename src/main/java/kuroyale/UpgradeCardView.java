package kuroyale;

import application.CardUpgradeService;
import application.CardUpgradeService.UpgradePreview;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import kuroyale.domain.Card;
import kuroyale.domain.CardProgression;
import kuroyale.domain.PlayerProfile;

public class UpgradeCardView {

    private final BorderPane root;
    private final CardUpgradeService upgradeService;
    private final FlowPane cardsPane = new FlowPane();
    private final VBox detailsPane = new VBox(10);
    private final Label goldLabel = new Label();
    private Card selectedCard;

    public UpgradeCardView(ScreenNavigator navigator, CardUpgradeService upgradeService) {
        this.upgradeService = upgradeService;
        
        root = new BorderPane();
        root.setPadding(new Insets(16));
        root.getStyleClass().add("upgrade-root"); // UI-only change: visual hierarchy

        Label title = new Label("Upgrade Card / Collection");
        title.getStyleClass().add("upgrade-title");
        Label subtitle = new Label("Manage your collection and upgrade cards");
        subtitle.getStyleClass().add("upgrade-subtitle");

        // Top bar with gold balance
        HBox goldBadge = new HBox(6);
        goldBadge.getStyleClass().add("upgrade-gold-badge");
        Label goldIcon = new Label("G");
        goldIcon.getStyleClass().add("upgrade-gold-icon");
        goldLabel.getStyleClass().add("upgrade-gold-value");
        goldBadge.getChildren().addAll(goldIcon, goldLabel);

        VBox titleBox = new VBox(4, title, subtitle);
        HBox header = new HBox(12, titleBox, goldBadge);
        header.getStyleClass().add("upgrade-header"); // UI-only change: spacing
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleBox, javafx.scene.layout.Priority.ALWAYS);
        root.setTop(header);
        BorderPane.setMargin(header, new Insets(0, 0, 12, 0));

        cardsPane.setHgap(12);
        cardsPane.setVgap(12);
        cardsPane.setPrefWrapLength(600);
        cardsPane.getStyleClass().add("upgrade-cards-pane");

        detailsPane.setPadding(new Insets(20));
        detailsPane.setPrefWidth(400);
        detailsPane.getStyleClass().add("upgrade-details-panel");

        Label collectionTitle = new Label("Your Collection");
        collectionTitle.getStyleClass().add("upgrade-section-title");
        VBox cardsBox = new VBox(12, collectionTitle, cardsPane);
        cardsBox.setPadding(new Insets(12));
        cardsBox.setPrefWidth(620);
        cardsBox.getStyleClass().add("upgrade-collection-panel");

        HBox content = new HBox(20, cardsBox, detailsPane);
        content.getStyleClass().add("upgrade-content");
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.getStyleClass().add("upgrade-scroll");

        VBox centerContent = new VBox(12, scrollPane);
        centerContent.getStyleClass().add("upgrade-center");
        root.setCenter(centerContent);

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("upgrade-back-button");
        backButton.setOnAction(e -> navigator.showWelcomeScreen());
        HBox bottomBar = new HBox(backButton);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(8, 0, 4, 0));
        bottomBar.getStyleClass().add("upgrade-bottom");
        root.setBottom(bottomBar);

        refreshCollection();
        updateGoldDisplay();
    }

    private void refreshCollection() {
        cardsPane.getChildren().clear();
        PlayerProfile profile = upgradeService.getPlayerProfile();
        
        java.util.List<Card> allCards = upgradeService.getAllCards();
        for (Card card : allCards) {
            CardProgression progression = profile.getCardProgression(card.getId());
            if (progression == null) {
                progression = new CardProgression(card.getId(), 1, kuroyale.domain.Rarity.COMMON);
            }
            cardsPane.getChildren().add(createCardTile(card, progression));
        }
    }

    private VBox createCardTile(Card card, CardProgression progression) {
        javafx.scene.image.ImageView portrait = CardArt.buildCardPortrait(card, 40);
        Label name = new Label(card.getName());
        name.getStyleClass().add("upgrade-card-name");
        
        String levelStars = starsForLevel(progression.getLevel());
        Label level = new Label("Level: " + progression.getLevel() + "/3 " + levelStars);
        level.getStyleClass().add("upgrade-card-meta");
        level.setTextFill(Color.web("#9fb7ff"));
        
        String rarityName = progression.getRarity() != null ? progression.getRarity().name() : "COMMON";
        Label rarity = new Label("Rarity: " + rarityName);
        rarity.getStyleClass().add("upgrade-card-meta");
        rarity.setTextFill(colorForRarity(progression.getRarity()));

        Button selectButton = new Button("Select");
        selectButton.getStyleClass().add("upgrade-select-button");
        selectButton.setMaxWidth(Double.MAX_VALUE);
        selectButton.setOnAction(e -> selectCard(card));

        VBox tile = portrait != null
                ? new VBox(5, portrait, name, level, rarity, selectButton)
                : new VBox(5, name, level, rarity, selectButton);
        tile.setPadding(new Insets(12));
        tile.getStyleClass().add("upgrade-card-tile");
        tile.setStyle("-fx-border-color: " + toHex(colorForRarity(progression.getRarity())) + ";");
        tile.setPrefWidth(160);
        return tile;
    }

    private void selectCard(Card card) {
        selectedCard = card;
        UpgradePreview preview = upgradeService.getUpgradePreview(card.getId());
        displayUpgradeDetails(preview);
    }

    private void displayUpgradeDetails(UpgradePreview preview) {
        detailsPane.getChildren().clear();
        
        if (preview == null) {
            Label emptyState = new Label("No card selected");
            emptyState.getStyleClass().add("upgrade-empty-state");
            detailsPane.getChildren().add(emptyState);
            return;
        }

        Card card = preview.getCard();
        CardProgression progression = preview.getProgression();
        javafx.scene.image.ImageView portrait = CardArt.buildCardPortrait(card, 64);

        Label title = new Label(card.getName());
        title.getStyleClass().add("upgrade-details-title");
        
        String rarityName = progression.getRarity() != null ? progression.getRarity().name() : "COMMON";
        Label levelLabel = new Label("Current Level: " + progression.getLevel() + "/3 " + starsForLevel(progression.getLevel()));
        Label rarityLabel = new Label("Rarity: " + rarityName);
        levelLabel.getStyleClass().add("upgrade-details-meta");
        rarityLabel.getStyleClass().add("upgrade-details-meta");
        rarityLabel.setTextFill(colorForRarity(progression.getRarity()));
        
        Label currentStatsTitle = new Label("Current Stats:");
        currentStatsTitle.getStyleClass().add("upgrade-section-title");
        Label currentStats = new Label(formatStats(preview.getCurrentStats()));
        currentStats.getStyleClass().add("upgrade-stats-text");
        
        Label nextStatsTitle = new Label("Next Level Stats:");
        nextStatsTitle.getStyleClass().add("upgrade-section-title");
        Label nextStats = new Label(formatStats(preview.getNextStats()));
        nextStats.getStyleClass().add("upgrade-stats-text");
        nextStats.setTextFill(Color.web("#86efac"));
        
        Label costLabel = new Label("Upgrade Cost: " + preview.getUpgradeCost() + " gold");
        costLabel.getStyleClass().add("upgrade-cost");
        
        Button upgradeButton = new Button("Upgrade");
        upgradeButton.getStyleClass().add("upgrade-primary-button");
        upgradeButton.setDisable(!preview.canUpgrade());
        upgradeButton.setPrefWidth(Double.MAX_VALUE);
        upgradeButton.setOnAction(e -> performUpgrade(card.getId()));
        
        if (!preview.canUpgrade()) {
            if (progression.isMaxLevel()) {
                Label maxLevelLabel = new Label("Max level reached");
                maxLevelLabel.getStyleClass().add("upgrade-warning");
                detailsPane.getChildren().add(maxLevelLabel);
            } else {
                Label insufficientGoldLabel = new Label("Insufficient gold");
                insufficientGoldLabel.getStyleClass().add("upgrade-warning");
                detailsPane.getChildren().add(insufficientGoldLabel);
            }
        }

        StackPane portraitFrame = new StackPane(portrait != null ? portrait : new Label(""));
        portraitFrame.getStyleClass().add("upgrade-portrait-frame");

        VBox infoSection = new VBox(6, title, levelLabel, rarityLabel);
        infoSection.getStyleClass().add("upgrade-info-section");

        VBox currentStatsBox = new VBox(6, currentStatsTitle, currentStats);
        currentStatsBox.getStyleClass().add("upgrade-stats-box");

        VBox nextStatsBox = new VBox(6, nextStatsTitle, nextStats);
        nextStatsBox.getStyleClass().add("upgrade-stats-box");

        HBox statsRow = new HBox(12, currentStatsBox, nextStatsBox);
        statsRow.getStyleClass().add("upgrade-stats-row");

        VBox costSection = new VBox(8, costLabel, upgradeButton);
        costSection.getStyleClass().add("upgrade-cost-section");

        detailsPane.getChildren().addAll(
            portraitFrame,
            infoSection,
            statsRow,
            costSection
        );
        detailsPane.setStyle("-fx-border-color: " + toHex(colorForRarity(progression.getRarity())) + ";");
    }

    private String formatStats(kuroyale.domain.CardStats stats) {
        if (stats == null) {
            return "N/A";
        }
        return String.format("HP: %d | Damage: %d | Range: %d | Speed: %d | Hit Speed: %dms",
            stats.getHp(), stats.getDamage(), stats.getRange(), 
            stats.getMoveSpeed(), stats.getHitSpeedMillis());
    }

    private void performUpgrade(String cardId) {
        var result = upgradeService.upgradeCard(cardId);
        
        if (result.isSuccess()) {
            Alert alert = new Alert(AlertType.INFORMATION, 
                "Card upgraded successfully! New level: " + result.getData().getLevel());
            if (root.getScene() != null) {
                alert.initOwner(root.getScene().getWindow());
            }
            alert.showAndWait();
            refreshCollection();
            updateGoldDisplay();
            if (selectedCard != null && selectedCard.getId().equals(cardId)) {
                selectCard(selectedCard); // Refresh details
            }
        } else {
            Alert alert = new Alert(AlertType.ERROR, result.getMessage());
            if (root.getScene() != null) {
                alert.initOwner(root.getScene().getWindow());
            }
            alert.showAndWait();
        }
    }

    private void updateGoldDisplay() {
        PlayerProfile profile = upgradeService.getPlayerProfile();
        goldLabel.setText(String.valueOf(profile.getGold()));
    }

    private Color colorForRarity(kuroyale.domain.Rarity rarity) {
        if (rarity == null) {
            return Color.GRAY;
        }
        switch (rarity) {
            case COMMON:
                return Color.web("#c0c0c0");
            case RARE:
                return Color.web("#4a90e2");
            case EPIC:
                return Color.web("#9b59b6");
            case LEGENDARY:
                return Color.web("#f39c12");
            default:
                return Color.GRAY;
        }
    }

    private String starsForLevel(int level) {
        if (level <= 1) {
            return "*";
        }
        if (level == 2) {
            return "**";
        }
        return "***";
    }

    private String toHex(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    public Parent getRoot() {
        return root;
    }
}

