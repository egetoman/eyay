package kuroyale;

import application.ArenaLayoutService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.Bridge;
import kuroyale.domain.Position;
import kuroyale.domain.Tower;
import kuroyale.domain.TowerOwner;
import kuroyale.domain.TowerType;

public class ArenaDesignerView {

    private enum PlacementMode {
        TOWER,
        BRIDGE
    }

    private static final double TILE_SIZE = 24.0;
    private static final int REQUIRED_KING_TOWERS = 1;
    private static final int REQUIRED_CROWN_TOWERS = 2;
    private static final int MAX_BRIDGES = 3;
    private static final int MIN_BRIDGES = 1;
    private final ArenaLayoutService layoutService;
    private final ScreenNavigator navigator;
    private final ArenaLayout baseLayout;
    private final List<Tower> playerTowers;
    private final List<Bridge> bridges;
    private final BorderPane root;
    private final Canvas canvas;
    private final TextField nameField;
    private final ComboBox<TowerOwner> ownerCombo;
    private final ComboBox<TowerType> typeCombo;
    private final ComboBox<PlacementMode> modeCombo;
    private boolean dirty;

    public ArenaDesignerView(ScreenNavigator navigator, ArenaLayoutService layoutService) {
        this.navigator = navigator;
        this.layoutService = layoutService;
        this.baseLayout = layoutService.getActiveLayout();
        this.playerTowers = new ArrayList<>();
        this.bridges = new ArrayList<>();
        this.canvas = new Canvas(baseLayout.getWidth() * TILE_SIZE, baseLayout.getHeight() * TILE_SIZE);
        resetToBase();

        StackPane canvasWrapper = new StackPane(canvas);
        canvasWrapper.setPadding(new Insets(10));
        ScrollPane scrollPane = new ScrollPane(canvasWrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        nameField = new TextField();
        nameField.setPromptText("Arena name");
        nameField.setText("Custom Arena " + DateTimeFormatter.ofPattern("HHmmss").format(LocalDateTime.now()));

        ownerCombo = new ComboBox<>();
        ownerCombo.getItems().addAll(TowerOwner.PLAYER, TowerOwner.OPPONENT);
        ownerCombo.setValue(TowerOwner.PLAYER);
        ownerCombo.setDisable(true);

        typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(TowerType.KING, TowerType.CROWN);
        typeCombo.setValue(TowerType.CROWN);

        modeCombo = new ComboBox<>();
        modeCombo.getItems().addAll(PlacementMode.TOWER, PlacementMode.BRIDGE);
        modeCombo.setValue(PlacementMode.TOWER);

        VBox controls = new VBox(10,
            labelledBox("Layout name", nameField),
            labelledBox("Placement mode", modeCombo),
            labelledBox("Tower owner", ownerCombo),
            labelledBox("Tower type", typeCombo),
            buildButtons()
        );
        controls.setPadding(new Insets(15));
        controls.setPrefWidth(260);
        controls.setStyle("-fx-background-color: #1c1f26;");

        Label title = new Label("Arena Designer");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);
        VBox header = new VBox(title, new Label("Left click to add, right click to remove. Bridges snap across the river."));
        header.setSpacing(5);
        header.setPadding(new Insets(10));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #0f1216; -fx-text-fill: #d7d7d7;");
        header.getChildren().filtered(node -> node instanceof Label && node != title)
            .forEach(node -> ((Label) node).setTextFill(Color.web("#c9d1d9")));

        root = new BorderPane();
        root.setTop(header);
        root.setLeft(controls);
        root.setCenter(scrollPane);
        root.setStyle("-fx-background-color: #0b0e12;");

        canvas.setOnMouseClicked(event -> {
            Position grid = screenToGrid(event.getX(), event.getY());
            if (!isWithinBounds(grid)) {
                return;
            }
            if (event.getButton() == MouseButton.PRIMARY) {
                if (modeCombo.getValue() == PlacementMode.TOWER) {
                    placeTower(grid);
                } else {
                    placeBridge(grid);
                }
            } else if (event.getButton() == MouseButton.SECONDARY) {
                if (modeCombo.getValue() == PlacementMode.TOWER) {
                    removeTower(grid);
                } else {
                    removeBridge(grid);
                }
            }
        });
    }

    private HBox labelledBox(String labelText, Parent control) {
        Label label = new Label(labelText);
        label.setTextFill(Color.web("#c9d1d9"));
        VBox box = new VBox(label, control);
        box.setSpacing(4);
        return new HBox(box);
    }

