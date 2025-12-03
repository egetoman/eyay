package kuroyale;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class StartGameView {
    // Shows the actual start match menu.
    private final VBox root;

    public StartGameView(ScreenNavigator navigator) {
        root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        Label title = new Label("Start Game Screen (Work In Progress)");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));

        Button backButton = new Button("Back to Main Menu");
        backButton.setOnAction(event -> navigator.showWelcomeScreen());

        root.getChildren().addAll(title, backButton);
    }

    public Parent getRoot() {
        return root;
    }
}
