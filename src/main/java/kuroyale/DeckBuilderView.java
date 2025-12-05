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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        root.setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(0, 0, 20, 0));

        availableCardsPane.setHgap(12);
        availableCardsPane.setVgap(12);
        availableCardsPane.setPrefWrapLength(400);

        deckSlotsPane.setHgap(12);
        deckSlotsPane.setVgap(12);
        deckSlotsPane.setPrefWrapLength(400);

        VBox availableBox = new VBox(10, new Label("Available Cards"), availableCardsPane);
        availableBox.setPadding(new Insets(10));
        availableBox.setPrefWidth(420);

        VBox deckBox = new VBox(10, new Label("Your Deck (8 cards)"), deckSlotsPane, statusLabel, buildDeckActions(navigator));
        deckBox.setPadding(new Insets(10));
        deckBox.setPrefWidth(420);

        HBox content = new HBox(20, availableBox, deckBox);
        HBox.setHgrow(deckBox, Priority.ALWAYS);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        root.setCenter(scrollPane);

        refreshAvailableCards();
        refreshDeckSlots();
    }

    private HBox buildDeckActions(ScreenNavigator navigator) {
        Button saveButton = new Button("Save Deck");
        saveButton.setOnAction(e -> saveDeck());

        Button resetButton = new Button("Reset to Stored Deck");
        resetButton.setOnAction(e -> {
            currentDeck = new Deck(deckController.loadDeck().getCards());
            refreshDeckSlots();
        });

        Button clearButton = new Button("Clear Deck");
        clearButton.setOnAction(e -> {
            currentDeck.clear();
            refreshDeckSlots();
        });

        Button backButton = new Button("Back");
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
        Label name = new Label(card.getName());
        name.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        Label cost = new Label("Cost: " + card.getElixirCost());
        Label type = new Label(card.getType().name());
        type.setTextFill(Color.GRAY);

        Button addButton = new Button("Add");
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

        VBox tile = new VBox(5, name, cost, type, addButton);
        tile.setPadding(new Insets(10));
        tile.setStyle("-fx-border-color: #2d2f36; -fx-border-radius: 6; -fx-background-color: #1c1f26;");
        tile.setPrefWidth(160);
        return tile;
    }

    private void refreshDeckSlots() {
        deckSlotsPane.getChildren().clear();
        List<Card> cards = currentDeck.getCards();
        for (int i = 0; i < Deck.MAX_CARDS; i++) {
            Card card = i < cards.size() ? cards.get(i) : null;
            deckSlotsPane.getChildren().add(createDeckSlot(card));
        }
        statusLabel.setText("Cards selected: " + cards.size() + "/" + Deck.MAX_CARDS);
    }

    private VBox createDeckSlot(Card card) {
        Label label = new Label(card != null ? card.getName() : "Empty Slot");
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        Button actionButton = new Button(card != null ? "Remove" : "Empty");
        actionButton.setDisable(card == null);
        actionButton.setOnAction(e -> {
            if (card != null) {
                currentDeck.removeCard(card);
                refreshDeckSlots();
            }
        });

        VBox slot = new VBox(5, label, actionButton);
        slot.setPadding(new Insets(10));
        slot.setPrefWidth(150);
        slot.setStyle("-fx-border-color: #2d2f36; -fx-border-radius: 6; -fx-background-color: #0f1216;");
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

