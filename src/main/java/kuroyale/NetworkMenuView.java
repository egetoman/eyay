package kuroyale;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Phase 2 Feature 2 entry screen placeholder.
 * <p>
 * The full host/join lobby is implemented in the next step; this keeps the build green in the meantime.
 */
public class NetworkMenuView {
    private final VBox root;

    public NetworkMenuView(ScreenNavigator navigator) {
        root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label title = new Label("Network Multiplayer (Phase 2)");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));

        Label subtitle = new Label("Host/Join lobby coming next.");

        Button back = new Button("Back");
        back.setOnAction(e -> navigator.showWelcomeScreen());

        root.getChildren().addAll(title, subtitle, back);
    }

    public Parent getRoot() {
        return root;
    }
}


