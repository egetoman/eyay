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
 * OVERVIEW:
 *   Records a match as a timeline of snapshots.
 *
 * ABSTRACT FUNCTION:
 *   AF(this) = list(frames) where each element is the observable match state when capture() was called.
 *
 * REPRESENTATION INVARIANT:
 *   arenaLayoutId != null && !arenaLayoutId.isBlank()
 *   tickMillis > 0
 *   frames != null && for all f in frames: f != null
 */
public class ReplayRecorder {
    private final String arenaLayoutId;
    private final int tickMillis;
    private final List<ReplayFrame> frames = new ArrayList<>();

    public ReplayRecorder(String arenaLayoutId, int tickMillis) {
        this.arenaLayoutId = arenaLayoutId;
        this.tickMillis = tickMillis;
    }

    /** RI checker */
    public boolean repOk() {
        if (arenaLayoutId == null || arenaLayoutId.isBlank()) return false;
        if (tickMillis <= 0) return false;
        if (frames == null) return false;
        for (ReplayFrame f : frames) if (f == null) return false;
        return true;
    }

    /**
     * REQUIRES:
     *   none (match may be null)
     *
     * MODIFIES:
     *   this.frames
     *
     * EFFECTS:
     *   if match == null: no effect;
     *   else: appends a ReplayFrame with:
     *     elapsedSeconds := match.getElapsedSeconds()
     *     remainingSeconds := match.getRemainingSeconds()
     *     phase := NORMAL/DOUBLE/TRIPLE based on match.getCurrentElixirPhase()
     *     playerElixir := (match.getPlayer()!=null ? match.getPlayer().getCurrentElixir() : 0)
     *     opponentElixir := (match.getOpponent()!=null ? match.getOpponent().getCurrentElixir() : 0)
     *     towers := from match.getArena().getTowers(); skip if tower==null or owner/type/position==null;
     *               map owner PLAYER→1, OPPONENT→2; copy type.name(), position.x, position.y, hp
     *     units  := from match.getArena().getUnits(); skip if unit==null or unit.getCard()==null;
     *               map owner PLAYER→1, OPPONENT→2; copy cardId, cardName, x, y, hp
     *     finished := match.isOver()
     *     winnerPlayerId := set only if match.getOutcome()!=null and getWinner()!=null; map PLAYER→1, OPPONENT→2
     */
    public void capture(Match match) {
        if (match == null) {
            return;
        }
        ReplayFrame frame = new ReplayFrame();
        frame.setElapsedSeconds(match.getElapsedSeconds());
        frame.setRemainingSeconds(match.getRemainingSeconds());
        ElixirPhase phase = match.getCurrentElixirPhase();
        if (phase == ElixirPhase.TRIPLE) {
            frame.setPhase("TRIPLE");
        } else if (phase == ElixirPhase.DOUBLE) {
            frame.setPhase("DOUBLE");
        } else {
            frame.setPhase("NORMAL");
        }

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

