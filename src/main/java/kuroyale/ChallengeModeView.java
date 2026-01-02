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
 * Phase 2 Feature 4 entry screen placeholder.
 * <p>
 * The full Challenge list/lock/star UI is implemented in the next step; this keeps the build green in the meantime.
 */
public class ChallengeModeView {
    private final VBox root;

    public ChallengeModeView(ScreenNavigator navigator) {
        root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label title = new Label("Challenge Mode (Phase 2)");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));

        Label subtitle = new Label("Challenge selection + rules enforcement coming next.");

        Button back = new Button("Back");
        back.setOnAction(e -> navigator.showWelcomeScreen());

        root.getChildren().addAll(title, subtitle, back);
    }

    public Parent getRoot() {
        return root;
    }
}


