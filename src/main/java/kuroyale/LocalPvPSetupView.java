package kuroyale;

import application.ArenaLayoutService;
import application.DeckController;
import application.MatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.Card;
import kuroyale.domain.Deck;
import kuroyale.domain.Player;

/**
 * Phase 2 Feature 1: Local Player vs Player - Setup screen.
 * <p>
 * Keeps UI thin: it only collects user choices and asks {@link ScreenNavigator} to start the match.
 */
public class LocalPvPSetupView {
    private final VBox root;

    public LocalPvPSetupView(ScreenNavigator navigator,
                             ArenaLayoutService arenaLayoutService,
                             DeckController deckController,
                             MatchService matchService) {
        // matchService is not used directly here (navigation starts the match through ScreenNavigator),
        // but we keep it in the signature to match the existing construction pattern.
        root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("setup-root");

        Label title = new Label("Local PvP Setup");
        title.getStyleClass().add("screen-title");

        Label hint = new Label("Choose decks for both players. This mode is turn-based: each deploy ends your turn.");
        hint.getStyleClass().add("setup-hint");

        TextField p1Name = new TextField("Player 1");
        TextField p2Name = new TextField("Player 2");
        p1Name.setPrefWidth(220);
        p2Name.setPrefWidth(220);
        p1Name.getStyleClass().add("setup-field");
        p2Name.getStyleClass().add("setup-field");

        ComboBox<String> p1DeckChoice = new ComboBox<>();
        ComboBox<String> p2DeckChoice = new ComboBox<>();
        p1DeckChoice.getItems().addAll("Saved Deck", "Default Deck", "Random Deck");
        p2DeckChoice.getItems().addAll("Saved Deck", "Default Deck", "Random Deck");
        p1DeckChoice.setValue("Saved Deck");
        p2DeckChoice.setValue("Random Deck");
        p1DeckChoice.getStyleClass().add("setup-field");
        p2DeckChoice.getStyleClass().add("setup-field");

        Label p1NameLabel = new Label("Player 1 Name");
        Label p1DeckLabel = new Label("Player 1 Deck");
        Label p2NameLabel = new Label("Player 2 Name");
        Label p2DeckLabel = new Label("Player 2 Deck");

        VBox left = new VBox(8, p1NameLabel, p1Name, p1DeckLabel, p1DeckChoice);
        VBox right = new VBox(8, p2NameLabel, p2Name, p2DeckLabel, p2DeckChoice);
        styleFormColumn(left);
        styleFormColumn(right);
        HBox form = new HBox(30, left, right);
        form.setAlignment(Pos.CENTER);
        form.getStyleClass().addAll("sub-panel", "setup-card");

        Label error = new Label();
        error.setWrapText(true);
        error.getStyleClass().add("setup-error");

        Button start = new Button("Start Local PvP");
        start.getStyleClass().add("game-button");
        start.setOnAction(e -> {
            Deck deck1 = resolveDeck(deckController, p1DeckChoice.getValue());
            Deck deck2 = resolveDeck(deckController, p2DeckChoice.getValue());
            if (deck1 == null || !deck1.isValid()) {
                error.setText("Player 1 deck is invalid.");
                return;
            }
            if (deck2 == null || !deck2.isValid()) {
                error.setText("Player 2 deck is invalid.");
                return;
            }
            if (deck1.getCards().equals(deck2.getCards())) {
                // Not a hard error, but helps meet the “decks are distinct” intent.
                error.setText("Warning: Both players selected the same deck. Consider choosing different decks.");
            } else {
                error.setText("");
            }
            ArenaLayout layout = arenaLayoutService.getActiveLayout();
            Player player1 = new Player(nonBlank(p1Name.getText(), "Player 1"), deck1, 0);
            Player player2 = new Player(nonBlank(p2Name.getText(), "Player 2"), deck2, 0);
            navigator.showLocalPvPMatch(layout, player1, player2);
        });

        Button back = new Button("Back");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> navigator.showWelcomeScreen());

        HBox buttons = new HBox(12, back, start);
        buttons.setAlignment(Pos.CENTER);
        buttons.getStyleClass().add("setup-actions");

        root.getChildren().addAll(title, hint, form, error, buttons);
    }

    private void styleFormColumn(VBox box) {
        box.setAlignment(Pos.TOP_LEFT);
        box.getChildren().stream()
            .filter(node -> node instanceof Label)
            .forEach(node -> node.getStyleClass().add("setup-label"));
    }

    private Deck resolveDeck(DeckController deckController, String choice) {
        if (deckController == null) {
            return null;
        }
        if ("Default Deck".equalsIgnoreCase(choice)) {
            return deckController.buildDefaultDeck();
        }
        if ("Random Deck".equalsIgnoreCase(choice)) {
            List<Card> available = new ArrayList<>(deckController.getAvailableCards());
            Collections.shuffle(available);
            List<Card> picked = new ArrayList<>();
            for (Card c : available) {
                if (picked.size() >= Deck.MAX_CARDS) {
                    break;
                }
                picked.add(c);
            }
            return new Deck(picked);
        }
        // Saved Deck
        return deckController.loadDeck();
    }

    private String nonBlank(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        return text.trim();
    }

    public Parent getRoot() {
        return root;
    }
}


