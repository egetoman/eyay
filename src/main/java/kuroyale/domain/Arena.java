package kuroyale.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Arena {

    public static final int DEFAULT_WIDTH = 18;
    public static final int DEFAULT_HEIGHT = 32;

    private List<Unit> units;
    private List<Tower> towers;
    private List<Bridge> bridges;
    private int width;
    private int height;

    public Arena() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        loadLayout(ArenaLayout.defaultLayout());
    }

    public Arena(int width, int height) {
        this(new ArrayList<>(), new ArrayList<>(), width, height);
    }

    public Arena(List<Unit> units, List<Tower> towers, int width, int height) {
        this.units = units != null ? units : new ArrayList<>();
        this.towers = towers != null ? towers : new ArrayList<>();
        this.bridges = new ArrayList<>();
        this.width = width;
        this.height = height;
    }

    public List<Unit> getUnits() {
        return units;
    }

    public void setUnits(List<Unit> units) {
        this.units = units;
    }

    public List<Tower> getTowers() {
        return towers;
    }

    public void setTowers(List<Tower> towers) {
        this.towers = towers;
    }

    public List<Bridge> getBridges() {
        return bridges;
    }

    public void setBridges(List<Bridge> bridges) {
        this.bridges = bridges;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isWithinBounds(Position position) {
        if (position == null) {
            return false;
        }
        return position.getX() >= 0 && position.getX() < width
            && position.getY() >= 0 && position.getY() < height;
    }

    public boolean isTileFree(Position position) {
        if (position == null) {
            return false;
        }
        return findUnitAt(position) == null && towers.stream()
            .map(Tower::getPosition)
            .noneMatch(pos -> pos != null && pos.sameTile(position));
    }

    public Unit findUnitAt(Position position) {
        if (position == null) {
            return null;
        }
        return units.stream()
            .filter(unit -> unit.getPosition() != null && unit.getPosition().sameTile(position))
            .findFirst()
            .orElse(null);
    }

    public void addUnit(Unit unit) {
        if (unit != null) {
            units.add(unit);
        }
    }

    public void removeUnit(Unit unit) {
        units.remove(unit);
    }

    public void loadLayout(ArenaLayout layout) {
        if (layout == null) {
            return;
        }
        this.width = layout.getWidth();
        this.height = layout.getHeight();
        this.towers = new ArrayList<>();
        for (Tower tower : layout.getTowers()) {
            this.towers.add(new Tower(tower));
        }
        this.bridges = new ArrayList<>();
        for (Bridge bridge : layout.getBridges()) {
            this.bridges.add(new Bridge(bridge));
        }
    }

    public ArenaLayout toLayout() {
        List<Tower> towerCopies = new ArrayList<>();
        for (Tower tower : towers) {
            towerCopies.add(new Tower(tower));
        }
        List<Bridge> bridgeCopies = new ArrayList<>();
        for (Bridge bridge : bridges) {
            bridgeCopies.add(new Bridge(bridge));
        }
        return ArenaLayout.of(width, height, towerCopies, bridgeCopies);
    }

    public void tickUnits(double deltaSeconds) {
        if (deltaSeconds <= 0 || units.isEmpty()) {
            return;
        }
        List<Unit> livingUnits = new ArrayList<>();
        for (Unit unit : units) {
            if (unit == null || unit.isDefeated()) {
                continue;
            }
            unit.tick(this, deltaSeconds);
            livingUnits.add(unit);
        }
        this.units = livingUnits;
    }

    public Tower findNearestEnemyTower(TowerOwner requester, Position fromPosition) {
        if (requester == null || fromPosition == null) {
            return null;
        }
        return towers.stream()
            .filter(tower -> tower != null && tower.getOwner() != requester && !tower.isDestroyed())
            .min(Comparator.comparingDouble(tower -> distance(tower.getPosition(), fromPosition)))
            .orElse(null);
    }

    private double distance(Position one, Position two) {
        if (one == null || two == null) {
            return Double.MAX_VALUE;
        }
        double dx = one.getX() - two.getX();
        double dy = one.getY() - two.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    public Unit findNearestEnemyUnit(TowerOwner requester, Position fromPosition, double maxDistance) {
        if (requester == null || fromPosition == null) {
            return null;
        }
        double bestDistance = maxDistance <= 0 ? Double.MAX_VALUE : maxDistance;
        Unit best = null;
        for (Unit unit : units) {
            if (unit == null || unit.getOwner() == requester || unit.isDefeated()) {
                continue;
            }
            double dx = unit.getPreciseX() - fromPosition.getX();
            double dy = unit.getPreciseY() - fromPosition.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = unit;
            }
        }
        return best;
    }

    public Position resolvePathTarget(Position from, Position desiredDestination) {
        if (from == null || desiredDestination == null) {
            return desiredDestination;
        }
        if (!requiresBridgeCrossing(from, desiredDestination)) {
            return desiredDestination;
        }
        Bridge bridge = findClosestBridge(from);
        if (bridge == null) {
            return desiredDestination;
        }
        int minX = Math.min(bridge.getStart().getX(), bridge.getEnd().getX());
        int maxX = Math.max(bridge.getStart().getX(), bridge.getEnd().getX());
        int targetX = clamp(from.getX(), minX, maxX);
        int targetY = isNorthSide(from) ? getRiverBottomRow() : getRiverTopRow();
        return new Position(targetX, targetY);
    }

    private boolean requiresBridgeCrossing(Position from, Position destination) {
        return isNorthSide(from) != isNorthSide(destination);
    }

    private boolean isNorthSide(Position position) {
        if (position == null) {
            return false;
        }
        return position.getY() <= getRiverTopRow();
    }

    private int getRiverTopRow() {
        return height / 2 - 1;
    }

    private int getRiverBottomRow() {
        return getRiverTopRow() + 1;
    }

    private Bridge findClosestBridge(Position from) {
        if (bridges.isEmpty() || from == null) {
            return null;
        }
        return bridges.stream()
            .filter(bridge -> bridge.getStart() != null && bridge.getEnd() != null)
            .min(Comparator.comparingDouble(bridge -> distanceToBridge(bridge, from)))
            .orElse(null);
    }

    private double distanceToBridge(Bridge bridge, Position from) {
        if (bridge == null || from == null || bridge.getStart() == null || bridge.getEnd() == null) {
            return Double.MAX_VALUE;
        }
        int minX = Math.min(bridge.getStart().getX(), bridge.getEnd().getX());
        int maxX = Math.max(bridge.getStart().getX(), bridge.getEnd().getX());
        int centerX = (minX + maxX) / 2;
        double dx = centerX - from.getX();
        double dy = (isNorthSide(from) ? getRiverTopRow() : getRiverBottomRow()) - from.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}




