package kuroyale;

import java.net.URL;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.input.MouseEvent;

public class SplashScreenView {

    private final ScreenNavigator navigator;
    private final StackPane root;

    public SplashScreenView(ScreenNavigator navigator) {
        this.navigator = navigator;
        this.root = new StackPane();
        root.setStyle("-fx-background-color: black;");

        // Load Background Image
        Image bgImage = loadImage("/assets/ui/splash_bg.jpg");
        if (bgImage != null) {
            ImageView bgView = new ImageView(bgImage);
            bgView.setPreserveRatio(true);
            bgView.setFitHeight(600); // Default height
            bgView.fitWidthProperty().bind(root.widthProperty());
            bgView.fitHeightProperty().bind(root.heightProperty());

            // If the image aspect ratio doesn't match window, we might want to cover
            // But for a splash, "cover" or "contain" depends. Let's try to fill nicely.
            // Using a simple image view for now.
            root.getChildren().add(bgView);
        } else {
            // Fallback text if image missing
            Label title = new Label("KU Royale");
            title.setTextFill(Color.WHITE);
            title.setFont(Font.font("Arial", FontWeight.BOLD, 48));
            root.getChildren().add(title);
        }

        // click anywhere to continue text
        Label continueLabel = new Label("Click anywhere to start");
        continueLabel.setTextFill(Color.WHITE);
        continueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        continueLabel.setStyle("-fx-effect: dropshadow(three-pass-box, black, 10, 0.5, 0, 0);");

        VBox bottomBox = new VBox(continueLabel);
        bottomBox.setAlignment(Pos.BOTTOM_CENTER);
        bottomBox.setPickOnBounds(false); // Let clicks pass through to root
        // Margin from bottom
        bottomBox.setTranslateY(-50);
        StackPane.setAlignment(bottomBox, Pos.BOTTOM_CENTER);

        root.getChildren().add(bottomBox);

        // Interaction
        root.setOnMouseClicked(this::onScreenClicked);

        // Auto-play sound
        playSound("/assets/sounds/intro.mp3");
    }

    private void onScreenClicked(MouseEvent event) {
        // Transition to main menu
        navigator.showWelcomeScreen();
    }

    private void playSound(String resourcePath) {
        try {
            URL resource = getClass().getResource(resourcePath);
            if (resource != null) {
                AudioClip clip = new AudioClip(resource.toExternalForm());
                clip.play();
            } else {
                System.out.println("Sound file not found: " + resourcePath);
            }
        } catch (Exception e) {
            System.err.println("Failed to play sound: " + e.getMessage());
        }
    }

    private Image loadImage(String path) {
        try {
            var url = getClass().getResource(path);
            if (url == null)
                return null;
            return new Image(url.toExternalForm());
        } catch (Exception e) {
            return null;
        }
    }

    public Parent getRoot() {
        return root;
    }
}
