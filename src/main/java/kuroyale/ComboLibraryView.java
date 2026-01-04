package kuroyale;

import application.ComboDetector;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import kuroyale.domain.ComboDefinition;
import kuroyale.domain.ComboType;

import java.util.List;

/**
 * View showing all available combos with their descriptions and effects.
 */
public class ComboLibraryView {
    private final BorderPane root;
    private final ScreenNavigator navigator;
    private final ComboDetector comboDetector;

    public ComboLibraryView(ScreenNavigator navigator) {
        this.navigator = navigator;
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

        // Combo list
        VBox comboContainer = new VBox(15);
        comboContainer.setPadding(new Insets(10));

        List<ComboDefinition> definitions = comboDetector.getAllComboDefinitions();
        for (ComboDefinition def : definitions) {
            comboContainer.getChildren().add(createComboCard(def));
        }

        ScrollPane scrollPane = new ScrollPane(comboContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        root.setCenter(scrollPane);
    }

    private VBox createComboCard(ComboDefinition def) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #1c1f2e; -fx-background-radius: 10; -fx-border-color: #404459; -fx-border-radius: 10; -fx-border-width: 1;");

        ComboType type = def.getType();

        // Combo name
        Label nameLabel = new Label(type.getDisplayName() + "!");
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        nameLabel.setTextFill(Color.web("#ffd54f"));

        // Description
        Label descLabel = new Label(type.getDescription());
        descLabel.setFont(Font.font("Arial", 14));
        descLabel.setTextFill(Color.web("#cbd0d6"));
        descLabel.setWrapText(true);

        // Effect
        Label effectLabel = new Label("Effect: " + def.getEffectDescription());
        effectLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        effectLabel.setTextFill(Color.web("#9be564"));

        card.getChildren().addAll(nameLabel, descLabel, effectLabel);
        return card;
    }

    public Parent getRoot() {
        return root;
    }
}

