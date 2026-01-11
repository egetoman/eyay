package kuroyale.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TowerAttackTest {

    @Test
    void towerDealsDamageToEnemyUnitInRange() {
        Arena arena = new Arena();
        Match match = new Match(new Player("P1", null, 0), new Player("P2", null, 0), arena);
        match.setBotEnabled(false);

        // Place a stationary enemy "building" near Player's left crown tower so it stays in range.
        Tower playerLeftCrown = findTower(arena, TowerOwner.PLAYER, TowerType.CROWN, true);
        assertNotNull(playerLeftCrown);
        Position p = playerLeftCrown.getPosition();
        assertNotNull(p);

        Card dummyBuilding = new Card(
                "dummy-building",
                "Dummy Building",
                0,
                CardType.BUILDING,
                new CardStats(500, 0, 0, 0, 0),
                CardTarget.NONE,
                "Test target"
        );

        Unit enemy = new Unit(dummyBuilding, new Position(p.getX(), p.getY() + 2), 500, TowerOwner.OPPONENT);
        arena.addUnit(enemy);

        int before = enemy.getCurrentHP();
        match.advanceTime(1.0); // crown tower attack interval is ~0.8s
        int after = enemy.getCurrentHP();

        assertTrue(after < before, "Expected tower to damage enemy unit in range");
    }

    private static Tower findTower(Arena arena, TowerOwner owner, TowerType type, boolean leftMost) {
        Tower best = null;
        for (Tower t : arena.getTowers()) {
            if (t == null || t.getOwner() != owner || t.getType() != type || t.getPosition() == null) {
                continue;
            }
            if (best == null) {
                best = t;
                continue;
            }
            if (leftMost) {
                if (t.getPosition().getX() < best.getPosition().getX()) {
                    best = t;
                }
            } else {
                if (t.getPosition().getX() > best.getPosition().getX()) {
                    best = t;
                }
            }
        }
        return best;
    }
}


