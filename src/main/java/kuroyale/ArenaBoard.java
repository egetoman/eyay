package kuroyale;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.Bridge;
import kuroyale.domain.Position;
import kuroyale.domain.Tower;
import kuroyale.domain.TowerOwner;
import kuroyale.domain.TowerType;
import kuroyale.domain.Unit;

public class ArenaBoard {

    private static final double TILE_SIZE = 20.0;

    private final ArenaLayout layout;
    private final Canvas canvas;
    private final ScrollPane root;
    private Consumer<Position> tileSelectionListener;

    public ArenaBoard(ArenaLayout layout) {
        this.layout = layout;
        this.canvas = new Canvas(layout.getWidth() * TILE_SIZE, layout.getHeight() * TILE_SIZE);
        renderUnits(Collections.emptyList());
        this.canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleCanvasClick);

        StackPane canvasContainer = new StackPane(canvas);
        canvasContainer.setPadding(new Insets(15));
        canvasContainer.setStyle("-fx-background-color: #1b1e24;");

        ScrollPane scrollPane = new ScrollPane(canvasContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        this.root = scrollPane;
    }

    public Parent getView() {
        return root;
    }

    public void setOnTileSelected(Consumer<Position> listener) {
        this.tileSelectionListener = listener;
    }

    public void renderUnits(List<Unit> units) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawArena(gc, layout);
        drawUnits(gc, layout, units);
    }

    private void handleCanvasClick(MouseEvent event) {
        if (tileSelectionListener == null) {
            return;
        }
        double x = event.getX();
        double y = event.getY();
        int gridX = (int) Math.floor(x / TILE_SIZE);
        int gridY = layout.getHeight() - 1 - (int) Math.floor(y / TILE_SIZE);
        if (gridX < 0 || gridX >= layout.getWidth() || gridY < 0 || gridY >= layout.getHeight()) {
            return;
        }
        tileSelectionListener.accept(new Position(gridX, gridY));
    }

    private void drawArena(GraphicsContext gc, ArenaLayout layout) {
        double widthPx = layout.getWidth() * TILE_SIZE;
        double heightPx = layout.getHeight() * TILE_SIZE;

        gc.setFill(Color.web("#14303c"));
        gc.fillRect(0, 0, widthPx, heightPx);

        gc.setStroke(Color.web("#1f4d5f"));
        gc.setLineWidth(0.5);
        for (int x = 0; x <= layout.getWidth(); x++) {
            double px = x * TILE_SIZE;
            gc.strokeLine(px, 0, px, heightPx);
        }
        for (int y = 0; y <= layout.getHeight(); y++) {
            double py = y * TILE_SIZE;
            gc.strokeLine(0, py, widthPx, py);
        }

        double riverTop = (layout.getHeight() / 2.0 - 1) * TILE_SIZE;
        double riverHeight = TILE_SIZE * 2;
        gc.setFill(Color.web("#205e7a"));
        gc.fillRect(0, riverTop, widthPx, riverHeight);

        for (Bridge bridge : layout.getBridges()) {
            drawBridge(gc, layout, bridge);
        }

        for (Tower tower : layout.getTowers()) {
            drawTower(gc, layout, tower);
        }
    }

    private void drawBridge(GraphicsContext gc, ArenaLayout layout, Bridge bridge) {
        if (bridge.getStart() == null || bridge.getEnd() == null) {
            return;
        }
        int minX = Math.min(bridge.getStart().getX(), bridge.getEnd().getX());
        int maxX = Math.max(bridge.getStart().getX(), bridge.getEnd().getX());
        int minY = Math.min(bridge.getStart().getY(), bridge.getEnd().getY());
        int maxY = Math.max(bridge.getStart().getY(), bridge.getEnd().getY());

        double x = minX * TILE_SIZE;
        double y = convertY(layout, maxY);
        double width = (maxX - minX + 1) * TILE_SIZE;
        double height = (maxY - minY + 1) * TILE_SIZE;

        gc.setFill(Color.web("#a36a32"));
        gc.fillRect(x, y, width, height);
        gc.setStroke(Color.web("#6b3e18"));
        gc.setLineWidth(1.5);
        gc.strokeRect(x, y, width, height);
    }

    private void drawTower(GraphicsContext gc, ArenaLayout layout, Tower tower) {
        Position position = tower.getPosition();
        if (position == null) {
            return;
        }
        double x = position.getX() * TILE_SIZE;
        double y = convertY(layout, position.getY());

        Color fillColor = tower.getOwner() == TowerOwner.PLAYER
            ? Color.web("#3cb371")
            : Color.web("#f05a5b");
        gc.setFill(fillColor);

        double size = TILE_SIZE * (tower.getType() == TowerType.KING ? 1.2 : 0.9);
        double offset = (TILE_SIZE - size) / 2;
        gc.fillOval(x + offset, y + offset, size, size);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeOval(x + offset, y + offset, size, size);

        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, TILE_SIZE * 0.45));
        String label = tower.getType() == TowerType.KING ? "K" : "C";
        gc.fillText(label, x + TILE_SIZE * 0.35, y + TILE_SIZE * 0.65);
    }

    private void drawUnits(GraphicsContext gc, ArenaLayout layout, List<Unit> units) {
        if (units == null || units.isEmpty()) {
            return;
        }
        for (Unit unit : units) {
            Position position = unit.getPosition();
            if (position == null) {
                continue;
            }
            double drawX = (unit.getPreciseX()) * TILE_SIZE;
            double drawY = convertY(layout, unit.getPreciseY());
            Color fill = unit.getOwner() == TowerOwner.PLAYER
                ? Color.web("#8bed4a")
                : Color.web("#ff8a80");
            gc.setFill(fill);
            gc.fillOval(drawX + 4, drawY + 4, TILE_SIZE - 8, TILE_SIZE - 8);
            gc.setStroke(Color.web("#000000"));
            gc.setLineWidth(0.8);
            gc.strokeOval(drawX + 4, drawY + 4, TILE_SIZE - 8, TILE_SIZE - 8);
        }
    }

    private double convertY(ArenaLayout layout, int gridY) {
        return (layout.getHeight() - gridY - 1) * TILE_SIZE;
    }

    private double convertY(ArenaLayout layout, double gridY) {
        return (layout.getHeight() - gridY - 1) * TILE_SIZE;
    }
}