    private HBox buildButtons() {
        Button saveButton = new Button("Save Layout");
        Button resetButton = new Button("Reset to Active Layout");
        Button backButton = new Button("Back");

        saveButton.setOnAction(e -> saveLayout());
        resetButton.setOnAction(e -> {
            resetToBase();
            draw();
        });
        backButton.setOnAction(e -> handleBackNavigation());

        return new HBox(10, saveButton, resetButton, backButton);
    }

    private void resetToBase() {
        playerTowers.clear();
        bridges.clear();
        for (Tower tower : baseLayout.getTowers()) {
            if (tower.getOwner() == TowerOwner.PLAYER) {
                Tower copy = new Tower(tower);
                copy.setOwner(TowerOwner.PLAYER);
                playerTowers.add(copy);
            }
        }
        for (Bridge bridge : baseLayout.getBridges()) {
            bridges.add(new Bridge(bridge));
        }
        dirty = false;
        draw();
    }

    private void placeTower(Position grid) {
        Position normalized = normalizeToPlayerSide(grid);
        if (!isWithinPlayerField(normalized)) {
            showWarning("Towers can only be placed on your side of the river.");
            return;
        }
        if (isOnBridgeTile(normalized)) {
            showWarning("You cannot place towers on top of bridges.");
            return;
        }

        Tower existing = findTowerAt(normalized);
        if (existing != null) {
            playerTowers.remove(existing);
        }

        TowerType selectedType = typeCombo.getValue();
        if (selectedType == TowerType.KING) {
            if (countTowersOfType(TowerType.KING) >= REQUIRED_KING_TOWERS) {
                showWarning("Only one King Tower is allowed.");
                if (existing != null) {
                    playerTowers.add(existing);
                }
                return;
            }
        } else {
            if (countTowersOfType(TowerType.CROWN) >= REQUIRED_CROWN_TOWERS) {
                showWarning("Only two Crown Towers are allowed.");
                if (existing != null) {
                    playerTowers.add(existing);
                }
                return;
            }
        }

        if (playerTowers.size() >= (REQUIRED_KING_TOWERS + REQUIRED_CROWN_TOWERS)) {
            showWarning("You already placed three towers. Remove one to reposition.");
            if (existing != null) {
                playerTowers.add(existing);
            }
            return;
        }

        int hp = selectedType == TowerType.KING ? 4000 : 2500;
        int damage = selectedType == TowerType.KING ? 250 : 140;
        Tower tower = new Tower(hp, normalized, damage, 1.0, selectedType, TowerOwner.PLAYER);
        playerTowers.add(tower);
        dirty = true;
        draw();
    }

    private void removeTower(Position grid) {
        boolean removed = false;
        Position normalized = normalizeToPlayerSide(grid);
        Iterator<Tower> iterator = playerTowers.iterator();
        while (iterator.hasNext()) {
            Tower existing = iterator.next();
            if (existing.getPosition() != null && existing.getPosition().sameTile(normalized)) {
                iterator.remove();
                removed = true;
            }
        }
        if (removed) {
            dirty = true;
            draw();
        }
    }

    private void placeBridge(Position grid) {
        int riverYStart = (baseLayout.getHeight() / 2) - 1;
        int riverYEnd = riverYStart + 1;
        int bridgeTop = riverYStart - 1;
        int bridgeBottom = riverYEnd + 1;
        Position target = normalizeBridgeCoordinate(grid);
        if (target.getY() < bridgeTop || target.getY() > bridgeBottom) {
            showWarning("Bridges must be placed across the river.");
            return;
        }
        int startX = Math.min(Math.max(target.getX(), 0), baseLayout.getWidth() - 2);

        Bridge newBridge = new Bridge(new Position(startX, bridgeTop), new Position(startX + 1, bridgeBottom), 2);
        removeBridge(target);
        if (bridges.size() >= MAX_BRIDGES) {
            showWarning("You can place at most " + MAX_BRIDGES + " bridges.");
            return;
        }
        bridges.add(newBridge);
        dirty = true;
        draw();
    }

    private void removeBridge(Position grid) {
        boolean removed = false;
        Position target = normalizeBridgeCoordinate(grid);
        Iterator<Bridge> iterator = bridges.iterator();
        while (iterator.hasNext()) {
            Bridge bridge = iterator.next();
            if (bridgeContains(bridge, target)) {
                iterator.remove();
                removed = true;
            }
        }
        if (removed) {
            dirty = true;
            draw();
        }
    }

    private boolean bridgeContains(Bridge bridge, Position grid) {
        if (bridge.getStart() == null || bridge.getEnd() == null) {
            return false;
        }
        int minX = Math.min(bridge.getStart().getX(), bridge.getEnd().getX());
        int maxX = Math.max(bridge.getStart().getX(), bridge.getEnd().getX());
        int minY = Math.min(bridge.getStart().getY(), bridge.getEnd().getY());
        int maxY = Math.max(bridge.getStart().getY(), bridge.getEnd().getY());
        return grid.getX() >= minX && grid.getX() <= maxX && grid.getY() >= minY && grid.getY() <= maxY;
    }

