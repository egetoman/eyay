package kuroyale;

import application.ComboDetector;
import application.DeckController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import kuroyale.domain.Card;
import kuroyale.domain.ComboDefinition;
import kuroyale.domain.ComboType;
import kuroyale.domain.Deck;

import java.util.List;

/**
 * Unified view showing both current deck combos and all available combos.
 */
public class ComboLibraryView {
    private final BorderPane root;
    private final ScreenNavigator navigator;
    private final ComboDetector comboDetector;
    private final DeckController deckController;

    public ComboLibraryView(ScreenNavigator navigator, DeckController deckController) {
        this.navigator = navigator;
        this.deckController = deckController;
        this.comboDetector = new ComboDetector();
        this.root = new BorderPane();
        initializeUI();
    }

    private void initializeUI() {
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0f1216;");

        // Title
        Label title = new Label("Combo Library");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setTextFill(Color.web("#f5f5f5"));
        title.setPadding(new Insets(0, 0, 20, 0));

        // Back button
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> navigator.showWelcomeScreen());
        backButton.setStyle("-fx-background-color: #2a2f45; -fx-text-fill: #f5f5f5; -fx-font-size: 14;");
        backButton.setPadding(new Insets(8, 16, 8, 16));

        HBox header = new HBox(15, backButton, title);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));
        root.setTop(header);

        // Main content container
        VBox mainContent = new VBox(25);
        mainContent.setPadding(new Insets(10));

        // Section 1: Current Deck's Combos
        VBox deckSection = createDeckCombosSection();
        mainContent.getChildren().add(deckSection);

        // Separator
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #404459;");
        mainContent.getChildren().add(separator);

        // Section 2: All Available Combos
        VBox allCombosSection = createAllCombosSection();
        mainContent.getChildren().add(allCombosSection);

        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        root.setCenter(scrollPane);
    }

    private VBox createDeckCombosSection() {
        VBox section = new VBox(15);
        
        // Section title
        Label sectionTitle = new Label("Your Current Deck's Combos");
        sectionTitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        sectionTitle.setTextFill(Color.web("#9be564"));
        sectionTitle.setPadding(new Insets(0, 0, 10, 0));

        // Load current deck
        Deck currentDeck = deckController != null ? deckController.loadDeck() : null;
        List<Card> deckCards = currentDeck != null ? currentDeck.getCards() : null;

        if (deckCards == null || deckCards.isEmpty()) {
            Label noDeckLabel = new Label("No deck loaded. Please create a deck first.");
            noDeckLabel.setFont(Font.font("Arial", 16));
            noDeckLabel.setTextFill(Color.web("#cbd0d6"));
            noDeckLabel.setWrapText(true);
            section.getChildren().addAll(sectionTitle, noDeckLabel);
        } else {
            // Get possible combos with current deck
            List<ComboDefinition> possibleCombos = comboDetector.getPossibleCombos(deckCards);

            if (possibleCombos.isEmpty()) {
                Label noCombosLabel = new Label("Your current deck doesn't support any combos.\nTry adding cards that work together!");
                noCombosLabel.setFont(Font.font("Arial", 16));
                noCombosLabel.setTextFill(Color.web("#cbd0d6"));
                noCombosLabel.setWrapText(true);
                section.getChildren().addAll(sectionTitle, noCombosLabel);
            } else {
                Label countLabel = new Label("Your deck can trigger " + possibleCombos.size() + " combo(s):");
                countLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
                countLabel.setTextFill(Color.web("#f5f5f5"));
                section.getChildren().add(sectionTitle);
                section.getChildren().add(countLabel);

                for (ComboDefinition def : possibleCombos) {
                    section.getChildren().add(createComboCard(def, true));
                }
            }
        }

        return section;
    }

    private VBox createAllCombosSection() {
        VBox section = new VBox(15);
        
        // Section title
        Label sectionTitle = new Label("All Available Combos");
        sectionTitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        sectionTitle.setTextFill(Color.web("#ffd54f"));
        sectionTitle.setPadding(new Insets(0, 0, 10, 0));

        List<ComboDefinition> definitions = comboDetector.getAllComboDefinitions();
        Label countLabel = new Label("Total: " + definitions.size() + " combo(s)");
        countLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        countLabel.setTextFill(Color.web("#f5f5f5"));

        section.getChildren().add(sectionTitle);
        section.getChildren().add(countLabel);

        for (ComboDefinition def : definitions) {
            section.getChildren().add(createComboCard(def, false));
        }

        return section;
    }

    private VBox createComboCard(ComboDefinition def, boolean isDeckCombo) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        
        // Different border color for deck combos vs all combos
        if (isDeckCombo) {
            card.setStyle("-fx-background-color: #1c1f2e; -fx-background-radius: 10; -fx-border-color: #9be564; -fx-border-radius: 10; -fx-border-width: 2;");
        } else {
            card.setStyle("-fx-background-color: #1c1f2e; -fx-background-radius: 10; -fx-border-color: #404459; -fx-border-radius: 10; -fx-border-width: 1;");
        }

        ComboType type = def.getType();

        // Combo name
        Label nameLabel = new Label(type.getDisplayName() + "!");
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        nameLabel.setTextFill(isDeckCombo ? Color.web("#9be564") : Color.web("#ffd54f"));

        // Description
        Label descLabel = new Label(type.getDescription());
        descLabel.setFont(Font.font("Arial", 14));
        descLabel.setTextFill(Color.web("#cbd0d6"));
        descLabel.setWrapText(true);

        // Effect
        Label effectLabel = new Label("Effect: " + def.getEffectDescription());
        effectLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        effectLabel.setTextFill(isDeckCombo ? Color.web("#ffd54f") : Color.web("#9be564"));

        card.getChildren().addAll(nameLabel, descLabel, effectLabel);
        return card;
    }

    public Parent getRoot() {
        return root;
    }
}

