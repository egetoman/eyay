package kuroyale;

import application.ArenaLayoutService;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import kuroyale.domain.ArenaLayout;

public class LayoutLibraryView {

    private final ArenaLayoutService layoutService;
    private final ScreenNavigator navigator;
    private final BorderPane root;
    private final ObservableList<ArenaLayout> layouts;

    public LayoutLibraryView(ScreenNavigator navigator, ArenaLayoutService layoutService) {
        this.navigator = navigator;
        this.layoutService = layoutService;
        this.layouts = FXCollections.observableArrayList();
        refresh();

        ListView<ArenaLayout> listView = new ListView<>(layouts);
        listView.getStyleClass().add("saved-arenas-list"); // UI-only change: list styling
        listView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ArenaLayout item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        Button previewButton = new Button("Preview");
        Button deleteButton = new Button("Delete");
        Button backButton = new Button("Back");
        previewButton.getStyleClass().add("saved-arenas-primary");
        deleteButton.getStyleClass().add("saved-arenas-danger");
        backButton.getStyleClass().add("saved-arenas-secondary");

        previewButton.setOnAction(e -> {
            ArenaLayout selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                navigator.showArenaPreview(selected, this::refreshAndStay);
            }
        });
        deleteButton.setOnAction(e -> {
            ArenaLayout selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete layout \"" + selected.getName() + "\"?");
            Optional<ButtonType> decision = confirm.showAndWait();
            if (decision.isPresent() && decision.get() == ButtonType.OK) {
                layoutService.delete(selected.getId());
                refresh();
                listView.setItems(layouts);
            }
        });
        backButton.setOnAction(e -> navigator.showWelcomeScreen());

        HBox buttons = new HBox(10, previewButton, deleteButton, backButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(10));
        buttons.getStyleClass().add("saved-arenas-actions"); // UI-only change: button hierarchy

        VBox left = new VBox(12);
        left.setPadding(new Insets(18)); // UI-only change: spacing
        javafx.scene.control.Label title = new javafx.scene.control.Label("Saved Arenas");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.getStyleClass().add("saved-arenas-title");
        javafx.scene.control.Label subtitle = new javafx.scene.control.Label("Select an arena to preview or delete");
        subtitle.getStyleClass().add("saved-arenas-subtitle");
        left.getStyleClass().add("saved-arenas-panel"); // UI-only change: panel styling
        left.getChildren().addAll(title, subtitle, listView, buttons);

        root = new BorderPane();
        root.setCenter(left);
        root.getStyleClass().add("saved-arenas-root");
    }

    private void refresh() {
        layouts.setAll(layoutService.getLayouts());
    }

    private void refreshAndStay() {
        refresh();
        navigator.showLayoutLibrary();
    }

    public Parent getRoot() {
        return root;
    }
}

