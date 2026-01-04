package application.replay;

import java.util.ArrayList;
import java.util.List;
import kuroyale.domain.Arena;
import kuroyale.domain.Card;
import kuroyale.domain.ElixirPhase;
import kuroyale.domain.Match;
import kuroyale.domain.MatchOutcome;
import kuroyale.domain.MatchReplay;
import kuroyale.domain.Player;
import kuroyale.domain.ReplayFrame;
import kuroyale.domain.Tower;
import kuroyale.domain.TowerOwner;
import kuroyale.domain.Unit;

/**
 * Records a match as a snapshot timeline (Bonus: Watch Match Replay).
 * <p>
 * Recording snapshots is robust against non-determinism (bot randomness, timing drift).
 */
public class ReplayRecorder {
    private final String arenaLayoutId;
    private final int tickMillis;
    private final List<ReplayFrame> frames = new ArrayList<>();

    public ReplayRecorder(String arenaLayoutId, int tickMillis) {
        this.arenaLayoutId = arenaLayoutId;
        this.tickMillis = tickMillis;
    }

    public void capture(Match match) {
        if (match == null) {
            return;
        }
        ReplayFrame frame = new ReplayFrame();
        frame.setElapsedSeconds(match.getElapsedSeconds());
        frame.setRemainingSeconds(match.getRemainingSeconds());
        frame.setPhase(match.getCurrentElixirPhase() == ElixirPhase.TRIPLE ? "TRIPLE" : "DOUBLE");

        Player p1 = match.getPlayer();
        Player p2 = match.getOpponent();
        frame.setPlayerElixir(p1 != null ? p1.getCurrentElixir() : 0);
        frame.setOpponentElixir(p2 != null ? p2.getCurrentElixir() : 0);

        Arena arena = match.getArena();
        if (arena != null) {
            List<ReplayFrame.TowerSnapshot> towers = new ArrayList<>();
            for (Tower t : arena.getTowers()) {
                if (t == null || t.getPosition() == null || t.getType() == null || t.getOwner() == null) {
                    continue;
                }
                ReplayFrame.TowerSnapshot ts = new ReplayFrame.TowerSnapshot();
                ts.owner = t.getOwner() == TowerOwner.PLAYER ? 1 : 2;
                ts.type = t.getType().name();
                ts.x = t.getPosition().getX();
                ts.y = t.getPosition().getY();
                ts.hp = t.getHp();
                towers.add(ts);
            }
            frame.setTowers(towers);

            List<ReplayFrame.UnitSnapshot> units = new ArrayList<>();
            for (Unit u : arena.getUnits()) {
                if (u == null || u.getCard() == null) {
                    continue;
                }
                Card c = u.getCard();
                ReplayFrame.UnitSnapshot us = new ReplayFrame.UnitSnapshot();
                us.cardId = c.getId();
                us.cardName = c.getName();
                us.owner = u.getOwner() == TowerOwner.PLAYER ? 1 : 2;
                us.x = u.getPreciseX();
                us.y = u.getPreciseY();
                us.hp = u.getCurrentHP();
                units.add(us);
            }
            frame.setUnits(units);
        }

        frame.setFinished(match.isOver());
        MatchOutcome outcome = match.getOutcome();
        if (outcome != null && outcome.getWinner() != null) {
            frame.setWinnerPlayerId(outcome.getWinner() == TowerOwner.PLAYER ? 1 : 2);
        }

        frames.add(frame);
    }

    public MatchReplay build() {
        return new MatchReplay(arenaLayoutId, tickMillis, frames);
    }
}