    private Position screenToGrid(double x, double y) {
        int gridX = (int) (x / TILE_SIZE);
        int gridY = baseLayout.getHeight() - 1 - (int) (y / TILE_SIZE);
        return new Position(gridX, gridY);
    }

    private boolean isWithinBounds(Position position) {
        return position.getX() >= 0 && position.getX() < baseLayout.getWidth()
            && position.getY() >= 0 && position.getY() < baseLayout.getHeight();
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double widthPx = baseLayout.getWidth() * TILE_SIZE;
        double heightPx = baseLayout.getHeight() * TILE_SIZE;
        gc.setFill(Color.web("#14222b"));
        gc.fillRect(0, 0, widthPx, heightPx);

        gc.setStroke(Color.web("#1f4d5f"));
        gc.setLineWidth(0.5);
        for (int x = 0; x <= baseLayout.getWidth(); x++) {
            double px = x * TILE_SIZE;
            gc.strokeLine(px, 0, px, heightPx);
        }
        for (int y = 0; y <= baseLayout.getHeight(); y++) {
            double py = y * TILE_SIZE;
            gc.strokeLine(0, py, widthPx, py);
        }

        double riverTop = (baseLayout.getHeight() / 2.0 - 1) * TILE_SIZE;
        gc.setFill(Color.web("#205e7a"));
        gc.fillRect(0, riverTop, widthPx, TILE_SIZE * 2);

        for (Bridge bridge : bridges) {
            drawBridge(gc, bridge);
        }
        for (Tower tower : buildMirroredTowers()) {
            drawTower(gc, tower);
        }
    }

    private void drawBridge(GraphicsContext gc, Bridge bridge) {
        if (bridge.getStart() == null || bridge.getEnd() == null) {
            return;
        }
        int minX = Math.min(bridge.getStart().getX(), bridge.getEnd().getX());
        int maxX = Math.max(bridge.getStart().getX(), bridge.getEnd().getX());
        int minY = Math.min(bridge.getStart().getY(), bridge.getEnd().getY());
        int maxY = Math.max(bridge.getStart().getY(), bridge.getEnd().getY());

        double x = minX * TILE_SIZE;
        double y = (baseLayout.getHeight() - maxY - 1) * TILE_SIZE;
        double width = (maxX - minX + 1) * TILE_SIZE;
        double height = (maxY - minY + 1) * TILE_SIZE;

        gc.setFill(Color.web("#a36a32"));
        gc.fillRect(x, y, width, height);
        gc.setStroke(Color.web("#6b3e18"));
        gc.setLineWidth(1.5);
        gc.strokeRect(x, y, width, height);
    }

