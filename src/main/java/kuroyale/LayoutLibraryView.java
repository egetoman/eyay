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

        VBox left = new VBox(10);
        left.setPadding(new Insets(20));
        javafx.scene.control.Label title = new javafx.scene.control.Label("Saved Arenas");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        left.getChildren().addAll(title, listView, buttons);

        root = new BorderPane();
        root.setCenter(left);
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

