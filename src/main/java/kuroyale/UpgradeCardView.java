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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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
        root.setPadding(new Insets(20));

        Label title = new Label("Upgrade Card / Collection");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        root.setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(0, 0, 20, 0));

        // Top bar with gold balance
        HBox topBar = new HBox(10);
        goldLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        goldLabel.setTextFill(Color.GOLD);
        topBar.getChildren().addAll(new Label("Gold:"), goldLabel);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10));

        cardsPane.setHgap(12);
        cardsPane.setVgap(12);
        cardsPane.setPrefWrapLength(600);

        detailsPane.setPadding(new Insets(20));
        detailsPane.setPrefWidth(400);
        detailsPane.setStyle("-fx-border-color: #2d2f36; -fx-border-radius: 6; -fx-background-color: #1c1f26;");

        VBox cardsBox = new VBox(10, new Label("Your Collection"), cardsPane);
        cardsBox.setPadding(new Insets(10));
        cardsBox.setPrefWidth(620);

        HBox content = new HBox(20, cardsBox, detailsPane);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox centerContent = new VBox(10, topBar, scrollPane);
        root.setCenter(centerContent);

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> navigator.showWelcomeScreen());
        HBox bottomBar = new HBox(backButton);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(10));
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
        Label name = new Label(card.getName());
        name.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        Label level = new Label("Level: " + progression.getLevel() + "/3");
        level.setTextFill(Color.LIGHTBLUE);
        
        Label rarity = new Label("Rarity: " + (progression.getRarity() != null ? progression.getRarity().name() : "COMMON"));
        rarity.setTextFill(Color.GRAY);

        Button selectButton = new Button("Select");
        selectButton.setMaxWidth(Double.MAX_VALUE);
        selectButton.setOnAction(e -> selectCard(card));

        VBox tile = new VBox(5, name, level, rarity, selectButton);
        tile.setPadding(new Insets(10));
        tile.setStyle("-fx-border-color: #2d2f36; -fx-border-radius: 6; -fx-background-color: #1c1f26;");
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
            detailsPane.getChildren().add(new Label("No card selected"));
            return;
        }

        Card card = preview.getCard();
        CardProgression progression = preview.getProgression();

        Label title = new Label(card.getName());
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        
        Label levelLabel = new Label("Current Level: " + progression.getLevel() + "/3");
        Label rarityLabel = new Label("Rarity: " + (progression.getRarity() != null ? progression.getRarity().name() : "COMMON"));
        
        Label currentStatsTitle = new Label("Current Stats:");
        currentStatsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        Label currentStats = new Label(formatStats(preview.getCurrentStats()));
        
        Label nextStatsTitle = new Label("Next Level Stats:");
        nextStatsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        Label nextStats = new Label(formatStats(preview.getNextStats()));
        nextStats.setTextFill(Color.LIGHTGREEN);
        
        Label costLabel = new Label("Upgrade Cost: " + preview.getUpgradeCost() + " gold");
        costLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        costLabel.setTextFill(Color.GOLD);
        
        Button upgradeButton = new Button("Upgrade");
        upgradeButton.setDisable(!preview.canUpgrade());
        upgradeButton.setPrefWidth(Double.MAX_VALUE);
        upgradeButton.setOnAction(e -> performUpgrade(card.getId()));
        
        if (!preview.canUpgrade()) {
            if (progression.isMaxLevel()) {
                Label maxLevelLabel = new Label("Max level reached");
                maxLevelLabel.setTextFill(Color.RED);
                detailsPane.getChildren().add(maxLevelLabel);
            } else {
                Label insufficientGoldLabel = new Label("Insufficient gold");
                insufficientGoldLabel.setTextFill(Color.RED);
                detailsPane.getChildren().add(insufficientGoldLabel);
            }
        }

        detailsPane.getChildren().addAll(
            title, levelLabel, rarityLabel,
            new Label(""), // Spacer
            currentStatsTitle, currentStats,
            new Label(""), // Spacer
            nextStatsTitle, nextStats,
            new Label(""), // Spacer
            costLabel, upgradeButton
        );
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

    public Parent getRoot() {
        return root;
    }
}

