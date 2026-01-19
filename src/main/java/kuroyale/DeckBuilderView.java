package kuroyale;

import application.DeckController;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import kuroyale.domain.Card;
import kuroyale.domain.Deck;

public class DeckBuilderView {

    private final BorderPane root;
    private final DeckController deckController;
    private final List<Card> availableCards;
    private Deck currentDeck;

    private final FlowPane availableCardsPane = new FlowPane();
    private final FlowPane deckSlotsPane = new FlowPane();
    private final Label statusLabel = new Label();

    public DeckBuilderView(ScreenNavigator navigator, DeckController deckController) {
        this.deckController = deckController;
        this.availableCards = new ArrayList<>(deckController.getAvailableCards());
        this.currentDeck = new Deck(deckController.loadDeck().getCards());

        root = new BorderPane();
        root.setPadding(new Insets(20));

        Label title = new Label("Deck Builder");
        title.getStyleClass().add("screen-title");
        root.setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(0, 0, 20, 0));

        availableCardsPane.setHgap(12);
        availableCardsPane.setVgap(12);
        availableCardsPane.setPrefWrapLength(400);

        deckSlotsPane.setHgap(12);
        deckSlotsPane.setVgap(12);
        deckSlotsPane.setPrefWrapLength(400);

        // --- Available Cards Panel ---
        VBox availableBox = new VBox(10);
        Label availTitle = new Label("Available Cards");
        availTitle.getStyleClass().add("section-title");
        ScrollPane availableScroll = new ScrollPane(availableCardsPane);
        availableScroll.setFitToWidth(true);
        availableScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(availableScroll, Priority.ALWAYS);
        availableBox.getChildren().addAll(availTitle, availableScroll);
        availableBox.getStyleClass().add("sub-panel");
        availableBox.setPrefWidth(420);

        // --- Current Deck Panel ---
        VBox deckBox = new VBox(10);
        Label deckTitle = new Label("Your Deck (8 cards)");
        deckTitle.getStyleClass().add("section-title");
        ScrollPane deckScroll = new ScrollPane(deckSlotsPane);
        deckScroll.setFitToWidth(true);
        deckScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(deckScroll, Priority.ALWAYS);
        deckBox.getChildren().addAll(deckTitle, deckScroll, statusLabel, buildDeckActions(navigator));
        deckBox.getStyleClass().add("sub-panel");
        deckBox.setPrefWidth(420);

        HBox content = new HBox(20, availableBox, deckBox);
        content.setAlignment(Pos.CENTER);
        HBox.setHgrow(deckBox, Priority.ALWAYS);

        root.setCenter(content);

        refreshAvailableCards();
        refreshDeckSlots();
    }

    private HBox buildDeckActions(ScreenNavigator navigator) {
        Button saveButton = new Button("Save Deck");
        saveButton.getStyleClass().add("game-button");
        saveButton.setOnAction(e -> saveDeck());

        Button resetButton = new Button("Reset");
        resetButton.getStyleClass().add("secondary-button");
        resetButton.setOnAction(e -> {
            currentDeck = new Deck(deckController.loadDeck().getCards());
            refreshDeckSlots();
        });

        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("danger-button");
        clearButton.setOnAction(e -> {
            currentDeck.clear();
            refreshDeckSlots();
        });

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("secondary-button");
        backButton.setOnAction(e -> navigator.showWelcomeScreen());

        HBox box = new HBox(10, saveButton, resetButton, clearButton, backButton);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void refreshAvailableCards() {
        availableCardsPane.getChildren().clear();
        for (Card card : availableCards) {
            availableCardsPane.getChildren().add(createCardTile(card));
        }
    }

    private VBox createCardTile(Card card) {
        ImageView portrait = CardArt.buildCardPortrait(card, 40);
        Label name = new Label(card.getName());
        name.setStyle("-fx-font-weight: bold;");
        Label cost = new Label("Elixir: " + card.getElixirCost());
        cost.setTextFill(Color.web("#ffd700"));
        Label type = new Label(card.getType().name());
        type.setStyle("-fx-font-size: 10px;");

        Button addButton = new Button("Add");
        addButton.getStyleClass().add("secondary-button");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(e -> {
            if (currentDeck.containsCard(card)) {
                showWarning(card.getName() + " is already in your deck.");
                return;
            }
            if (!currentDeck.addCard(card)) {
                showWarning("Deck is full. Remove a card before adding new ones.");
                return;
            }
            refreshDeckSlots();
        });

        VBox tile = portrait != null
                ? new VBox(5, portrait, name, cost, type, addButton)
                : new VBox(5, name, cost, type, addButton);
        tile.getStyleClass().add("card-tile");
        tile.setPrefWidth(140);
        return tile;
    }

    private void refreshDeckSlots() {
        deckSlotsPane.getChildren().clear();
        List<Card> cards = currentDeck.getCards();
        for (int i = 0; i < Deck.MAX_CARDS; i++) {
            Card card = i < cards.size() ? cards.get(i) : null;
            deckSlotsPane.getChildren().add(createDeckSlot(card));
        }
        statusLabel.setText("Cards: " + cards.size() + "/" + Deck.MAX_CARDS);
    }

    private VBox createDeckSlot(Card card) {
        ImageView portrait = CardArt.buildCardPortrait(card, 40);
        Label label = new Label(card != null ? card.getName() : "Empty");

        Button actionButton = new Button(card != null ? "Remove" : "-");
        actionButton.getStyleClass().add("secondary-button");
        actionButton.setDisable(card == null);
        actionButton.setOnAction(e -> {
            if (card != null) {
                currentDeck.removeCard(card);
                refreshDeckSlots();
            }
        });

        VBox slot = portrait != null
                ? new VBox(5, portrait, label, actionButton)
                : new VBox(5, label, actionButton);
        slot.getStyleClass().add("card-tile");
        slot.setPrefWidth(130);
        return slot;
    }

    private void saveDeck() {
        if (!currentDeck.isValid()) {
            showWarning("Deck must contain exactly " + Deck.MAX_CARDS + " cards before saving.");
            return;
        }
        try {
            deckController.saveDeck(new Deck(currentDeck.getCards()));
            Alert alert = new Alert(AlertType.INFORMATION, "Deck saved successfully.");
            if (root.getScene() != null) {
                alert.initOwner(root.getScene().getWindow());
            }
            alert.showAndWait();
        } catch (IllegalArgumentException ex) {
            showWarning(ex.getMessage());
        }
    }

    private void showWarning(String message) {
        Alert alert = new Alert(AlertType.WARNING, message);
        if (root.getScene() != null) {
            alert.initOwner(root.getScene().getWindow());
        }
        alert.showAndWait();
    }

    public Parent getRoot() {
        return root;
    }
}
