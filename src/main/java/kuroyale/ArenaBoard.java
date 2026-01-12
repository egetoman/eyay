package kuroyale;

import java.util.Collections;
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
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.image.Image;
import kuroyale.domain.Arena;
import kuroyale.domain.ArenaLayout;
import kuroyale.domain.Bridge;
import kuroyale.domain.Position;
import kuroyale.domain.Tower;
import kuroyale.domain.TowerOwner;
import kuroyale.domain.TowerType;
import kuroyale.domain.Unit;

public class ArenaBoard {

    private static final double TILE_SIZE = 20.0;
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
    private final ScrollPane root;
    private Consumer<Position> tileSelectionListener;
    private final boolean flipVertical;
    private List<Tower> currentTowers = Collections.emptyList();
    private final Map<Unit, UnitAnimState> animByUnit = new WeakHashMap<>();

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
        currentTowers = layout.getTowers();
        drawArena(gc, layout);
        drawUnits(gc, layout, units);
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
    }

    private void handleCanvasClick(MouseEvent event) {
        if (tileSelectionListener == null) {
            return;
        }
        double x = event.getX();
        double y = event.getY();
        int gridX = (int) Math.floor(x / TILE_SIZE);
        int gridY;
        if (flipVertical) {
            // Opponent perspective: top of screen is global bottom
            gridY = (int) Math.floor(y / TILE_SIZE);
        } else {
            // Normal: top of screen is global top
            gridY = layout.getHeight() - 1 - (int) Math.floor(y / TILE_SIZE);
        }
        if (gridX < 0 || gridX >= layout.getWidth() || gridY < 0 || gridY >= layout.getHeight()) {
            return;
        }
        tileSelectionListener.accept(new Position(gridX, gridY));
    }

    private void drawArenaBase(GraphicsContext gc, ArenaLayout layout) {
        double widthPx = layout.getWidth() * TILE_SIZE;
        double heightPx = layout.getHeight() * TILE_SIZE;
        double riverTop = (layout.getHeight() / 2.0 - 1) * TILE_SIZE;
        double riverHeight = TILE_SIZE * 2;

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
            double px = x * TILE_SIZE;
            gc.strokeLine(px, 0, px, heightPx);
        }
        for (int y = 0; y <= layout.getHeight(); y++) {
            double py = y * TILE_SIZE;
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
            double plankY = y + (i * TILE_SIZE);
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
        double x = position.getX() * TILE_SIZE;
        double y = convertY(layout, position.getY());

        boolean friendly = (!flipVertical && tower.getOwner() == TowerOwner.PLAYER)
                || (flipVertical && tower.getOwner() == TowerOwner.OPPONENT);

        boolean isKing = tower.getType() == TowerType.KING;
        double towerWidth = TILE_SIZE * (isKing ? 1.6 : 1.3);
        double towerHeight = TILE_SIZE * (isKing ? 2.0 : 1.7);
        double centerX = x + TILE_SIZE / 2;
        double towerX = centerX - towerWidth / 2;
        double towerY = y + TILE_SIZE - towerHeight;

        // Shadow
        gc.setFill(Color.web("#000000", 0.4));
        gc.fillOval(towerX - 2, y + TILE_SIZE - 4, towerWidth + 4, 8);

        // Foundation base (wider, darker)
        double baseWidth = towerWidth + 8;
        double baseHeight = 8;
        double baseX = centerX - baseWidth / 2;
        double baseY = y + TILE_SIZE - baseHeight;

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
        double barY = y + TILE_SIZE + 6;

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
            boolean friendly = (!flipVertical && unit.getOwner() == TowerOwner.PLAYER)
                    || (flipVertical && unit.getOwner() == TowerOwner.OPPONENT);

            if (ENABLE_UNIT_SPRITES) {
                drawUnitSprite(gc, unit, drawX, drawY, friendly, units, currentTowers);
            } else {
                Color fill = friendly ? Color.web("#8bed4a") : Color.web("#ff8a80");
                gc.setFill(fill);
                gc.fillOval(drawX + 4, drawY + 4, TILE_SIZE - 8, TILE_SIZE - 8);
                gc.setStroke(Color.web("#000000"));
                gc.setLineWidth(0.8);
                gc.strokeOval(drawX + 4, drawY + 4, TILE_SIZE - 8, TILE_SIZE - 8);
            }

            // Keep text overlays for debugging (can be removed later)
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            gc.setTextAlign(TextAlignment.CENTER);
            String name = unit.getCard() != null ? unit.getCard().getName() : "Unit";
            gc.fillText(name, drawX + TILE_SIZE / 2, drawY);

            gc.setFont(Font.font("Arial", FontWeight.BOLD, 9));
            gc.setFill(Color.web("#ffe082"));
            gc.fillText(String.valueOf(unit.getCurrentHP()), drawX + TILE_SIZE / 2, drawY + TILE_SIZE + 10);
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
        if (!moving) {
            boolean inRange = hasEnemyInRange(unit, units, towers);
            boolean cooldownReady = (now - st.lastAttackAtMs) >= ATTACK_TRIGGER_COOLDOWN_MILLIS;
            if (inRange && cooldownReady) {
                st.lastAttackAtMs = now;
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

        // Draw slightly larger than a single tile for readability.
        double dw = TILE_SIZE * 1.6;
        double dh = TILE_SIZE * 1.6;
        double dx = drawX + (TILE_SIZE - dw) / 2;
        double dy = drawY + (TILE_SIZE - dh) / 2;

        gc.drawImage(sheet, sx, sy, sw, sh, dx, dy, dw, dh);
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

    private boolean hasEnemyInRange(Unit unit, List<Unit> units, List<Tower> towers) {
        if (unit == null || unit.getOwner() == null || unit.getPosition() == null) {
            return false;
        }
        double range = resolveUnitRenderRange(unit);
        TowerOwner myOwner = unit.getOwner();
        double ux = unit.getPreciseX();
        double uy = unit.getPreciseY();

        if (units != null) {
            for (Unit other : units) {
                if (other == null || other == unit || other.isDefeated() || other.getOwner() == myOwner) {
                    continue;
                }
                double dx = other.getPreciseX() - ux;
                double dy = other.getPreciseY() - uy;
                if (Math.sqrt(dx * dx + dy * dy) <= range) {
                    return true;
                }
            }
        }
        if (towers != null) {
            for (Tower t : towers) {
                if (t == null || t.isDestroyed() || t.getOwner() == null || t.getPosition() == null || t.getOwner() == myOwner) {
                    continue;
                }
                double dx = t.getPosition().getX() - ux;
                double dy = t.getPosition().getY() - uy;
                if (Math.sqrt(dx * dx + dy * dy) <= range) {
                    return true;
                }
            }
        }
        return false;
    }

    private double resolveUnitRenderRange(Unit unit) {
        // Prefer card stats range when present; otherwise use a sensible default.
        if (unit != null && unit.getCard() != null && unit.getCard().getStats() != null) {
            int r = unit.getCard().getStats().getRange();
            if (r > 0) {
                return r;
            }
        }
        return 1.5;
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

        private UnitAnimState(double lastX, double lastY) {
            this.lastX = lastX;
            this.lastY = lastY;
            this.lastAttackAtMs = 0;
        }
    }

    private double convertY(ArenaLayout layout, int gridY) {
        if (flipVertical) {
            return gridY * TILE_SIZE;
        }
        return (layout.getHeight() - gridY - 1) * TILE_SIZE;
    }

    private double convertY(ArenaLayout layout, double gridY) {
        if (flipVertical) {
            return gridY * TILE_SIZE;
        }
        return (layout.getHeight() - gridY - 1) * TILE_SIZE;
    }
}
