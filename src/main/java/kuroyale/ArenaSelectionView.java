package kuroyale;

import application.ArenaLayoutService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import kuroyale.domain.ArenaLayout;

public class ArenaSelectionView {

    private final ScreenNavigator navigator;
    private final ArenaLayoutService layoutService;
    private final ObservableList<ArenaLayout> layouts;
    private final BorderPane root;

    public ArenaSelectionView(ScreenNavigator navigator, ArenaLayoutService layoutService) {
        this.navigator = navigator;
        this.layoutService = layoutService;
        this.layouts = FXCollections.observableArrayList(layoutService.getLayouts());

        Label title = new Label("Choose Arena Layout");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));

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
        listView.getSelectionModel().select(layoutService.getActiveLayout());

        Button useButton = new Button("Use Selected Layout");
        Button previewButton = new Button("Preview");
        Button backButton = new Button("Back");

        useButton.setOnAction(e -> {
            ArenaLayout selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                layoutService.setActiveLayout(selected.getId());
                navigator.showStartGameScreen();
            }
        });
        previewButton.setOnAction(e -> {
            ArenaLayout selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                navigator.showArenaPreview(selected, () -> navigator.showArenaSelection());
            }
        });
        backButton.setOnAction(e -> navigator.showWelcomeScreen());

        VBox controls = new VBox(10, title, listView, useButton, previewButton, backButton);
        controls.setPadding(new Insets(20));
        controls.setAlignment(Pos.CENTER);

        root = new BorderPane();
        root.setCenter(controls);
    }

    public Parent getRoot() {
        return root;
    }
}

