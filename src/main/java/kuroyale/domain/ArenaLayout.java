package kuroyale.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArenaLayout {

    private final String id;
    private final int width;
    private final int height;
    private final List<Tower> towers;
    private final List<Bridge> bridges;
    private String name;

    ArenaLayout(String id, String name, int width, int height, List<Tower> towers, List<Bridge> bridges) {
        this.id = id;
        this.name = name;
        this.width = width;
        this.height = height;
        this.towers = new ArrayList<>();
        if (towers != null) {
            for (Tower tower : towers) {
                this.towers.add(new Tower(tower));
            }
        }
        this.bridges = new ArrayList<>();
        if (bridges != null) {
            for (Bridge bridge : bridges) {
                this.bridges.add(new Bridge(bridge));
            }
        }
    }

    public static ArenaLayout of(int width, int height, List<Tower> towers, List<Bridge> bridges) {
        return new ArenaLayout(java.util.UUID.randomUUID().toString(), "Custom Arena", width, height, towers, bridges);
    }

    public static ArenaLayout of(String id, String name, int width, int height, List<Tower> towers, List<Bridge> bridges) {
        return new ArenaLayout(id, name, width, height, towers, bridges);
    }

    public static ArenaLayout defaultLayout() {
        int width = 18;
        int height = 32;
        List<Tower> towers = new ArrayList<>();
        List<Bridge> bridges = new ArrayList<>();

        int centerX = width / 2;
        int princessOffset = 4;
        int playerKingY = 2;
        int playerPrincessY = 7;
        int opponentKingY = height - 3;
        int opponentPrincessY = height - 8;

        towers.add(createTower(TowerOwner.PLAYER, TowerType.KING, new Position(centerX, playerKingY)));
        towers.add(createTower(TowerOwner.PLAYER, TowerType.CROWN, new Position(centerX - princessOffset, playerPrincessY)));
        towers.add(createTower(TowerOwner.PLAYER, TowerType.CROWN, new Position(centerX + princessOffset, playerPrincessY)));

        towers.add(createTower(TowerOwner.OPPONENT, TowerType.KING, new Position(centerX, opponentKingY)));
        towers.add(createTower(TowerOwner.OPPONENT, TowerType.CROWN, new Position(centerX - princessOffset, opponentPrincessY)));
        towers.add(createTower(TowerOwner.OPPONENT, TowerType.CROWN, new Position(centerX + princessOffset, opponentPrincessY)));

        int riverYStart = (height / 2) - 1;
        int riverYEnd = riverYStart + 1;
        int bridgeTop = riverYStart - 1;
        int bridgeBottom = riverYEnd + 1;
        bridges.add(new Bridge(new Position(centerX - 5, riverYStart), new Position(centerX - 4, bridgeBottom), 2));
        bridges.add(new Bridge(new Position(centerX + 3, riverYStart), new Position(centerX + 4, bridgeBottom), 2));

        return new ArenaLayout("default-arena", "Default Arena", width, height, towers, bridges);
    }

    private static Tower createTower(TowerOwner owner, TowerType type, Position position) {
        int baseHp = type == TowerType.KING ? 4000 : 2500;
        int baseDamage = type == TowerType.KING ? 250 : 140;
        double attackSpeed = type == TowerType.KING ? 1.0 : 0.8;
        return new Tower(baseHp, position, baseDamage, attackSpeed, type, owner);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<Tower> getTowers() {
        return Collections.unmodifiableList(towers);
    }

    public List<Bridge> getBridges() {
        return Collections.unmodifiableList(bridges);
    }
}

