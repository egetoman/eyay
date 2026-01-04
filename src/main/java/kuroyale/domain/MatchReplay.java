package kuroyale.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot-timeline replay for a match (Bonus: Watch Match Replay).
 * <p>
 * This is a pragmatic replay format: instead of requiring deterministic simulation across machines,
 * we record periodic snapshots of the authoritative state (towers/units/elixir/timer) and play them back.
 */
public class MatchReplay {
    private String version = "v1";
    private int tickMillis = 500;
    private String arenaLayoutId;
    private List<ReplayFrame> frames = new ArrayList<>();

    public MatchReplay() {
    }

    public MatchReplay(String arenaLayoutId, int tickMillis, List<ReplayFrame> frames) {
        this.arenaLayoutId = arenaLayoutId;
        this.tickMillis = tickMillis;
        this.frames = frames != null ? new ArrayList<>(frames) : new ArrayList<>();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getTickMillis() {
        return tickMillis;
    }

    public void setTickMillis(int tickMillis) {
        this.tickMillis = Math.max(50, tickMillis);
    }

    public String getArenaLayoutId() {
        return arenaLayoutId;
    }

    public void setArenaLayoutId(String arenaLayoutId) {
        this.arenaLayoutId = arenaLayoutId;
    }

    public List<ReplayFrame> getFrames() {
        return frames != null ? new ArrayList<>(frames) : new ArrayList<>();
    }

    public void setFrames(List<ReplayFrame> frames) {
        this.frames = frames != null ? new ArrayList<>(frames) : new ArrayList<>();
    }
}


