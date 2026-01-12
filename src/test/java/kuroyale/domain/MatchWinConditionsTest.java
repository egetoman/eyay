package kuroyale.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class MatchWinConditionsTest {

    @Test
    void kingDestroyedEndsMatchWithThreeCrowns() {
        Arena arena = new Arena();
        Match match = new Match(new Player("P1", null, 0), new Player("P2", null, 0), arena);
        match.setBotEnabled(false);

        // Opponent king destroyed => player wins with 3 crowns.
        Tower opponentKing = findTower(arena, TowerOwner.OPPONENT, TowerType.KING);
        assertNotNull(opponentKing);
        opponentKing.setHp(0);

        MatchOutcome out = match.getOutcome();
        assertNotNull(out);
        assertEquals(TowerOwner.PLAYER, out.getWinner());
        assertEquals(3, out.getPlayerCrowns());
        assertEquals(0, out.getOpponentCrowns());
        assertEquals("KING_DESTROYED", out.getReason());
    }

    @Test
    void destroyingBothCrownTowersEndsMatchEarly() {
        Arena arena = new Arena();
        Match match = new Match(new Player("P1", null, 0), new Player("P2", null, 0), arena);
        match.setBotEnabled(false);

        // Player destroys both opponent crown towers.
        for (Tower t : arena.getTowers()) {
            if (t != null && t.getOwner() == TowerOwner.OPPONENT && t.getType() == TowerType.CROWN) {
                t.setHp(0);
            }
        }

        MatchOutcome out = match.getOutcome();
        assertNotNull(out);
        assertEquals(TowerOwner.PLAYER, out.getWinner());
        assertEquals(2, out.getPlayerCrowns());
        assertEquals(0, out.getOpponentCrowns());
        assertEquals("CROWN_TOWERS_DESTROYED", out.getReason());
    }

    @Test
    void timeOutUsesHpTieBreakWhenCrownsAreEqual() {
        Arena arena = new Arena();
        Match match = new Match(new Player("P1", null, 0), new Player("P2", null, 0), arena);
        match.setBotEnabled(false);

        // Time-out with equal crowns (0-0), but PLAYER has less total tower HP.
        // Reduce PLAYER king HP a bit so OPPONENT should win by HP.
        Tower playerKing = findTower(arena, TowerOwner.PLAYER, TowerType.KING);
        assertNotNull(playerKing);
        playerKing.setHp(1000);

        match.setElapsedSeconds(match.getTotalDurationSeconds());
        MatchOutcome out = match.getOutcome();

        assertNotNull(out);
        assertEquals(TowerOwner.OPPONENT, out.getWinner());
        assertEquals(0, out.getPlayerCrowns());
        assertEquals(0, out.getOpponentCrowns());
        assertEquals("TIME_OUT_HP", out.getReason());
    }

    private static Tower findTower(Arena arena, TowerOwner owner, TowerType type) {
        if (arena == null || owner == null || type == null) {
            return null;
        }
        for (Tower t : arena.getTowers()) {
            if (t != null && t.getOwner() == owner && t.getType() == type) {
                return t;
            }
        }
        return null;
    }
}



