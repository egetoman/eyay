package kuroyale.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.io.FileWriter;
import java.io.PrintWriter;

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
        return findUnitAt(position) == null && !isBlockedByTower(position);
    }
    
    /**
     * Checks if a position is blocked by a tower.
     * Towers occupy a larger area than just their center tile.
     * King towers have a radius of ~1.5 tiles, Crown towers ~1.0 tiles.
     */
    public boolean isBlockedByTower(Position position) {
        if (position == null) {
            return false;
        }
        return isBlockedByTower(position.getX(), position.getY());
    }
    
    /**
     * Checks if precise coordinates are blocked by a tower.
     */
    public boolean isBlockedByTower(double x, double y) {
        for (Tower tower : towers) {
            if (tower == null || tower.getPosition() == null || tower.isDestroyed()) {
                continue;
            }
            double towerX = tower.getPosition().getX();
            double towerY = tower.getPosition().getY();
            // Tower collision radius: King ~1.5 tiles, Crown ~1.0 tiles
            double radius = tower.getType() == TowerType.KING ? 1.5 : 1.0;
            
            double dx = x - towerX;
            double dy = y - towerY;
            double distSq = dx * dx + dy * dy;
            if (distSq < radius * radius) {
                return true;
            }
        }
        return false;
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

    /**
     * Advances simulation for both units and towers.
     */
    public void tick(double deltaSeconds) {
        if (deltaSeconds <= 0) {
            return;
        }
        // 1) Move/attack units
        if (!units.isEmpty()) {
            for (Unit unit : units) {
                if (unit == null || unit.isDefeated()) {
                    continue;
                }
                unit.tick(this, deltaSeconds);
            }
        }
        // 2) Towers auto-attack
        if (!towers.isEmpty()) {
            for (Tower tower : towers) {
                if (tower == null) {
                    continue;
                }
                tower.tick(this, deltaSeconds);
            }
        }
        // 3) Cleanup defeated units
        if (!units.isEmpty()) {
            List<Unit> livingUnits = new ArrayList<>();
            for (Unit unit : units) {
                if (unit != null && !unit.isDefeated()) {
                    livingUnits.add(unit);
                }
            }
            units = livingUnits;
        }
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
        return findNearestEnemyUnit(requester, fromPosition, maxDistance, null);
    }

    /**
     * Finds the nearest enemy unit that can be attacked by the attacker.
     * Respects ground/flying restrictions: ground units with GROUND target cannot attack flying units.
     * Also prevents units on different bridges from attacking each other.
     * Ground units will not target enemies across the river unless the target is reachable.
     * 
     * @param requester The owner of the unit seeking targets
     * @param fromPosition The position to search from
     * @param maxDistance Maximum distance to search
     * @param attacker The unit that will attack (null for backward compatibility, treats all units as attackable)
     * @return The nearest attackable enemy unit, or null if none found
     */
    public Unit findNearestEnemyUnit(TowerOwner requester, Position fromPosition, double maxDistance, Unit attacker) {
        if (requester == null || fromPosition == null) {
            return null;
        }
        double bestDistance = maxDistance <= 0 ? Double.MAX_VALUE : maxDistance;
        Unit best = null;
        
        // Determine if attacker is on a bridge and which bridge
        boolean attackerOnBridge = false;
        Bridge attackerBridge = null;
        boolean attackerIsGround = attacker != null && attacker.getMovementType() == UnitMovementType.GROUND;
        if (attacker != null && attacker.getPosition() != null) {
            attackerBridge = getBridgeAt(attacker.getPosition());
            attackerOnBridge = (attackerBridge != null);
        }
        
        for (Unit unit : units) {
            if (unit == null || unit.getOwner() == requester || unit.isDefeated()) {
                continue;
            }
            // Check if attacker can attack this unit (respects ground/flying restrictions)
            if (attacker != null && !attacker.canAttackUnit(unit)) {
                continue;
            }
            
            Position targetPos = unit.getPosition();
            if (targetPos == null) {
                continue;
            }
            
            // Prevent units on different bridges from attacking each other
            // (but allow all other attack scenarios: bridge-to-land, land-to-bridge, land-to-land)
            Bridge targetBridge = getBridgeAt(targetPos);
            boolean targetOnBridge = (targetBridge != null);
            
            if (attacker != null && attackerOnBridge) {
                // Only prevent attack if both are on bridges but DIFFERENT bridges
                if (targetOnBridge && !isSameBridge(attackerBridge, targetBridge)) {
                    continue;
                }
                
                // For ground units ON a bridge: only target enemies that are reachable
                // (on same bridge, or on land within X distance of the bridge's footprint)
                if (attackerIsGround && !targetOnBridge) {
                    // Target is on land - check if it's near the bridge the attacker is on
                    int bridgeMinX = Math.min(attackerBridge.getStart().getX(), attackerBridge.getEnd().getX());
                    int bridgeMaxX = Math.max(attackerBridge.getStart().getX(), attackerBridge.getEnd().getX());
                    int targetX = targetPos.getX();
                    
                    // Only target if within 3 tiles horizontally of the bridge
                    // This prevents units on a bridge from targeting enemies far across the arena
                    if (targetX < bridgeMinX - 3 || targetX > bridgeMaxX + 3) {
                        continue;
                    }
                }
            }
            
            // For ground units NOT on a bridge: check if target is across river and unreachable
            // Ground units should not target enemies across the river unless they're
            // within attack range OR on a direct bridge path
            if (attackerIsGround && !attackerOnBridge) {
                boolean attackerNorthSide = isNorthSide(fromPosition);
                boolean targetNorthSide = isNorthSide(targetPos);
                
                // If on opposite sides of the river, only target if very close to a bridge
                if (attackerNorthSide != targetNorthSide) {
                    // Check if attacker is close enough to a bridge to justify cross-river targeting
                    Bridge nearestBridge = findClosestBridge(fromPosition);
                    if (nearestBridge != null) {
                        double distToBridge = distanceToBridge(nearestBridge, fromPosition);
                        // Only allow cross-river targeting if very close to a bridge (within 2 tiles)
                        // This prevents units far from bridges from getting stuck trying to reach
                        // enemies across the river
                        if (distToBridge > 2.0) {
                            continue;
                        }
                    }
                }
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

    /**
     * Resolves the next path target for a unit moving from 'from' to 'desiredDestination'.
     * Ground units must use bridges to cross the river.
     * Flying units can go directly to their destination.
     * 
     * @param from Current position
     * @param desiredDestination Final destination
     * @param movementType The movement type of the unit (GROUND or FLYING)
     * @return The next position to move toward
     */
    public Position resolvePathTarget(Position from, Position desiredDestination, UnitMovementType movementType) {
        if (from == null || desiredDestination == null) {
            return desiredDestination;
        }
        
        // Flying units can go directly - no bridge needed
        if (movementType == UnitMovementType.FLYING) {
            return desiredDestination;
        }
        
        // Check if we need bridge pathing:
        // 1. Crossing from one side to the other
        // 2. Destination is in the river (need to reach via bridge)
        // 3. Current position NOT on bridge but destination requires river crossing
        boolean needsBridgePathing = requiresBridgeCrossing(from, desiredDestination);
        
        // Also need bridge pathing if destination is in river and we're not on bridge
        if (!needsBridgePathing && isRiverTile(desiredDestination)) {
            needsBridgePathing = !isOnBridge(from);
        }
        
        // If we're not on bridge but trying to move toward river, need bridge pathing
        if (!needsBridgePathing && !isOnBridge(from)) {
            // Check if direct path would cross river
            int fromY = from.getY();
            int destY = desiredDestination.getY();
            int riverTop = getRiverTopRow();
            int riverBottom = getRiverBottomRow();
            
            // If path crosses river tiles
            if ((fromY < riverTop && destY > riverBottom) || (fromY > riverBottom && destY < riverTop)) {
                needsBridgePathing = true;
            }
            // If destination is in river zone
            if (destY >= riverTop && destY <= riverBottom) {
                needsBridgePathing = true;
            }
        }
        
        if (!needsBridgePathing) {
            return desiredDestination;
        }
        
        Bridge bridge = findClosestBridge(from);
        if (bridge == null) {
            return desiredDestination;
        }
        
        int minX = Math.min(bridge.getStart().getX(), bridge.getEnd().getX());
        int maxX = Math.max(bridge.getStart().getX(), bridge.getEnd().getX());
        int bridgeMinY = Math.min(bridge.getStart().getY(), bridge.getEnd().getY());
        int bridgeMaxY = Math.max(bridge.getStart().getY(), bridge.getEnd().getY());
        
        int fromX = from.getX();
        int fromY = from.getY();
        
        // #region agent log
        try (PrintWriter pw = new PrintWriter(new FileWriter("/Users/ozanozak/eyay/.cursor/debug.log", true))) { pw.println("{\"hypothesisId\":\"C,E\",\"location\":\"Arena.java:317\",\"message\":\"Bridge pathing engaged\",\"data\":{\"fromX\":" + fromX + ",\"fromY\":" + fromY + ",\"destX\":" + desiredDestination.getX() + ",\"destY\":" + desiredDestination.getY() + ",\"bridgeMinX\":" + minX + ",\"bridgeMaxX\":" + maxX + ",\"bridgeMinY\":" + bridgeMinY + ",\"bridgeMaxY\":" + bridgeMaxY + ",\"bridgeWidth\":" + bridge.getWidth() + "},\"timestamp\":" + System.currentTimeMillis() + "}"); } catch (Exception e) {}
        // #endregion
        
        // Step 1: If not at bridge X range, move horizontally to the bridge first
        if (fromX < minX) {
            // #region agent log
            Position result = new Position(minX, fromY);
            try (PrintWriter pw = new PrintWriter(new FileWriter("/Users/ozanozak/eyay/.cursor/debug.log", true))) { pw.println("{\"hypothesisId\":\"E\",\"location\":\"Arena.java:322\",\"message\":\"PathTarget: move right to bridge\",\"data\":{\"resultX\":" + result.getX() + ",\"resultY\":" + result.getY() + "},\"timestamp\":" + System.currentTimeMillis() + "}"); } catch (Exception e) {}
            return result;
            // #endregion
        }
        if (fromX > maxX) {
            // #region agent log
            Position result = new Position(maxX, fromY);
            try (PrintWriter pw = new PrintWriter(new FileWriter("/Users/ozanozak/eyay/.cursor/debug.log", true))) { pw.println("{\"hypothesisId\":\"E\",\"location\":\"Arena.java:327\",\"message\":\"PathTarget: move left to bridge\",\"data\":{\"resultX\":" + result.getX() + ",\"resultY\":" + result.getY() + "},\"timestamp\":" + System.currentTimeMillis() + "}"); } catch (Exception e) {}
            return result;
            // #endregion
        }
        
        // Step 2: At correct X position - now move to enter the bridge
        if (fromY > bridgeMaxY) {
            // Coming from south, move to bridge entrance (south end of bridge)
            // #region agent log
            Position result = new Position(fromX, bridgeMaxY);
            try (PrintWriter pw = new PrintWriter(new FileWriter("/Users/ozanozak/eyay/.cursor/debug.log", true))) { pw.println("{\"hypothesisId\":\"E\",\"location\":\"Arena.java:335\",\"message\":\"PathTarget: enter bridge from south\",\"data\":{\"resultX\":" + result.getX() + ",\"resultY\":" + result.getY() + "},\"timestamp\":" + System.currentTimeMillis() + "}"); } catch (Exception e) {}
            return result;
            // #endregion
        }
        if (fromY < bridgeMinY) {
            // Coming from north, move to bridge entrance (north end of bridge)
            // #region agent log
            Position result = new Position(fromX, bridgeMinY);
            try (PrintWriter pw = new PrintWriter(new FileWriter("/Users/ozanozak/eyay/.cursor/debug.log", true))) { pw.println("{\"hypothesisId\":\"E\",\"location\":\"Arena.java:341\",\"message\":\"PathTarget: enter bridge from north\",\"data\":{\"resultX\":" + result.getX() + ",\"resultY\":" + result.getY() + "},\"timestamp\":" + System.currentTimeMillis() + "}"); } catch (Exception e) {}
            return result;
            // #endregion
        }
        
        // Step 3: Already on the bridge - head toward destination
        // If destination is also on bridge or in river, go to the closest edge toward it
        int destY = desiredDestination.getY();
        if (destY <= bridgeMinY) {
            // #region agent log
            Position result = new Position(fromX, bridgeMinY);
            try (PrintWriter pw = new PrintWriter(new FileWriter("/Users/ozanozak/eyay/.cursor/debug.log", true))) { pw.println("{\"hypothesisId\":\"E\",\"location\":\"Arena.java:349\",\"message\":\"PathTarget: exit bridge north\",\"data\":{\"resultX\":" + result.getX() + ",\"resultY\":" + result.getY() + "},\"timestamp\":" + System.currentTimeMillis() + "}"); } catch (Exception e) {}
            return result;
            // #endregion
        } else if (destY >= bridgeMaxY) {
            // #region agent log
            Position result = new Position(fromX, bridgeMaxY);
            try (PrintWriter pw = new PrintWriter(new FileWriter("/Users/ozanozak/eyay/.cursor/debug.log", true))) { pw.println("{\"hypothesisId\":\"E\",\"location\":\"Arena.java:353\",\"message\":\"PathTarget: exit bridge south\",\"data\":{\"resultX\":" + result.getX() + ",\"resultY\":" + result.getY() + "},\"timestamp\":" + System.currentTimeMillis() + "}"); } catch (Exception e) {}
            return result;
            // #endregion
        } else {
            // Destination is within bridge Y range - go directly if on bridge X
            // #region agent log
            try (PrintWriter pw = new PrintWriter(new FileWriter("/Users/ozanozak/eyay/.cursor/debug.log", true))) { pw.println("{\"hypothesisId\":\"E\",\"location\":\"Arena.java:357\",\"message\":\"PathTarget: direct to dest on bridge\",\"data\":{\"resultX\":" + desiredDestination.getX() + ",\"resultY\":" + desiredDestination.getY() + "},\"timestamp\":" + System.currentTimeMillis() + "}"); } catch (Exception e) {}
            // #endregion
            return desiredDestination;
        }
    }

    public Position resolvePathTarget(Position from, Position desiredDestination) {
        return resolvePathTarget(from, desiredDestination, UnitMovementType.GROUND);
    }

    /**
     * Checks if a position is in the river (impassable for ground units).
     */
    public boolean isRiverTile(Position position) {
        if (position == null) {
            return false;
        }
        // Delegate to the precise version for consistent behavior
        return isRiverTile((double) position.getY());
    }

    /**
     * Checks if a position is in the river based on precise Y coordinate.
     * Uses floor/ceil to ensure units are only blocked when actually IN the river tiles,
     * not when approaching from either side.
     */
    public boolean isRiverTile(double preciseY) {
        int riverTop = getRiverTopRow();
        int riverBottom = getRiverBottomRow();
        // A position is in the river if its precise Y falls within [riverTop, riverBottom+1)
        // This ensures units at Y=14.9 (approaching from north) are NOT blocked,
        // but units at Y=15.0+ ARE blocked (actually in river)
        boolean result = preciseY >= riverTop && preciseY < (riverBottom + 1);
        // #region agent log
        if (result) { try (PrintWriter pw = new PrintWriter(new FileWriter("/Users/ozanozak/eyay/.cursor/debug.log", true))) { pw.println("{\"hypothesisId\":\"D-FIX\",\"location\":\"Arena.java:368\",\"message\":\"isRiverTile check (fixed)\",\"data\":{\"preciseY\":" + preciseY + ",\"riverTop\":" + riverTop + ",\"riverBottom\":" + riverBottom + ",\"isRiver\":" + result + "},\"timestamp\":" + System.currentTimeMillis() + "}"); } catch (Exception e) {} }
        // #endregion
        return result;
    }

    /**
     * Checks if a position is on a bridge (passable even though it's over river).
     */
    public boolean isOnBridge(Position position) {
        if (position == null || bridges == null || bridges.isEmpty()) {
            return false;
        }
        int px = position.getX();
        int py = position.getY();
        for (Bridge bridge : bridges) {
            if (bridge.getStart() == null || bridge.getEnd() == null) {
                continue;
            }
            int minX = Math.min(bridge.getStart().getX(), bridge.getEnd().getX());
            int maxX = Math.max(bridge.getStart().getX(), bridge.getEnd().getX());
            int minY = Math.min(bridge.getStart().getY(), bridge.getEnd().getY());
            int maxY = Math.max(bridge.getStart().getY(), bridge.getEnd().getY());
            if (px >= minX && px <= maxX && py >= minY && py <= maxY) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a position is on a bridge based on precise coordinates.
     */
    public boolean isOnBridge(double preciseX, double preciseY) {
        if (bridges == null || bridges.isEmpty()) {
            return false;
        }
        int px = (int) Math.round(preciseX);
        int py = (int) Math.round(preciseY);
        return isOnBridge(new Position(px, py));
    }

    /**
     * Gets the bridge that a position is on, or null if not on any bridge.
     */
    private Bridge getBridgeAt(Position position) {
        if (position == null || bridges == null || bridges.isEmpty()) {
            return null;
        }
        int px = position.getX();
        int py = position.getY();
        for (Bridge bridge : bridges) {
            if (bridge.getStart() == null || bridge.getEnd() == null) {
                continue;
            }
            int minX = Math.min(bridge.getStart().getX(), bridge.getEnd().getX());
            int maxX = Math.max(bridge.getStart().getX(), bridge.getEnd().getX());
            int minY = Math.min(bridge.getStart().getY(), bridge.getEnd().getY());
            int maxY = Math.max(bridge.getStart().getY(), bridge.getEnd().getY());
            if (px >= minX && px <= maxX && py >= minY && py <= maxY) {
                return bridge;
            }
        }
        return null;
    }

    /**
     * Checks if two bridges are the same bridge by comparing their positions.
     */
    private boolean isSameBridge(Bridge bridge1, Bridge bridge2) {
        if (bridge1 == null || bridge2 == null) {
            return false;
        }
        if (bridge1 == bridge2) {
            return true;
        }
        // Compare by position coordinates
        if (bridge1.getStart() == null || bridge1.getEnd() == null ||
            bridge2.getStart() == null || bridge2.getEnd() == null) {
            return false;
        }
        // Two bridges are the same if their start and end positions match
        return (bridge1.getStart().equals(bridge2.getStart()) && bridge1.getEnd().equals(bridge2.getEnd())) ||
               (bridge1.getStart().equals(bridge2.getEnd()) && bridge1.getEnd().equals(bridge2.getStart()));
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

    public boolean isPlayerSide(Position position) {
        if (position == null) {
            return false;
        }
        // Player towers are at the TOP (north) with LOW Y values (Y=2, Y=7)
        return position.getY() <= getRiverTopRow();
    }

    public boolean isOpponentSide(Position position) {
        if (position == null) {
            return false;
        }
        // Opponent towers are at the BOTTOM (south) with HIGH Y values (Y=24, Y=29)
        return position.getY() > getRiverBottomRow();
    }

    /**
     * Finds all units within a radius from a position.
     * @param center The center position
     * @param radiusTiles The radius in tiles
     * @return List of units within radius
     */
    public List<Unit> findUnitsInRadius(Position center, double radiusTiles) {
        List<Unit> result = new ArrayList<>();
        if (center == null || radiusTiles <= 0) {
            return result;
        }
        for (Unit unit : units) {
            if (unit == null || unit.isDefeated() || unit.getPosition() == null) {
                continue;
            }
            double dx = unit.getPreciseX() - center.getX();
            double dy = unit.getPreciseY() - center.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance <= radiusTiles) {
                result.add(unit);
            }
        }
        return result;
    }

    /**
     * Finds all towers within a radius from a position.
     * @param center The center position
     * @param radiusTiles The radius in tiles
     * @return List of towers within radius
     */
    public List<Tower> findTowersInRadius(Position center, double radiusTiles) {
        List<Tower> result = new ArrayList<>();
        if (center == null || radiusTiles <= 0) {
            return result;
        }
        for (Tower tower : towers) {
            if (tower == null || tower.isDestroyed() || tower.getPosition() == null) {
                continue;
            }
            double dx = tower.getPosition().getX() - center.getX();
            double dy = tower.getPosition().getY() - center.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance <= radiusTiles) {
                result.add(tower);
            }
        }
        return result;
    }
}
