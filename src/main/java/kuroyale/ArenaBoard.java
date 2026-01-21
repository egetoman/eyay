package kuroyale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.image.Image;
import javafx.geometry.Point2D;
import kuroyale.domain.Arena;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.Bridge;
import kuroyale.domain.Position;
import kuroyale.domain.Tower;
import kuroyale.domain.TowerOwner;
import kuroyale.domain.TowerType;
import kuroyale.domain.Unit;

public class ArenaBoard {

    private static final double DEFAULT_TILE_SIZE = 26.0;
    private static final int SPRITE_FRAME_SIZE = 100; // Tiny RPG pack frames are 100x100
    private static final boolean ENABLE_UNIT_SPRITES = true;
    private static final long ATTACK_HOLD_MILLIS = 450;
    private static final long ATTACK_TRIGGER_COOLDOWN_MILLIS = 500;
    private static final double MOVE_EPSILON_TILES = 0.03;

    private static final Image SOLDIER_IDLE = load("/assets/tiny-rpg/soldier_idle.png");
    private static final Image SOLDIER_WALK = load("/assets/tiny-rpg/soldier_walk.png");
    private static final Image SOLDIER_ATTACK1 = load("/assets/tiny-rpg/soldier_attack1.png");
    private static final Image ORC_IDLE = load("/assets/tiny-rpg/orc_idle.png");
    private static final Image ORC_WALK = load("/assets/tiny-rpg/orc_walk.png");
    private static final Image ORC_ATTACK1 = load("/assets/tiny-rpg/orc_attack1.png");

    private final ArenaLayout layout;
    private final Canvas canvas;
    private final StackPane canvasContainer;
    private final ScrollPane root;
    private double tileSize = DEFAULT_TILE_SIZE;
    private final Pane overlayLayer;
    private Consumer<Position> tileSelectionListener;
    private final boolean flipVertical;
    private List<Tower> currentTowers = Collections.emptyList();
    private final Map<Unit, UnitAnimState> animByUnit = new WeakHashMap<>();
    private final List<SpellEffect> activeSpellEffects = new ArrayList<>();
    private final List<DeployEffect> activeDeployEffects = new ArrayList<>();

    public ArenaBoard(ArenaLayout layout) {
        this(layout, false);
    }

    /**
     * @param flipVertical If true, render the arena from the opposite vertical
     *                     perspective (used for Network PvP player 2).
     */
    public ArenaBoard(ArenaLayout layout, boolean flipVertical) {
        this.layout = layout;
        this.flipVertical = flipVertical;
        this.tileSize = DEFAULT_TILE_SIZE;
        this.canvas = new Canvas(layout.getWidth() * tileSize, layout.getHeight() * tileSize);
        this.overlayLayer = new Pane();
        this.overlayLayer.setPickOnBounds(false);
        this.overlayLayer.setMouseTransparent(true);
        this.overlayLayer.prefWidthProperty().bind(canvas.widthProperty());
        this.overlayLayer.prefHeightProperty().bind(canvas.heightProperty());
        renderUnits(Collections.emptyList());
        this.canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleCanvasClick);

        this.canvasContainer = new StackPane(canvas, overlayLayer);
        canvasContainer.setPadding(new Insets(8));
        canvasContainer.setStyle("-fx-background-color: #1b1e24;");

