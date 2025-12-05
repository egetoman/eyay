package kuroyale;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import kuroyale.domain.ArenaLayout;

public class ArenaView {

    private final BorderPane root;
    private final Runnable backAction;

    public ArenaView(ScreenNavigator navigator) {
        this(navigator, ArenaLayout.defaultLayout(), navigator::showWelcomeScreen);
    }

    public ArenaView(ScreenNavigator navigator, ArenaLayout layout) {
        this(navigator, layout, navigator::showWelcomeScreen);
    }

    public ArenaView(ScreenNavigator navigator, ArenaLayout layout, Runnable backAction) {
        this.backAction = backAction;

        Label title = new Label("Arena Layout Preview: " + layout.getName());
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));

        Label subtitle = new Label("Green = Player towers, Red = Opponent towers. Bridges span the river.");
        subtitle.setFont(Font.font("Arial", 14));

        VBox header = new VBox(5, title, subtitle);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(15, 15, 5, 15));

        Button backButton = new Button("Back");
        backButton.setOnAction(event -> backAction.run());
        VBox footer = new VBox(backButton);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(15));

        root = new BorderPane();
        root.setTop(header);
        ArenaBoard board = new ArenaBoard(layout);
        root.setCenter(board.getView());
        root.setBottom(footer);
        root.setStyle("-fx-background-color: #0f1216;");
    }

    public Parent getRoot() {
        return root;
    }
}

