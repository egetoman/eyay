package kuroyale;

import java.net.URL;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class SplashScreenView {

    private final ScreenNavigator navigator;
    private final StackPane root;
    private final ProgressBar progressBar;
    private final Label progressLabel;
    private final Button startButton;
    private final VBox bottomBox;

    public SplashScreenView(ScreenNavigator navigator) {
        this.navigator = navigator;
        this.root = new StackPane();
        root.getStyleClass().add("root"); // Ensure theme background applies fallback

        // Note: Global theme 'root' style adds background image or gradient via CSS.
        // But for splash, we often want a specific image.
        // We will keep the specific image logic but let CSS handle fallback.

        // Load Specific Splash Image
        Image bgImage = loadImage("/assets/ui/splash_bg.jpg");
        if (bgImage != null) {
            ImageView bgView = new ImageView(bgImage);
            bgView.setPreserveRatio(true);
            bgView.setFitHeight(600);
            bgView.fitWidthProperty().bind(root.widthProperty());
            bgView.fitHeightProperty().bind(root.heightProperty());
            root.getChildren().add(bgView);
        } else {
            // Fallback Title
            Label title = new Label("KU Royale");
            title.setStyle(
                    "-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);");
            root.getChildren().add(title);
        }

        // --- Interaction Area ---

        // 1. Start Button
        startButton = new Button("START GAME");
        startButton.getStyleClass().add("game-button");
        startButton.setStyle("-fx-font-size: 24px; -fx-padding: 15 50 15 50;"); // Extra large for splash
        startButton.setOnAction(e -> startLoading());

        // 2. Loading Elements (Initially Hidden)
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setStyle("-fx-accent: #f39c12;"); // Gold loading bar

        progressLabel = new Label("Loading... 0%");
        progressLabel.setStyle(
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, black, 2, 0, 0, 0);");

        // Layout Container
        bottomBox = new VBox(20);
        bottomBox.setAlignment(Pos.BOTTOM_CENTER);
        bottomBox.setTranslateY(-80); // Lift up from bottom edge
        bottomBox.getChildren().add(startButton); // Start with button visible

        root.getChildren().add(bottomBox);
        StackPane.setAlignment(bottomBox, Pos.BOTTOM_CENTER);

        // Auto-play sound
        playSound("/assets/sounds/Clash Royale Intro Sound Effect.mp3");
    }

    private void startLoading() {
        // Switch UI
        bottomBox.getChildren().clear();
        bottomBox.getChildren().addAll(progressLabel, progressBar);

        // Animation: 0 to 100% over 2 seconds
        Timeline timeline = new Timeline();

        // Update progress every 50ms (40 steps in 2 seconds)
        // Total duration 2000ms. Step = 2000 / 40 = 50ms. Increment = 1.0 / 40 = 0.025
        int steps = 50;
        double durationMs = 2000;
        double increment = 1.0 / steps;

        for (int i = 0; i <= steps; i++) {
            double progress = i * increment;
            int percentage = (int) (progress * 100);
            KeyFrame frame = new KeyFrame(Duration.millis((durationMs / steps) * i), e -> {
                progressBar.setProgress(progress);
                progressLabel.setText("Loading... " + percentage + "%");
                if (percentage >= 100) {
                    // Ensure 100% text is seen briefly before switching?
                    // The last keyframe will trigger this.
                }
            });
            timeline.getKeyFrames().add(frame);
        }

        // After finishing
        timeline.setOnFinished(e -> {
            // Small delay at 100% for polish
            new Timeline(new KeyFrame(Duration.millis(200), ev -> navigator.showWelcomeScreen())).play();
        });

        timeline.play();
    }

    private void playSound(String resourcePath) {
        try {
            URL resource = getClass().getResource(resourcePath);
            if (resource != null) {
                AudioClip clip = new AudioClip(resource.toExternalForm());
                clip.play();
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
