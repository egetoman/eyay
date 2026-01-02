package kuroyale.domain;

/**
 * Immutable match outcome snapshot used by UI/application code to display winner and record history.
 * <p>
 * Win condition (Phase 1 / Phase 2): King Tower destruction ends the match immediately.
 * If time runs out, winner is determined by crown tower count (destroyed opponent Crown Towers).
 * Draw is possible when crown counts are equal at time-out.
 */
public final class MatchOutcome {
    private final TowerOwner winner; // null => draw
    private final int playerCrowns;
    private final int opponentCrowns;
    private final String reason; // "KING_DESTROYED" | "TIME_OUT"

    public MatchOutcome(TowerOwner winner, int playerCrowns, int opponentCrowns, String reason) {
        this.winner = winner;
        this.playerCrowns = Math.max(0, playerCrowns);
        this.opponentCrowns = Math.max(0, opponentCrowns);
        this.reason = reason;
    }

    public TowerOwner getWinner() {
        return winner;
    }

    public boolean isDraw() {
        return winner == null;
    }

    public int getPlayerCrowns() {
        return playerCrowns;
    }

    public int getOpponentCrowns() {
        return opponentCrowns;
    }

    public String getReason() {
        return reason;
    }
}