    private void drawTower(GraphicsContext gc, Tower tower) {
        Position pos = tower.getPosition();
        if (pos == null) {
            return;
        }
        double x = pos.getX() * TILE_SIZE;
        double y = (baseLayout.getHeight() - pos.getY() - 1) * TILE_SIZE;
        Color color = tower.getOwner() == TowerOwner.PLAYER ? Color.web("#3cb371") : Color.web("#f05a5b");
        double size = TILE_SIZE * (tower.getType() == TowerType.KING ? 1.2 : 0.9);
        double offset = (TILE_SIZE - size) / 2;

        gc.setFill(color);
        gc.fillOval(x + offset, y + offset, size, size);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeOval(x + offset, y + offset, size, size);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, TILE_SIZE * 0.45));
        gc.setFill(Color.WHITE);
        gc.fillText(tower.getType() == TowerType.KING ? "K" : "C", x + TILE_SIZE * 0.35, y + TILE_SIZE * 0.65);
    }

    private boolean isOnBridgeTile(Position grid) {
        for (Bridge bridge : bridges) {
            if (bridgeContains(bridge, grid)) {
                return true;
            }
        }
        int riverYStart = (baseLayout.getHeight() / 2) - 1;
        int riverYEnd = riverYStart + 1;
        return grid.getY() == riverYStart || grid.getY() == riverYEnd;
    }

    private Tower findTowerAt(Position grid) {
        for (Tower tower : playerTowers) {
            if (tower.getPosition() != null && tower.getPosition().sameTile(grid)) {
                return tower;
            }
        }
        return null;
    }

    private List<Tower> buildMirroredTowers() {
        List<Tower> all = new ArrayList<>();
        for (Tower tower : playerTowers) {
            Tower playerCopy = new Tower(tower);
            playerCopy.setOwner(TowerOwner.PLAYER);
            all.add(playerCopy);

            Position pos = tower.getPosition();
            Position mirroredPos = new Position(pos.getX(), baseLayout.getHeight() - pos.getY() - 1);
            Tower mirror = new Tower(tower);
            mirror.setOwner(TowerOwner.OPPONENT);
            mirror.setPosition(mirroredPos);
            all.add(mirror);
        }
        return all;
    }

    private int riverStartRow() {
        return (baseLayout.getHeight() / 2) - 1;
    }

    private int riverEndRow() {
        return riverStartRow() + 1;
    }

    private boolean isWithinPlayerField(Position grid) {
        return grid.getY() <= riverStartRow() - 1;
    }

    private Position normalizeToPlayerSide(Position grid) {
        if (grid.getY() <= riverStartRow() - 1) {
            return grid;
        }
        if (grid.getY() >= riverEndRow() + 1) {
            int mirroredY = baseLayout.getHeight() - grid.getY() - 1;
            return new Position(grid.getX(), mirroredY);
        }
        return grid;
    }

    private Position normalizeBridgeCoordinate(Position grid) {
        if (grid.getY() > riverEndRow()) {
            int mirroredY = baseLayout.getHeight() - grid.getY() - 1;
            return new Position(grid.getX(), mirroredY);
        }
        return grid;
    }

    private long countTowersOfType(TowerType type) {
        return playerTowers.stream().filter(t -> t.getType() == type).count();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (root.getScene() != null) {
            alert.initOwner(root.getScene().getWindow());
        }
        alert.initModality(Modality.WINDOW_MODAL);
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait();
    }

    private void handleBackNavigation() {
        if (!dirty) {
            navigator.showWelcomeScreen();
            return;
        }
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Layout");
        alert.setHeaderText("You forgot to save the layout.");
        ButtonType saveButton = new ButtonType("Save");
        ButtonType discardButton = new ButtonType("Don't save and continue");
        alert.getButtonTypes().setAll(saveButton, discardButton);
        if (root.getScene() != null) {
            alert.initOwner(root.getScene().getWindow());
        }
        alert.initModality(Modality.WINDOW_MODAL);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty()) {
            return;
        }
        if (result.get() == saveButton) {
            if (saveLayout()) {
                navigator.showWelcomeScreen();
            }
        } else if (result.get() == discardButton) {
            navigator.showWelcomeScreen();
        }
    }

    private boolean saveLayout() {
        if (nameField.getText().isBlank()) {
            Alert alert = new Alert(AlertType.WARNING, "Please enter a layout name.", ButtonType.OK);
            if (root.getScene() != null) {
                alert.initOwner(root.getScene().getWindow());
            }
            alert.initModality(Modality.WINDOW_MODAL);
            alert.showAndWait();
            return false;
        }
        long kingCount = countTowersOfType(TowerType.KING);
        long crownCount = countTowersOfType(TowerType.CROWN);
        if (kingCount != REQUIRED_KING_TOWERS || crownCount != REQUIRED_CROWN_TOWERS) {
            showWarning("Layout must have exactly " + REQUIRED_KING_TOWERS + " King Tower and "
                + REQUIRED_CROWN_TOWERS + " Crown Towers. "
                + "Currently: " + kingCount + " King / " + crownCount + " Crown.");
            return false;
        }
        if (bridges.size() < MIN_BRIDGES || bridges.size() > MAX_BRIDGES) {
            showWarning("Layout must have between " + MIN_BRIDGES + " and " + MAX_BRIDGES + " bridges. "
                + "Currently: " + bridges.size());
            return false;
        }

        List<Tower> mirrored = new ArrayList<>();
        for (Tower tower : buildMirroredTowers()) {
            mirrored.add(new Tower(tower));
        }
        List<Bridge> bridgeCopies = new ArrayList<>();
        for (Bridge bridge : bridges) {
            bridgeCopies.add(new Bridge(bridge));
        }
        ArenaLayout layout = ArenaLayout.of(java.util.UUID.randomUUID().toString(), nameField.getText().trim(),
            baseLayout.getWidth(), baseLayout.getHeight(), mirrored, bridgeCopies);
        layoutService.save(layout);
        dirty = false;
        Alert alert = new Alert(AlertType.INFORMATION, "Layout saved.", ButtonType.OK);
        if (root.getScene() != null) {
            alert.initOwner(root.getScene().getWindow());
        }
        alert.initModality(Modality.WINDOW_MODAL);
        alert.showAndWait();
        return true;
    }

    public Parent getRoot() {
        return root;
    }
}

