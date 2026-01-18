package kuroyale.emote;

import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

public class EmotePanel {
    private final StackPane root = new StackPane();
    private final TilePane grid = new TilePane();
    private final Consumer<EmoteType> onSelected;

    public EmotePanel(Consumer<EmoteType> onSelected) {
        this.onSelected = onSelected;
        root.getStyleClass().add("emote-panel");
        root.setPadding(new Insets(14));
        root.setVisible(false);
        root.setManaged(false);
        root.setMaxWidth(460);

        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPrefColumns(5);
        grid.setAlignment(Pos.CENTER);

        for (EmoteType type : EmoteType.values()) {
            grid.getChildren().add(buildTile(type));
        }

        root.getChildren().add(grid);
    }

    private Parent buildTile(EmoteType type) {
        Button button = new Button();
        button.getStyleClass().add("emote-tile");
        button.setPrefSize(86, 110);
        button.setOnAction(e -> {
            if (onSelected != null) {
                onSelected.accept(type);
            }
            hide();
        });

        VBox content = new VBox(6);
        content.setAlignment(Pos.CENTER);

        Image image = EmoteAssets.load(type);
        if (image != null) {
            ImageView icon = new ImageView(image);
            icon.setPreserveRatio(true);
            icon.setFitWidth(64);
            icon.setFitHeight(64);
            content.getChildren().add(icon);
        }

        Label label = new Label(type.getLabel());
        label.getStyleClass().add("emote-label");
        content.getChildren().add(label);

        button.setGraphic(content);
        return button;
    }

    public Parent getView() {
        return root;
    }

    public void toggle() {
        if (root.isVisible()) {
            hide();
        } else {
            show();
        }
    }

    public void show() {
        root.setVisible(true);
        root.setManaged(true);
    }

    public void hide() {
        root.setVisible(false);
        root.setManaged(false);
    }
}
