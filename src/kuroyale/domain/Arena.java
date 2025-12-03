package kuroyale.domain;

import java.util.ArrayList;
import java.util.List;

public class Arena {

    private List<Unit> units;
    private List<Tower> towers;
    private int width;
    private int height;

    public Arena() {
        this.units = new ArrayList<>();
        this.towers = new ArrayList<>();
    }

    public Arena(List<Unit> units, List<Tower> towers, int width, int height) {
        this.units = units != null ? units : new ArrayList<>();
        this.towers = towers != null ? towers : new ArrayList<>();
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
}




