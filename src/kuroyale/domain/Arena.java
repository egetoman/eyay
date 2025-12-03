package kuroyale.domain;

import java.util.ArrayList;
import java.util.List;

public class Arena {

    private List<Unit> units;
    private List<Tower> towers;
    private List<Bridge> bridges;
    private int width;
    private int height;

    public Arena() {
        this.units = new ArrayList<>();
        this.towers = new ArrayList<>();
        this.bridges = new ArrayList<>();
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
        this.bridges = bridges != null ? bridges : new ArrayList<>();
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

    public void addTower(Tower tower) {
        if (tower != null) {
            towers.add(tower);
        }
    }

    public void addBridge(Bridge bridge) {
        if (bridge != null && bridges.size() < 3) {
            bridges.add(bridge);
        }
    }

    public Arena mirrorVertically() {
        Arena mirrored = new Arena(new ArrayList<>(), new ArrayList<>(), width, height);
        for (Tower tower : towers) {
            Position p = tower.getPosition();
            Position mirroredPos = new Position(p.getX(), (height - 1) - p.getY());
            Tower copy = new Tower(tower.getHp(), mirroredPos, tower.getDamage(), tower.getAttackSpeed(), PlayerSide.OPPONENT);
            mirrored.addTower(copy);
        }
        for (Bridge bridge : bridges) {
            Position start = bridge.getStart();
            Position end = bridge.getEnd();
            Position mirroredStart = new Position(start.getX(), (height - 1) - start.getY());
            Position mirroredEnd = new Position(end.getX(), (height - 1) - end.getY());
            mirrored.addBridge(new Bridge(mirroredStart, mirroredEnd));
        }
        return mirrored;
    }
}

