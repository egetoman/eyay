package kuroyale.emote;

import java.util.ArrayDeque;
import java.util.Deque;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class EmoteBubbleManager {
    private final Pane layer;
    private final Deque<EmoteType> topQueue = new ArrayDeque<>();
    private final Deque<EmoteType> bottomQueue = new ArrayDeque<>();
    private boolean topShowing = false;
    private boolean bottomShowing = false;

    public EmoteBubbleManager(Pane layer) {
        this.layer = layer;
    }

    public void showForTop(EmoteType type) {
        enqueue(type, false);
    }

    public void showForBottom(EmoteType type) {
        enqueue(type, true);
    }

    private void enqueue(EmoteType type, boolean bottom) {
        if (type == null || layer == null) {
            return;
        }
        if (bottom) {
            if (bottomShowing) {
                if (bottomQueue.size() < 3) {
                    bottomQueue.addLast(type);
                }
                return;
            }
            bottomShowing = true;
        } else {
            if (topShowing) {
                if (topQueue.size() < 3) {
                    topQueue.addLast(type);
                }
                return;
            }
            topShowing = true;
        }
        playBubble(type, bottom);
    }

    private void playBubble(EmoteType type, boolean bottom) {
        StackPane bubble = buildBubble(type);
        bubble.setOpacity(0);
        bubble.setScaleX(0.6);
        bubble.setScaleY(0.6);
        positionBubble(bubble, bottom);
        layer.getChildren().add(bubble);

        ScaleTransition scale = new ScaleTransition(Duration.millis(140), bubble);
        scale.setToX(1.0);
        scale.setToY(1.0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(140), bubble);
        fadeIn.setToValue(1.0);

        PauseTransition hold = new PauseTransition(Duration.seconds(1.2));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), bubble);
        fadeOut.setToValue(0);

        SequentialTransition seq = new SequentialTransition(
                new ParallelTransition(scale, fadeIn),
                hold,
                fadeOut);

        seq.setOnFinished(e -> {
            layer.getChildren().remove(bubble);
            if (bottom) {
                bottomShowing = false;
                if (!bottomQueue.isEmpty()) {
                    showForBottom(bottomQueue.pollFirst());
                }
            } else {
                topShowing = false;
                if (!topQueue.isEmpty()) {
                    showForTop(topQueue.pollFirst());
                }
            }
        });
        seq.play();
    }

    private StackPane buildBubble(EmoteType type) {
        StackPane bubble = new StackPane();
        bubble.getStyleClass().add("emote-bubble");
        bubble.setPadding(new Insets(10, 14, 10, 14));
        bubble.setAlignment(Pos.CENTER);

        Node content;
        if (type.isTextEmote()) {
            Label label = new Label(type.getDisplayText());
            label.getStyleClass().add("emote-bubble-text");
            content = label;
        } else {
            Image image = EmoteAssets.load(type);
            if (image != null) {
                ImageView icon = new ImageView(image);
                icon.setPreserveRatio(true);
                icon.setFitWidth(84);
                icon.setFitHeight(84);
                content = icon;
            } else {
                Label fallback = new Label(type.getLabel());
                fallback.getStyleClass().add("emote-bubble-text");
                content = fallback;
            }
        }
        bubble.getChildren().add(content);
        return bubble;
    }

    private void positionBubble(StackPane bubble, boolean bottom) {
        double width = layer.getWidth() > 0 ? layer.getWidth() : 1000;
        double height = layer.getHeight() > 0 ? layer.getHeight() : 700;
        double x = width / 2.0 - 70;
        double y = bottom ? height * 0.72 : height * 0.18;
        bubble.setLayoutX(x);
        bubble.setLayoutY(y);
    }
}