        ScrollPane scrollPane = new ScrollPane(canvasContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.root = scrollPane;

        // Auto-resize arena to fit available space when viewport changes
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            if (newBounds != null && newBounds.getHeight() > 100) {
                resizeToFit(newBounds.getWidth(), newBounds.getHeight());
            }
        });
    }

    /**
     * Resizes the arena to fit within the given dimensions while maintaining aspect
     * ratio.
     */
    private void resizeToFit(double availableWidth, double availableHeight) {
        // Account for padding (8px on each side)
        double usableHeight = availableHeight - 16;
        double usableWidth = availableWidth - 16;

        // Calculate tile size to fit height (height is usually the limiting factor)
        double tileSizeForHeight = usableHeight / layout.getHeight();
        double tileSizeForWidth = usableWidth / layout.getWidth();

        // Use the smaller to ensure it fits both dimensions
        double newTileSize = Math.min(tileSizeForHeight, tileSizeForWidth);

        // Clamp to reasonable range (minimum 14 for readability, maximum 128 for
        // performance/size)
        newTileSize = Math.max(14.0, Math.min(128.0, newTileSize));

        // Only resize if change is significant (avoid infinite loops)
        if (Math.abs(newTileSize - tileSize) > 0.5) {
            tileSize = newTileSize;
            canvas.setWidth(layout.getWidth() * tileSize);
            canvas.setHeight(layout.getHeight() * tileSize);
        }
    }

    public Parent getView() {
        return root;
    }

    public Pane getOverlayLayer() {
        return overlayLayer;
    }

    public Point2D getTileCenterPx(Position position) {
        if (position == null) {
            return null;
        }
        double x = position.getX() * tileSize + (tileSize / 2.0);
        double y = convertY(layout, position.getY()) + (tileSize / 2.0);
        return new Point2D(x, y);
    }

    public void setOnTileSelected(Consumer<Position> listener) {
        this.tileSelectionListener = listener;
    }

    public void renderUnits(List<Unit> units) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        currentTowers = layout.getTowers();
        drawArena(gc, layout);
        drawUnits(gc, layout, units);
        drawSpellEffects(gc);
        drawDeployEffects(gc);
    }

    /**
     * Records a spell cast for visual effect rendering.
     * 
     * @param spellId  The ID of the spell card (e.g., "card_fireball",
     *                 "card_arrows")
     * @param position The target position where the spell was cast
     */
    public void recordSpellCast(String spellId, Position position) {
        if (spellId == null || position == null) {
            return;
        }
        activeSpellEffects.add(new SpellEffect(spellId, position, System.currentTimeMillis()));
    }

    public void recordDeployEffect(TowerOwner owner, Position position) {
        if (position == null) {
            return;
        }
        activeDeployEffects.add(new DeployEffect(position, owner, System.currentTimeMillis()));
    }

    public void render(Arena arena) {
        if (arena == null) {
            renderUnits(Collections.emptyList());
            return;
        }
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawArenaBase(gc, layout);
        currentTowers = arena.getTowers() != null ? arena.getTowers() : layout.getTowers();
        drawLiveTowers(gc, layout, currentTowers);
        drawUnits(gc, layout, arena.getUnits());
        drawSpellEffects(gc);
        drawDeployEffects(gc);
    }

    private void handleCanvasClick(MouseEvent event) {
        if (tileSelectionListener == null) {
            return;
        }
        double x = event.getX();
        double y = event.getY();
        int gridX = (int) Math.floor(x / tileSize);
        int gridY;
        if (flipVertical) {
            // Opponent perspective: top of screen is global bottom
            gridY = (int) Math.floor(y / tileSize);
        } else {
            // Normal: top of screen is global top
            gridY = layout.getHeight() - 1 - (int) Math.floor(y / tileSize);
        }
        if (gridX < 0 || gridX >= layout.getWidth() || gridY < 0 || gridY >= layout.getHeight()) {
            return;
        }
        tileSelectionListener.accept(new Position(gridX, gridY));
    }

    private void drawArenaBase(GraphicsContext gc, ArenaLayout layout) {
        double widthPx = layout.getWidth() * tileSize;
        double heightPx = layout.getHeight() * tileSize;
        double riverTop = (layout.getHeight() / 2.0 - 1) * tileSize;
        double riverHeight = tileSize * 2;

        // Draw grass background for player side (top/north)
        gc.setFill(new javafx.scene.paint.LinearGradient(
                0, 0, 0, riverTop,
                false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#6bb85a")),
                new javafx.scene.paint.Stop(1, Color.web("#4a9e3d"))));
        gc.fillRect(0, 0, widthPx, riverTop);

        // Draw grass background for opponent side (bottom/south)
        gc.setFill(new javafx.scene.paint.LinearGradient(
                0, riverTop + riverHeight, 0, heightPx,
                false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#4a9e3d")),
                new javafx.scene.paint.Stop(1, Color.web("#3d7a2e"))));
        gc.fillRect(0, riverTop + riverHeight, widthPx, heightPx - (riverTop + riverHeight));

        // Draw river with gradient
        gc.setFill(new javafx.scene.paint.LinearGradient(
                0, riverTop, 0, riverTop + riverHeight,
                false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#5ba3d4")),
                new javafx.scene.paint.Stop(0.5, Color.web("#3d8ac4")),
                new javafx.scene.paint.Stop(1, Color.web("#2d6fa8"))));
        gc.fillRect(0, riverTop, widthPx, riverHeight);

        // Draw subtle grid lines
        gc.setStroke(Color.web("#3d7a2e", 0.3));
        gc.setLineWidth(0.5);
        for (int x = 0; x <= layout.getWidth(); x++) {
            double px = x * tileSize;
            gc.strokeLine(px, 0, px, heightPx);
        }
        for (int y = 0; y <= layout.getHeight(); y++) {
            double py = y * tileSize;
            gc.strokeLine(0, py, widthPx, py);
        }

        // Draw bridges
        for (Bridge bridge : layout.getBridges()) {
            drawBridge(gc, layout, bridge);
        }
    }

    private void drawArena(GraphicsContext gc, ArenaLayout layout) {
        drawArenaBase(gc, layout);
        for (Tower tower : layout.getTowers()) {
            drawTower(gc, layout, tower);
        }
    }

    private void drawLiveTowers(GraphicsContext gc, ArenaLayout layout, List<Tower> towers) {
        if (towers == null) {
            return;
        }
        for (Tower tower : towers) {
            drawTower(gc, layout, tower);
            drawTowerShot(gc, layout, tower);
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

        double x = minX * tileSize;
        // Use the same coordinate system as the river (which doesn't use convertY)
        // River is at (layout.getHeight() / 2.0 - 1) * tileSize
        // Bridge should extend from one tile above river to one tile below
        double riverTop = (layout.getHeight() / 2.0 - 1) * tileSize;
        double y = riverTop - tileSize; // Start one tile above river
        double width = (maxX - minX + 1) * tileSize;
        double height = (maxY - minY + 1) * tileSize;

        // Shadow underneath
        gc.setFill(Color.web("#000000", 0.3));
        gc.fillRect(x + 2, y + 2, width, height);

        // Wooden bridge with gradient
        gc.setFill(new javafx.scene.paint.LinearGradient(
                0, y, 0, y + height,
                false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#a87f5a")),
                new javafx.scene.paint.Stop(1, Color.web("#8b6f47"))));
        gc.fillRect(x, y, width, height);

        // Wood planks (horizontal lines)
        gc.setStroke(Color.web("#6b4423", 0.4));
        gc.setLineWidth(1);
        for (int i = 1; i < (maxY - minY + 1); i++) {
            double plankY = y + (i * tileSize);
            gc.strokeLine(x, plankY, x + width, plankY);
        }

        // Border
        gc.setStroke(Color.web("#5a3a1f"));
        gc.setLineWidth(2);
        gc.strokeRect(x, y, width, height);
    }

    private void drawTower(GraphicsContext gc, ArenaLayout layout, Tower tower) {
        Position position = tower.getPosition();
        if (position == null) {
            return;
        }
        // Draw destroyed visuals when tower is down (visual only, no gameplay effect)
        if (tower.getHp() <= 0) {
            drawDestroyedTower(gc, layout, tower);
            return;
        }
        double x = position.getX() * tileSize;
        double y = convertY(layout, position.getY());

        boolean friendly = (!flipVertical && tower.getOwner() == TowerOwner.PLAYER)
                || (flipVertical && tower.getOwner() == TowerOwner.OPPONENT);

        boolean isKing = tower.getType() == TowerType.KING;
        double towerWidth = tileSize * (isKing ? 1.6 : 1.3);
        double towerHeight = tileSize * (isKing ? 2.0 : 1.7);
        double centerX = x + tileSize / 2;
        double towerX = centerX - towerWidth / 2;
        double towerY = y + tileSize - towerHeight;

        // Shadow
        gc.setFill(Color.web("#000000", 0.4));
        gc.fillOval(towerX - 2, y + tileSize - 4, towerWidth + 4, 8);

        // Foundation base (wider, darker)
        double baseWidth = towerWidth + 8;
        double baseHeight = 8;
        double baseX = centerX - baseWidth / 2;
        double baseY = y + tileSize - baseHeight;

        gc.setFill(new javafx.scene.paint.LinearGradient(
                0, baseY, 0, baseY + baseHeight,
                false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#3a3a3a")),
                new javafx.scene.paint.Stop(1, Color.web("#2a2a2a"))));
        gc.fillRect(baseX, baseY, baseWidth, baseHeight);
        gc.setStroke(Color.web("#1a1a1a"));
        gc.setLineWidth(1);
        gc.strokeRect(baseX, baseY, baseWidth, baseHeight);

        // Tower body (stone texture)
        Color bodyColor1 = friendly ? Color.web("#6b7a8a") : Color.web("#8a6b6b");
        Color bodyColor2 = friendly ? Color.web("#5a6a7a") : Color.web("#7a5a7a");

        gc.setFill(new javafx.scene.paint.LinearGradient(
                towerX, towerY, towerX + towerWidth, towerY,
                false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, bodyColor1),
                new javafx.scene.paint.Stop(0.5, bodyColor2),
                new javafx.scene.paint.Stop(1, bodyColor1)));
        gc.fillRect(towerX, towerY, towerWidth, towerHeight - baseHeight);

        // Stone texture (horizontal lines)
        gc.setStroke(Color.web("#000000", 0.15));
        gc.setLineWidth(1);
        for (int i = 1; i < 6; i++) {
            double lineY = towerY + (towerHeight - baseHeight) * i / 6;
            gc.strokeLine(towerX, lineY, towerX + towerWidth, lineY);
        }

        // Team color accent stripe
        double accentY = towerY + (towerHeight - baseHeight) * 0.4;
        double accentHeight = 6;
        gc.setFill(friendly ? Color.web("#4a90e2", 0.6) : Color.web("#e74c3c", 0.6));
        gc.fillRect(towerX, accentY, towerWidth, accentHeight);

        // Windows (arrow slits)
        gc.setFill(Color.web("#1a1a1a"));
        double windowWidth = 3;
        double windowHeight = 10;
        double window1X = towerX + towerWidth * 0.3 - windowWidth / 2;
        double window2X = towerX + towerWidth * 0.7 - windowWidth / 2;
        double windowY = towerY + (towerHeight - baseHeight) * 0.6;
        gc.fillRect(window1X, windowY, windowWidth, windowHeight);
        gc.fillRect(window2X, windowY, windowWidth, windowHeight);

        // Tower body border
        gc.setStroke(Color.web("#1a1a1a"));
        gc.setLineWidth(2);
        gc.strokeRect(towerX, towerY, towerWidth, towerHeight - baseHeight);

        // Battlements (crenellations)
        double battlementHeight = 8;
        double battlementY = towerY;
        Color battlementColor = friendly ? Color.web("#8a9aaa") : Color.web("#aa8a8a");

        gc.setFill(battlementColor);
        // Draw crenellated pattern
        int numMerlons = isKing ? 5 : 4;
        double merlonWidth = towerWidth / (numMerlons * 2);
        for (int i = 0; i < numMerlons; i++) {
            double merlonX = towerX + (i * 2) * merlonWidth;
            gc.fillRect(merlonX, battlementY, merlonWidth, battlementHeight);
        }

        // Battlement border
        gc.setStroke(Color.web("#1a1a1a"));
        gc.setLineWidth(1);
        for (int i = 0; i < numMerlons; i++) {
            double merlonX = towerX + (i * 2) * merlonWidth;
            gc.strokeRect(merlonX, battlementY, merlonWidth, battlementHeight);
        }

        // Crown icon on top
        gc.setFill(isKing ? Color.web("#ffd700") : Color.web("#c0c0c0"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, isKing ? 20 : 16));
        gc.setTextAlign(TextAlignment.CENTER);
        String crownIcon = isKing ? "♔" : "♕";
        gc.fillText(crownIcon, centerX, towerY - 2);

        // Health bar
        double barWidth = towerWidth + 4;
        double barHeight = 8;
        double barX = centerX - barWidth / 2;
        double barY = y + tileSize + 6;

        gc.setFill(Color.web("#2a2a2a"));
        gc.fillRoundRect(barX, barY, barWidth, barHeight, 4, 4);

        double healthPercent = tower.getMaxHp() > 0 ? (double) tower.getHp() / tower.getMaxHp() : 0;
        healthPercent = Math.max(0, Math.min(1, healthPercent));

        Color healthColor;
        if (healthPercent > 0.6) {
            healthColor = Color.web("#4caf50");
        } else if (healthPercent > 0.3) {
            healthColor = Color.web("#ff9800");
        } else {
            healthColor = Color.web("#f44336");
        }

        gc.setFill(healthColor);
        gc.fillRoundRect(barX, barY, barWidth * healthPercent, barHeight, 4, 4);

        gc.setStroke(Color.web("#000000"));
        gc.setLineWidth(1);
        gc.strokeRoundRect(barX, barY, barWidth, barHeight, 4, 4);

        // Health text
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        gc.setTextAlign(TextAlignment.CENTER);
        String healthText = tower.getHp() + "/" + tower.getMaxHp();
        gc.fillText(healthText, centerX, barY + barHeight + 13);
    }

    private void drawTowerShot(GraphicsContext gc, ArenaLayout layout, Tower tower) {
        if (gc == null || tower == null || tower.getPosition() == null) {
            return;
        }
        Position target = tower.getLastTargetPosition();
        if (target == null) {
            return;
        }
        long age = System.currentTimeMillis() - tower.getLastFiredAtMs();
        if (age < 0 || age > 180) {
            return;
        }

        double alpha = 0.9 * (1.0 - (age / 180.0));
        double startX = tower.getPosition().getX() * tileSize + tileSize / 2.0;
        double startY = convertY(layout, tower.getPosition().getY()) + tileSize / 2.0;
        double endX = target.getX() * tileSize + tileSize / 2.0;
        double endY = convertY(layout, target.getY()) + tileSize / 2.0;

        Color beam = tower.getOwner() == TowerOwner.PLAYER
                ? Color.web("#6aa7ff", alpha)
                : Color.web("#ff7a7a", alpha);

        gc.setStroke(beam);
        gc.setLineWidth(2.5);
        gc.strokeLine(startX, startY, endX, endY);

        gc.setFill(beam.deriveColor(0, 1, 1, alpha * 0.8));
        gc.fillOval(endX - 3, endY - 3, 6, 6);
    }

    private void drawDestroyedTower(GraphicsContext gc, ArenaLayout layout, Tower tower) {
        if (gc == null || tower == null || tower.getPosition() == null) {
            return;
        }
        Position position = tower.getPosition();
        double x = position.getX() * tileSize;
        double y = convertY(layout, position.getY());

        boolean friendly = (!flipVertical && tower.getOwner() == TowerOwner.PLAYER)
                || (flipVertical && tower.getOwner() == TowerOwner.OPPONENT);

        double rubbleWidth = tileSize * 1.4;
        double rubbleHeight = tileSize * 0.7;
        double centerX = x + tileSize / 2;
        double rubbleX = centerX - rubbleWidth / 2;
        double rubbleY = y + tileSize - rubbleHeight;

        // Shadow
        gc.setFill(Color.web("#000000", 0.35));
        gc.fillOval(rubbleX - 2, rubbleY + rubbleHeight - 4, rubbleWidth + 4, 8);

        // Rubble base
        gc.setFill(Color.web("#2f333d"));
        gc.fillRoundRect(rubbleX, rubbleY, rubbleWidth, rubbleHeight, 6, 6);

        // Cracked blocks
        gc.setStroke(Color.web("#1b1f26"));
        gc.setLineWidth(1);
        gc.strokeRoundRect(rubbleX, rubbleY, rubbleWidth, rubbleHeight, 6, 6);
        gc.strokeLine(rubbleX + 6, rubbleY + rubbleHeight * 0.4, rubbleX + rubbleWidth - 6,
                rubbleY + rubbleHeight * 0.35);
        gc.strokeLine(rubbleX + 10, rubbleY + rubbleHeight * 0.7, rubbleX + rubbleWidth - 12,
                rubbleY + rubbleHeight * 0.55);

        // Banner scrap
        Color scrap = friendly ? Color.web("#2a6fd2") : Color.web("#b63a4c");
        gc.setFill(scrap.deriveColor(0, 1, 1, 0.65));
        gc.fillRect(rubbleX + rubbleWidth * 0.15, rubbleY + 4, rubbleWidth * 0.2, 6);

        // Broken crown icon
        gc.setFill(Color.web("#9aa3ad"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("♛", centerX, rubbleY - 2);
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
            double drawX = (unit.getPreciseX()) * tileSize;
            double drawY = convertY(layout, unit.getPreciseY());
            boolean friendly = (!flipVertical && unit.getOwner() == TowerOwner.PLAYER)
                    || (flipVertical && unit.getOwner() == TowerOwner.OPPONENT);

            if (ENABLE_UNIT_SPRITES) {
                drawUnitSprite(gc, unit, drawX, drawY, friendly, units, currentTowers);
            } else {
                // Fallback circle rendering - draw larger for better visibility
                Color fill = friendly ? Color.web("#8bed4a") : Color.web("#ff8a80");
                gc.setFill(fill);
                double circleSize = tileSize * 2.5;
                double circleOffset = (tileSize - circleSize) / 2;
                gc.fillOval(drawX + circleOffset, drawY + circleOffset, circleSize, circleSize);
                gc.setStroke(Color.web("#000000"));
                gc.setLineWidth(1.2);
                gc.strokeOval(drawX + circleOffset, drawY + circleOffset, circleSize, circleSize);
            }

            // Health Bar logic
            int maxHp = unit.getCard() != null && unit.getCard().getStats() != null
                    ? unit.getCard().getStats().getHp()
                    : unit.getCurrentHP();
            if (maxHp <= 0)
                maxHp = 1;

            double hpPercent = (double) unit.getCurrentHP() / maxHp;
            hpPercent = Math.max(0, Math.min(1.0, hpPercent));

            double barWidth = tileSize;
            double barHeight = 4;
            double barX = drawX;
            double barY = drawY - 6;

            // Background
            gc.setFill(Color.web("#2a2a2a"));
            gc.fillRect(barX, barY, barWidth, barHeight);

            // Foreground
            gc.setFill(friendly ? Color.web("#4caf50") : Color.web("#f44336"));
            gc.fillRect(barX, barY, barWidth * hpPercent, barHeight);

            // Border
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(0.5);
            gc.strokeRect(barX, barY, barWidth, barHeight);

            // Unit/Card name label below the unit
            String unitName = unit.getCard() != null ? unit.getCard().getName() : "Unit";
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 9));
            gc.setTextAlign(TextAlignment.CENTER);
            double nameCenterX = drawX + tileSize / 2;
            double nameY = drawY + tileSize + 10;
            // Drop shadow for readability
            gc.setFill(Color.web("#000000", 0.7));
            gc.fillText(unitName, nameCenterX + 1, nameY + 1);
            gc.setFill(Color.WHITE);
            gc.fillText(unitName, nameCenterX, nameY);
        }
    }

    private void drawUnitSprite(GraphicsContext gc,
            Unit unit,
            double drawX,
            double drawY,
            boolean friendly,
            List<Unit> units,
            List<Tower> towers) {
        if (gc == null || unit == null) {
            return;
        }

        UnitAnimState st = animByUnit.computeIfAbsent(unit, u -> new UnitAnimState(u.getPreciseX(), u.getPreciseY()));
        long now = System.currentTimeMillis();
        double deltaX = unit.getPreciseX() - st.lastX;
        double deltaY = unit.getPreciseY() - st.lastY;
        double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        boolean moving = dist > MOVE_EPSILON_TILES;
        st.lastX = unit.getPreciseX();
        st.lastY = unit.getPreciseY();

        boolean attacking = false;
        double unitRange = getUnitRange(unit);
        boolean isRanged = unitRange > 2.0;

        if (!moving) {
            // Find the actual target for ranged attack visualization
            Position targetPos = findAttackTarget(unit, units, towers);
            boolean inRange = targetPos != null;
            boolean cooldownReady = (now - st.lastAttackAtMs) >= ATTACK_TRIGGER_COOLDOWN_MILLIS;
            if (inRange && cooldownReady) {
                st.lastAttackAtMs = now;
                if (targetPos != null) {
                    st.targetX = targetPos.getX();
                    st.targetY = targetPos.getY();
                    st.hasTarget = true;
                }
            }
            attacking = inRange && (now - st.lastAttackAtMs) <= ATTACK_HOLD_MILLIS;
        }

        Image sheet = pickSheet(friendly, moving, attacking);
        if (sheet == null) {
            return;
        }

        int frames = Math.max(1, (int) Math.floor(sheet.getWidth() / SPRITE_FRAME_SIZE));
        long t = System.currentTimeMillis();
        int frame = (int) ((t / 140) % frames);

        double sx = frame * SPRITE_FRAME_SIZE;
        double sy = 0;
        double sw = SPRITE_FRAME_SIZE;
        double sh = Math.min(SPRITE_FRAME_SIZE, sheet.getHeight());

        // Draw larger than a single tile for better visibility.
        double dw = tileSize * 4.0;
        double dh = tileSize * 4.0;
        double dx = drawX + (tileSize - dw) / 2;
        double dy = drawY + (tileSize - dh) / 2;

        gc.drawImage(sheet, sx, sy, sw, sh, dx, dy, dw, dh);

        // Draw projectile for ranged units when attacking
        if (attacking && isRanged && st.hasTarget) {
            drawRangedProjectile(gc, unit, drawX, drawY, st, friendly);
        }
    }

    private double getUnitRange(Unit unit) {
        if (unit == null || unit.getCard() == null || unit.getCard().getStats() == null) {
            return 1.5;
        }
        return unit.getCard().getStats().getRange() / 10.0;
    }

    private Position findAttackTarget(Unit unit, List<Unit> units, List<Tower> towers) {
        if (unit == null || unit.getOwner() == null || unit.getPosition() == null) {
            return null;
        }
        double range = getUnitRange(unit);
        TowerOwner myOwner = unit.getOwner();
        double ux = unit.getPreciseX();
        double uy = unit.getPreciseY();

        // Find nearest enemy unit in range
        if (units != null) {
            for (Unit other : units) {
                if (other == null || other == unit || other.isDefeated() || other.getOwner() == myOwner) {
                    continue;
                }
                double ddx = other.getPreciseX() - ux;
                double ddy = other.getPreciseY() - uy;
                if (Math.sqrt(ddx * ddx + ddy * ddy) <= range) {
                    return new Position((int) other.getPreciseX(), (int) other.getPreciseY());
                }
            }
        }
        // Find nearest enemy tower in range
        if (towers != null) {
            for (Tower t : towers) {
                if (t == null || t.isDestroyed() || t.getOwner() == null || t.getPosition() == null
                        || t.getOwner() == myOwner) {
                    continue;
                }
                double ddx = t.getPosition().getX() - ux;
                double ddy = t.getPosition().getY() - uy;
                if (Math.sqrt(ddx * ddx + ddy * ddy) <= range) {
                    return t.getPosition();
                }
            }
        }
        return null;
    }

    private void drawRangedProjectile(GraphicsContext gc, Unit unit, double unitDrawX, double unitDrawY,
            UnitAnimState st, boolean friendly) {
        if (gc == null || unit == null || !st.hasTarget) {
            return;
        }

        // Calculate projectile animation progress (0 to 1)
        long now = System.currentTimeMillis();
        long attackTime = now - st.lastAttackAtMs;
        double progress = Math.min(1.0, attackTime / (double) ATTACK_HOLD_MILLIS);

        // Source position (center of unit)
        double srcX = unitDrawX + tileSize / 2;
        double srcY = unitDrawY + tileSize / 2;

        // Target position
        double targetDrawX = st.targetX * tileSize + tileSize / 2;
        double targetDrawY = convertY(layout, st.targetY) + tileSize / 2;

        // Interpolate projectile position
        double projX = srcX + (targetDrawX - srcX) * progress;
        double projY = srcY + (targetDrawY - srcY) * progress;

        double angle = Math.atan2(targetDrawY - srcY, targetDrawX - srcX);
        double trailLength = 10;
        double trailX = projX - Math.cos(angle) * trailLength;
        double trailY = projY - Math.sin(angle) * trailLength;
        Color baseColor = friendly ? Color.web("#4a90e2") : Color.web("#e74c3c");

        // Draw projectile based on unit type
        String cardId = unit.getCard() != null ? unit.getCard().getId() : "";

        if ("card_bomber".equals(cardId) || "card_bomb_tower".equals(cardId)) {
            // Draw bomb projectile
            gc.setStroke(Color.web("#000000", 0.35));
            gc.setLineWidth(3);
            gc.strokeLine(trailX, trailY, projX, projY);
            gc.setFill(Color.web("#444444", 0.4));
            gc.fillOval(trailX - 3, trailY - 3, 6, 6);

            gc.setFill(Color.web("#333333"));
            gc.fillOval(projX - 5, projY - 5, 10, 10);
            gc.setFill(Color.web("#ff6600"));
            gc.fillOval(projX - 2, projY - 6, 4, 4); // Fuse spark
        } else if ("card_wizard".equals(cardId)) {
            // Draw fireball
            gc.setStroke(Color.web("#ff6600", 0.45));
            gc.setLineWidth(4);
            gc.strokeLine(trailX, trailY, projX, projY);
            gc.setFill(Color.web("#ffbb33", 0.35));
            gc.fillOval(projX - 8, projY - 8, 16, 16);

            gc.setFill(Color.web("#ff4400", 0.8));
            gc.fillOval(projX - 6, projY - 6, 12, 12);
            gc.setFill(Color.web("#ffaa00", 0.6));
            gc.fillOval(projX - 4, projY - 4, 8, 8);
        } else if ("card_musketeer".equals(cardId) || "card_archers".equals(cardId)
                || "card_spear_goblins".equals(cardId)) {
            // Draw arrow/bullet
            gc.setStroke(Color.web("#ffffff", 0.35));
            gc.setLineWidth(3);
            gc.strokeLine(trailX, trailY, projX, projY);

            gc.save();
            gc.translate(projX, projY);
            gc.rotate(Math.toDegrees(angle));
            gc.setFill(baseColor);
            gc.fillRect(-8, -2, 16, 4);
            gc.setFill(Color.web("#ffd700"));
            gc.fillPolygon(new double[] { 8, 8, 14 }, new double[] { -3, 3, 0 }, 3);
            gc.restore();
        } else {
            // Default projectile (small circle)
            gc.setStroke(Color.web("#ffffff", 0.3));
            gc.setLineWidth(2.5);
            gc.strokeLine(trailX, trailY, projX, projY);
            gc.setFill(Color.web("#ffffff", 0.2));
            gc.fillOval(projX - 6, projY - 6, 12, 12);
            gc.setFill(baseColor);
            gc.fillOval(projX - 4, projY - 4, 8, 8);
        }
    }

    private Image pickSheet(boolean friendly, boolean moving, boolean attacking) {
        if (friendly) {
            if (attacking && SOLDIER_ATTACK1 != null) {
                return SOLDIER_ATTACK1;
            }
            if (moving && SOLDIER_WALK != null) {
                return SOLDIER_WALK;
            }
            return SOLDIER_IDLE != null ? SOLDIER_IDLE : SOLDIER_WALK;
        }
        if (attacking && ORC_ATTACK1 != null) {
            return ORC_ATTACK1;
        }
        if (moving && ORC_WALK != null) {
            return ORC_WALK;
        }
        return ORC_IDLE != null ? ORC_IDLE : ORC_WALK;
    }

    private static Image load(String resourcePath) {
        try (var in = ArenaBoard.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            return new Image(in);
        } catch (Exception e) {
            return null;
        }
    }

    private static final class UnitAnimState {
        private double lastX;
        private double lastY;
        private long lastAttackAtMs;
        private double targetX;
        private double targetY;
        private boolean hasTarget;

        private UnitAnimState(double lastX, double lastY) {
            this.lastX = lastX;
            this.lastY = lastY;
            this.lastAttackAtMs = 0;
            this.hasTarget = false;
        }
    }

    private double convertY(ArenaLayout layout, int gridY) {
        if (flipVertical) {
            return gridY * tileSize;
        }
        return (layout.getHeight() - gridY - 1) * tileSize;
    }

    private double convertY(ArenaLayout layout, double gridY) {
        if (flipVertical) {
            return gridY * tileSize;
        }
        return (layout.getHeight() - gridY - 1) * tileSize;
    }

    private void drawSpellEffects(GraphicsContext gc) {
        if (gc == null || activeSpellEffects.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<SpellEffect> it = activeSpellEffects.iterator();

        while (it.hasNext()) {
            SpellEffect effect = it.next();
            long age = now - effect.castTime;

            // Remove effects older than 1.5 seconds
            if (age > 1500) {
                it.remove();
                continue;
            }

            // Calculate position in pixels
            double centerX = effect.position.getX() * tileSize + tileSize / 2.0;
            double centerY = convertY(layout, effect.position.getY()) + tileSize / 2.0;

            // Draw spell effect based on type
            drawSpellEffect(gc, effect.spellId, centerX, centerY, age);
        }
    }

    private void drawDeployEffects(GraphicsContext gc) {
        if (gc == null || activeDeployEffects.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<DeployEffect> it = activeDeployEffects.iterator();
        while (it.hasNext()) {
            DeployEffect effect = it.next();
            long age = now - effect.castTime;
            if (age > 650) {
                it.remove();
                continue;
            }
            double progress = Math.min(1.0, age / 650.0);
            double centerX = effect.position.getX() * tileSize + tileSize / 2.0;
            double centerY = convertY(layout, effect.position.getY()) + tileSize / 2.0;

            boolean friendly = (!flipVertical && effect.owner == TowerOwner.PLAYER)
                    || (flipVertical && effect.owner == TowerOwner.OPPONENT);
            Color ring = friendly ? Color.web("#5aa2ff") : Color.web("#ff6f6f");
            double radius = 6 + (progress * 18);
            double alpha = 0.7 * (1.0 - progress);

            gc.setStroke(ring.deriveColor(0, 1, 1, alpha));
            gc.setLineWidth(2.5);
            gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

            gc.setFill(ring.deriveColor(0, 1, 1, alpha * 0.35));
            gc.fillOval(centerX - radius * 0.6, centerY - radius * 0.6, radius * 1.2, radius * 1.2);
        }
    }

    private void drawSpellEffect(GraphicsContext gc, String spellId, double centerX, double centerY, long ageMs) {
        if (gc == null || spellId == null) {
            return;
        }

        // Calculate animation progress (0 to 1)
        double progress = Math.min(1.0, ageMs / 1500.0);
        double fadeOut = 1.0 - progress; // Fade out over time

        if ("card_fireball".equals(spellId)) {
            drawFireballEffect(gc, centerX, centerY, ageMs, fadeOut);
        } else if ("card_arrows".equals(spellId)) {
            drawArrowsEffect(gc, centerX, centerY, ageMs, fadeOut);
        } else if ("card_zap".equals(spellId)) {
            drawZapEffect(gc, centerX, centerY, ageMs, fadeOut);
        } else if ("card_rocket".equals(spellId)) {
            drawRocketEffect(gc, centerX, centerY, ageMs, fadeOut);
        }
    }

    private void drawFireballEffect(GraphicsContext gc, double centerX, double centerY, long ageMs, double fadeOut) {
        // Advanced multi-layered fireball explosion with particles, shockwaves, and
        // animated effects
        // UI-only change: add distinct ground sigil for fireball
        if (ageMs < 900) {
            double sigilRadius = 18 + (ageMs * 0.02);
            double sigilAlpha = Math.max(0, 0.35 * (1.0 - ageMs / 900.0) * fadeOut);
            gc.setStroke(Color.web("#ffb347", sigilAlpha));
            gc.setLineWidth(2);
            gc.strokeOval(centerX - sigilRadius, centerY - sigilRadius, sigilRadius * 2, sigilRadius * 2);
            gc.setStroke(Color.web("#ff5f2e", sigilAlpha * 0.7));
            gc.setLineWidth(1.2);
            gc.strokeOval(centerX - sigilRadius * 0.6, centerY - sigilRadius * 0.6, sigilRadius * 1.2,
                    sigilRadius * 1.2);
        }

        // Phase 1: Initial flash (0-100ms)
        if (ageMs < 100) {
            double flashIntensity = 1.0 - (ageMs / 100.0);
            gc.setFill(Color.web("#ffffff", flashIntensity * 0.9 * fadeOut));
            gc.fillOval(centerX - 25, centerY - 25, 50, 50);
        }

        // Shockwave rings (expanding outward)
        double shockwaveSpeed = 0.8;
        for (int ring = 0; ring < 3; ring++) {
            double ringRadius = (ageMs * shockwaveSpeed) - (ring * 30);
            if (ringRadius > 0 && ringRadius < 60) {
                double ringAlpha = Math.max(0, 0.4 * (1.0 - ringRadius / 60.0) * fadeOut);
                gc.setStroke(Color.web("#ff8800", ringAlpha));
                gc.setLineWidth(3 - ring);
                gc.strokeOval(centerX - ringRadius, centerY - ringRadius, ringRadius * 2, ringRadius * 2);
            }
        }

        // Main explosion - expanding with easing
        double easeOut = 1.0 - Math.pow(1.0 - Math.min(ageMs / 800.0, 1.0), 3);
        double radius = 8 + (easeOut * 35);

        // Outer fire ring with gradient effect
        for (int layer = 0; layer < 5; layer++) {
            double layerRadius = radius * (1.0 - layer * 0.15);
            double layerAlpha = (0.5 - layer * 0.08) * fadeOut;
            Color layerColor = Color.web(
                    String.format("#ff%02x00", 68 - layer * 8),
                    layerAlpha);
            gc.setFill(layerColor);
            gc.fillOval(centerX - layerRadius, centerY - layerRadius, layerRadius * 2, layerRadius * 2);
        }

        // Inner core with pulsating effect
        double pulse = 0.8 + 0.2 * Math.sin(ageMs / 30.0);
        double coreRadius = radius * 0.35 * pulse;
        gc.setFill(Color.web("#ffff00", 0.95 * fadeOut));
        gc.fillOval(centerX - coreRadius, centerY - coreRadius, coreRadius * 2, coreRadius * 2);

        // Bright white center
        gc.setFill(Color.web("#ffffff", 0.8 * fadeOut));
        gc.fillOval(centerX - coreRadius * 0.5, centerY - coreRadius * 0.5, coreRadius, coreRadius);

        // Particle sparks with varying sizes and velocities
        if (ageMs < 500) {
            int numParticles = 16;
            for (int i = 0; i < numParticles; i++) {
                double angle = (i * Math.PI * 2 / numParticles) + (ageMs / 80.0);
                double particleSpeed = 0.3 + (i % 3) * 0.1;
                double particleDist = radius * 0.6 + (ageMs * particleSpeed);
                double particleX = centerX + Math.cos(angle) * particleDist;
                double particleY = centerY + Math.sin(angle) * particleDist;

                // Varying particle sizes
                double particleSize = 2 + (i % 3);
                double particleFade = Math.max(0, 1.0 - (particleDist / 50.0)) * fadeOut;

                // Color gradient from yellow to orange
                Color particleColor = Color.web(
                        i % 2 == 0 ? "#ffff00" : "#ff8800",
                        particleFade * 0.8);
                gc.setFill(particleColor);
                gc.fillOval(particleX - particleSize, particleY - particleSize, particleSize * 2, particleSize * 2);
            }
        }

        // Smoke/debris particles (appear after initial explosion)
        if (ageMs > 150 && ageMs < 800) {
            int numSmoke = 8;
            for (int i = 0; i < numSmoke; i++) {
                double angle = (i * Math.PI * 2 / numSmoke) + (ageMs / 120.0);
                double smokeDist = radius * 0.4 + ((ageMs - 150) * 0.15);
                double smokeX = centerX + Math.cos(angle) * smokeDist;
                double smokeY = centerY + Math.sin(angle) * smokeDist;
                double smokeSize = 4 + (ageMs / 100.0);
                double smokeAlpha = Math.max(0, 0.4 * (1.0 - (ageMs - 150) / 650.0) * fadeOut);

                gc.setFill(Color.web("#333333", smokeAlpha));
                gc.fillOval(smokeX - smokeSize, smokeY - smokeSize, smokeSize * 2, smokeSize * 2);
            }
        }
    }

    private void drawArrowsEffect(GraphicsContext gc, double centerX, double centerY, long ageMs, double fadeOut) {
        // Advanced arrows rain effect with multiple impact points, trails, and debris
        // UI-only change: add distinct target marker for arrows
        if (ageMs < 800) {
            double markerRadius = 14 + (ageMs * 0.015);
            double markerAlpha = Math.max(0, 0.3 * (1.0 - ageMs / 800.0) * fadeOut);
            gc.setStroke(Color.web("#ffd166", markerAlpha));
            gc.setLineWidth(1.8);
            gc.strokeOval(centerX - markerRadius, centerY - markerRadius, markerRadius * 2, markerRadius * 2);
            gc.setStroke(Color.web("#ef476f", markerAlpha * 0.7));
            gc.strokeLine(centerX - markerRadius, centerY, centerX + markerRadius, centerY);
            gc.strokeLine(centerX, centerY - markerRadius, centerX, centerY + markerRadius);
        }

        // Expanding impact radius
        double easeOut = 1.0 - Math.pow(1.0 - Math.min(ageMs / 600.0, 1.0), 2);
        double radius = 12 + (easeOut * 28);

        // Number of arrow impacts (more arrows = more coverage)
        int numArrows = 8;

        // Draw arrow impacts with staggered timing
        for (int i = 0; i < numArrows; i++) {
            double angle = (i * Math.PI * 2 / numArrows);
            double staggerDelay = i * 20; // Stagger each arrow by 20ms
            double adjustedAge = Math.max(0, ageMs - staggerDelay);

            if (adjustedAge < 0)
                continue;

            // Arrow impact position (spread in circular pattern)
            double dist = radius * (0.4 + (i % 3) * 0.2);
            double arrowX = centerX + Math.cos(angle) * dist;
            double arrowY = centerY + Math.sin(angle) * dist;

            // Arrow trail (visible during first 100ms)
            if (adjustedAge < 100) {
                double trailLength = 12 + (adjustedAge / 5.0);
                double trailEndX = arrowX - Math.cos(angle) * trailLength;
                double trailEndY = arrowY - Math.sin(angle) * trailLength;

                // Trail with gradient
                gc.setStroke(Color.web("#ffaa00", 0.6 * fadeOut));
                gc.setLineWidth(2);
                gc.strokeLine(trailEndX, trailEndY, arrowX, arrowY);

                // Trail glow
                gc.setStroke(Color.web("#ffff00", 0.3 * fadeOut));
                gc.setLineWidth(4);
                gc.strokeLine(trailEndX, trailEndY, arrowX, arrowY);
            }

            // Impact flash (brief bright circle)
            if (adjustedAge < 80) {
                double flashSize = 6 + (adjustedAge / 10.0);
                double flashAlpha = (1.0 - adjustedAge / 80.0) * fadeOut;
                gc.setFill(Color.web("#ffffff", flashAlpha));
                gc.fillOval(arrowX - flashSize, arrowY - flashSize, flashSize * 2, flashSize * 2);
            }

            // Impact circle (expanding then fading)
            double impactSize = 3 + Math.min(adjustedAge / 15.0, 6);
            double impactAlpha = Math.max(0, (1.0 - adjustedAge / 300.0) * 0.7 * fadeOut);
            gc.setFill(Color.web("#ffaa00", impactAlpha));
            gc.fillOval(arrowX - impactSize, arrowY - impactSize, impactSize * 2, impactSize * 2);

            // Debris particles (small sparks flying outward)
            if (adjustedAge < 200) {
                for (int j = 0; j < 3; j++) {
                    double debrisAngle = angle + (j - 1) * 0.3;
                    double debrisDist = impactSize + (adjustedAge * 0.2);
                    double debrisX = arrowX + Math.cos(debrisAngle) * debrisDist;
                    double debrisY = arrowY + Math.sin(debrisAngle) * debrisDist;
                    double debrisAlpha = (1.0 - adjustedAge / 200.0) * fadeOut;

                    gc.setFill(Color.web("#ffff00", debrisAlpha * 0.6));
                    gc.fillOval(debrisX - 1.5, debrisY - 1.5, 3, 3);
                }
            }
        }

        // Central impact zone (larger area effect)
        double centralRadius = 6 + (easeOut * 12);
        double centralAlpha = Math.max(0, (1.0 - ageMs / 500.0) * 0.5 * fadeOut);

        // Outer ring
        gc.setFill(Color.web("#ffaa00", centralAlpha));
        gc.fillOval(centerX - centralRadius, centerY - centralRadius, centralRadius * 2, centralRadius * 2);

        // Inner bright core
        double coreRadius = centralRadius * 0.5;
        gc.setFill(Color.web("#ffff00", centralAlpha * 1.5));
        gc.fillOval(centerX - coreRadius, centerY - coreRadius, coreRadius * 2, coreRadius * 2);

        // Shockwave ring
        if (ageMs < 400) {
            double shockwaveRadius = radius * 0.8 + (ageMs * 0.2);
            double shockwaveAlpha = Math.max(0, 0.3 * (1.0 - ageMs / 400.0) * fadeOut);
            gc.setStroke(Color.web("#ffaa00", shockwaveAlpha));
            gc.setLineWidth(2);
            gc.strokeOval(centerX - shockwaveRadius, centerY - shockwaveRadius, shockwaveRadius * 2,
                    shockwaveRadius * 2);
        }
    }

    private void drawZapEffect(GraphicsContext gc, double centerX, double centerY, long ageMs, double fadeOut) {
        // Advanced lightning zap effect with multiple bolts, electric arcs, and pulsing
        // energy
        // UI-only change: add electric burst rings for distinct zap identity
        if (ageMs < 450) {
            double burstRadius = 10 + (ageMs * 0.05);
            double burstAlpha = Math.max(0, 0.35 * (1.0 - ageMs / 450.0) * fadeOut);
            gc.setStroke(Color.web("#22d3ee", burstAlpha));
            gc.setLineWidth(2);
            gc.strokeOval(centerX - burstRadius, centerY - burstRadius, burstRadius * 2, burstRadius * 2);
            gc.setStroke(Color.web("#e0f2fe", burstAlpha * 0.7));
            gc.setLineWidth(1.2);
            gc.strokeOval(centerX - burstRadius * 0.65, centerY - burstRadius * 0.65, burstRadius * 1.3,
                    burstRadius * 1.3);
        }

        // Phase 1: Initial bright flash (0-50ms)
        if (ageMs < 50) {
            double flashIntensity = 1.0 - (ageMs / 50.0);
            gc.setFill(Color.web("#ffffff", flashIntensity * fadeOut));
            gc.fillOval(centerX - 20, centerY - 20, 40, 40);
        }

        // Pulsing central core
        double pulse = 0.7 + 0.3 * Math.sin(ageMs / 25.0);
        double coreRadius = 8 * pulse;

        // Bright yellow-white core
        gc.setFill(Color.web("#ffff00", 0.95 * fadeOut));
        gc.fillOval(centerX - coreRadius, centerY - coreRadius, coreRadius * 2, coreRadius * 2);

        // White hot center
        gc.setFill(Color.web("#ffffff", 0.9 * fadeOut));
        gc.fillOval(centerX - coreRadius * 0.5, centerY - coreRadius * 0.5, coreRadius, coreRadius);

        // Electric field rings (expanding outward)
        for (int ring = 0; ring < 4; ring++) {
            double ringRadius = 10 + (ageMs * 0.15) + (ring * 8);
            if (ringRadius < 35) {
                double ringAlpha = Math.max(0, 0.4 * (1.0 - ringRadius / 35.0) * fadeOut);
                double ringPhase = (ageMs / 30.0) + (ring * Math.PI / 4);

                // Animated electric ring with segments
                gc.setStroke(Color.web("#00ffff", ringAlpha));
                gc.setLineWidth(2);

                // Draw segmented ring (electric arcs)
                int segments = 8;
                for (int seg = 0; seg < segments; seg++) {
                    double segAngle1 = (seg * Math.PI * 2 / segments) + ringPhase;
                    double segAngle2 = ((seg + 1) * Math.PI * 2 / segments) + ringPhase;
                    double x1 = centerX + Math.cos(segAngle1) * ringRadius;
                    double y1 = centerY + Math.sin(segAngle1) * ringRadius;
                    double x2 = centerX + Math.cos(segAngle2) * ringRadius;
                    double y2 = centerY + Math.sin(segAngle2) * ringRadius;

                    // Only draw some segments (flickering effect)
                    if ((seg + (int) (ageMs / 50)) % 2 == 0) {
                        gc.strokeLine(x1, y1, x2, y2);
                    }
                }
            }
        }

        // Main lightning bolts (zigzag patterns radiating outward)
        if (ageMs < 350) {
            int numBolts = 6;
            for (int i = 0; i < numBolts; i++) {
                double angle = (i * Math.PI * 2 / numBolts) + (ageMs / 100.0);
                double boltLength = 15 + (ageMs * 0.1);
                double endX = centerX + Math.cos(angle) * boltLength;
                double endY = centerY + Math.sin(angle) * boltLength;

                // Main bolt (bright white)
                drawLightningBolt(gc, centerX, centerY, endX, endY, Color.web("#ffffff", fadeOut), 2.5);

                // Secondary bolt (cyan, slightly offset)
                double offsetAngle = angle + 0.2;
                double offsetEndX = centerX + Math.cos(offsetAngle) * boltLength * 0.8;
                double offsetEndY = centerY + Math.sin(offsetAngle) * boltLength * 0.8;
                drawLightningBolt(gc, centerX, centerY, offsetEndX, offsetEndY, Color.web("#00ffff", fadeOut * 0.6),
                        1.5);
            }
        }

        // Electric sparks (small particles)
        if (ageMs < 300) {
            int numSparks = 12;
            for (int i = 0; i < numSparks; i++) {
                double sparkAngle = (i * Math.PI * 2 / numSparks) + (ageMs / 60.0);
                double sparkDist = 8 + (ageMs * 0.12);
                double sparkX = centerX + Math.cos(sparkAngle) * sparkDist;
                double sparkY = centerY + Math.sin(sparkAngle) * sparkDist;
                double sparkSize = 1.5 + (i % 2) * 0.5;
                double sparkAlpha = Math.max(0, (1.0 - ageMs / 300.0) * fadeOut);

                gc.setFill(Color.web("#ffff00", sparkAlpha));
                gc.fillOval(sparkX - sparkSize, sparkY - sparkSize, sparkSize * 2, sparkSize * 2);
            }
        }

        // Stun effect indicator (pulsing rings)
        if (ageMs < 500) {
            double stunRadius = 12 + Math.sin(ageMs / 40.0) * 2;
            double stunAlpha = Math.max(0, 0.3 * (1.0 - ageMs / 500.0) * fadeOut);
            gc.setStroke(Color.web("#00ffff", stunAlpha));
            gc.setLineWidth(2);
            gc.strokeOval(centerX - stunRadius, centerY - stunRadius, stunRadius * 2, stunRadius * 2);
        }
    }

    private void drawLightningBolt(GraphicsContext gc, double startX, double startY, double endX, double endY,
            Color color, double lineWidth) {
        gc.setStroke(color);
        gc.setLineWidth(lineWidth);

        // Create jagged lightning path with deterministic "randomness"
        double distance = Math.sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY));
        int segments = Math.max(3, (int) (distance / 4));

        double prevX = startX;
        double prevY = startY;
        double angle = Math.atan2(endY - startY, endX - startX);

        // Use deterministic pseudo-randomness based on position
        double seed = (startX + startY) * 17.0 + (endX + endY) * 23.0;

        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;
            double baseX = startX + (endX - startX) * t;
            double baseY = startY + (endY - startY) * t;

            // Add deterministic offset perpendicular to the line
            double perpAngle = angle + Math.PI / 2;
            // Pseudo-random offset using sine of seed + segment
            double pseudoRandom = Math.sin(seed + i * 7.3) * 0.5 + 0.5;
            double offset = (pseudoRandom - 0.5) * 4 * (1.0 - t * 0.5); // Less offset near end
            double x = baseX + Math.cos(perpAngle) * offset;
            double y = baseY + Math.sin(perpAngle) * offset;

            gc.strokeLine(prevX, prevY, x, y);
            prevX = x;
            prevY = y;
        }

        // Final segment to end point
        gc.strokeLine(prevX, prevY, endX, endY);
    }

    private void drawRocketEffect(GraphicsContext gc, double centerX, double centerY, long ageMs, double fadeOut) {
        // Advanced rocket explosion - massive, multi-stage explosion with debris,
        // shockwaves, and smoke
        // UI-only change: add crater ring to distinguish rocket impact
        if (ageMs < 1200) {
            double craterRadius = 22 + (ageMs * 0.03);
            double craterAlpha = Math.max(0, 0.28 * (1.0 - ageMs / 1200.0) * fadeOut);
            gc.setStroke(Color.web("#6b4f3f", craterAlpha));
            gc.setLineWidth(2.4);
            gc.strokeOval(centerX - craterRadius, centerY - craterRadius, craterRadius * 2, craterRadius * 2);
            gc.setStroke(Color.web("#c08457", craterAlpha * 0.6));
            gc.setLineWidth(1.4);
            gc.strokeOval(centerX - craterRadius * 0.6, centerY - craterRadius * 0.6, craterRadius * 1.2,
                    craterRadius * 1.2);
        }

        // Phase 1: Massive initial flash (0-80ms)
        if (ageMs < 80) {
            double flashIntensity = 1.0 - (ageMs / 80.0);
            gc.setFill(Color.web("#ffffff", flashIntensity * 0.95 * fadeOut));
            gc.fillOval(centerX - 35, centerY - 35, 70, 70);
        }

        // Multiple shockwave rings (expanding rapidly)
        double shockwaveSpeed = 1.2;
        for (int wave = 0; wave < 5; wave++) {
            double waveRadius = (ageMs * shockwaveSpeed) - (wave * 25);
            if (waveRadius > 0 && waveRadius < 80) {
                double waveAlpha = Math.max(0, 0.5 * (1.0 - waveRadius / 80.0) * fadeOut);
                gc.setStroke(Color.web("#ff8800", waveAlpha));
                gc.setLineWidth(4 - wave);
                gc.strokeOval(centerX - waveRadius, centerY - waveRadius, waveRadius * 2, waveRadius * 2);
            }
        }

        // Main explosion - massive expanding fireball with easing
        double easeOut = 1.0 - Math.pow(1.0 - Math.min(ageMs / 1000.0, 1.0), 2);
        double radius = 15 + (easeOut * 45);

        // Multi-layered explosion (5 layers for depth)
        for (int layer = 0; layer < 6; layer++) {
            double layerRadius = radius * (1.0 - layer * 0.12);
            double layerAlpha = (0.6 - layer * 0.08) * fadeOut;

            // Color gradient from white-hot center to orange-red edges
            int red = 255 - layer * 15;
            int green = Math.max(100, 255 - layer * 25);
            Color layerColor = Color.web(
                    String.format("#%02x%02x00", Math.min(255, red), Math.min(255, green)),
                    layerAlpha);
            gc.setFill(layerColor);
            gc.fillOval(centerX - layerRadius, centerY - layerRadius, layerRadius * 2, layerRadius * 2);
        }

        // Pulsating inner core
        double pulse = 0.85 + 0.15 * Math.sin(ageMs / 20.0);
        double coreRadius = radius * 0.4 * pulse;
        gc.setFill(Color.web("#ffff00", 0.98 * fadeOut));
        gc.fillOval(centerX - coreRadius, centerY - coreRadius, coreRadius * 2, coreRadius * 2);

        // Bright white center
        double whiteCoreRadius = coreRadius * 0.4;
        gc.setFill(Color.web("#ffffff", 0.95 * fadeOut));
        gc.fillOval(centerX - whiteCoreRadius, centerY - whiteCoreRadius, whiteCoreRadius * 2, whiteCoreRadius * 2);

        // Large debris chunks (flying outward)
        if (ageMs < 600) {
            int numDebris = 16;
            for (int i = 0; i < numDebris; i++) {
                double angle = (i * Math.PI * 2 / numDebris) + (ageMs / 50.0);
                double debrisSpeed = 0.25 + (i % 4) * 0.08;
                double debrisDist = radius * 0.5 + (ageMs * debrisSpeed);
                double debrisX = centerX + Math.cos(angle) * debrisDist;
                double debrisY = centerY + Math.sin(angle) * debrisDist;

                // Varying debris sizes
                double debrisSize = 3 + (i % 3) * 1.5;
                double debrisFade = Math.max(0, 1.0 - (debrisDist / 70.0)) * fadeOut;

                // Debris color (orange to dark)
                Color debrisColor = Color.web(
                        i % 2 == 0 ? "#ff6600" : "#cc4400",
                        debrisFade * 0.7);
                gc.setFill(debrisColor);
                gc.fillOval(debrisX - debrisSize, debrisY - debrisSize, debrisSize * 2, debrisSize * 2);
            }
        }

        // Massive particle burst (sparks)
        if (ageMs < 500) {
            int numSparks = 24;
            for (int i = 0; i < numSparks; i++) {
                double angle = (i * Math.PI * 2 / numSparks) + (ageMs / 35.0);
                double sparkSpeed = 0.4 + (i % 5) * 0.1;
                double sparkDist = radius * 0.6 + (ageMs * sparkSpeed);
                double sparkX = centerX + Math.cos(angle) * sparkDist;
                double sparkY = centerY + Math.sin(angle) * sparkDist;

                double sparkSize = 2 + (ageMs / 80.0);
                double sparkFade = Math.max(0, 1.0 - (sparkDist / 65.0)) * fadeOut;

                gc.setFill(Color.web("#ffff00", sparkFade));
                gc.fillOval(sparkX - sparkSize, sparkY - sparkSize, sparkSize * 2, sparkSize * 2);
            }
        }

        // Multi-stage smoke clouds (appear after explosion)
        if (ageMs > 100) {
            // Primary smoke cloud
            double smokeAge = ageMs - 100;
            double smokeRadius = 25 + (smokeAge * 0.3);
            double smokeAlpha = Math.max(0, 0.5 * (1.0 - smokeAge / 900.0) * fadeOut);

            // Large dark smoke
            gc.setFill(Color.web("#222222", smokeAlpha));
            gc.fillOval(centerX - smokeRadius * 0.9, centerY - smokeRadius * 0.9, smokeRadius * 1.8, smokeRadius * 1.8);

            // Medium gray smoke
            gc.setFill(Color.web("#444444", smokeAlpha * 0.7));
            gc.fillOval(centerX - smokeRadius * 0.7, centerY - smokeRadius * 0.7, smokeRadius * 1.4, smokeRadius * 1.4);

            // Secondary smoke puffs (smaller clouds)
            if (smokeAge < 600) {
                for (int puff = 0; puff < 6; puff++) {
                    double puffAngle = (puff * Math.PI * 2 / 6) + (smokeAge / 80.0);
                    double puffDist = smokeRadius * 0.5 + (smokeAge * 0.15);
                    double puffX = centerX + Math.cos(puffAngle) * puffDist;
                    double puffY = centerY + Math.sin(puffAngle) * puffDist;
                    double puffSize = 8 + (smokeAge / 40.0);
                    double puffAlpha = Math.max(0, 0.3 * (1.0 - smokeAge / 600.0) * fadeOut);

                    gc.setFill(Color.web("#333333", puffAlpha));
                    gc.fillOval(puffX - puffSize, puffY - puffSize, puffSize * 2, puffSize * 2);
                }
            }
        }

        // Ground impact effect (expanding circle on ground)
        if (ageMs < 400) {
            double groundRadius = radius * 0.8 + (ageMs * 0.2);
            double groundAlpha = Math.max(0, 0.2 * (1.0 - ageMs / 400.0) * fadeOut);
            gc.setFill(Color.web("#ff6600", groundAlpha));
            gc.fillOval(centerX - groundRadius, centerY - groundRadius * 0.3, groundRadius * 2, groundRadius * 0.6);
        }
    }

    private static final class SpellEffect {
        final String spellId;
        final Position position;
        final long castTime;

        SpellEffect(String spellId, Position position, long castTime) {
            this.spellId = spellId;
            this.position = position;
            this.castTime = castTime;
        }
    }

    private static final class DeployEffect {
        final Position position;
        final TowerOwner owner;
        final long castTime;

        DeployEffect(Position position, TowerOwner owner, long castTime) {
            this.position = position;
            this.owner = owner != null ? owner : TowerOwner.PLAYER;
            this.castTime = castTime;
        }
    }
}
