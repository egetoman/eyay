package application.replay;

import kuroyale.domain.Match;
import kuroyale.domain.MatchReplay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReplayRecorderAdtTest {

    @Test
    void repOk_onFreshInstance() {
        ReplayRecorder r = new ReplayRecorder("arena_x", 50);
        assertTrue(r.repOk());
    }

    @Test
    void repOk_afterMultipleCaptures() {
        ReplayRecorder r = new ReplayRecorder("arena_x", 50);

        Match m = new Match();
        m.setElapsedSeconds(10);

        r.capture(m);
        r.capture(m);

        assertTrue(r.repOk());
        assertEquals(2, r.build().getFrames().size());
    }

    @Test
    void build_preservesMetadata() {
        ReplayRecorder r = new ReplayRecorder("arena_meta", 120);

        Match m = new Match();
        m.setElapsedSeconds(100);

        r.capture(m);
        MatchReplay replay = r.build();

        assertEquals("arena_meta", replay.getArenaLayoutId());
        assertEquals(120, replay.getTickMillis());
        assertEquals(1, replay.getFrames().size());
    }

    @Test
    void build_preservesFrameOrderAndValues() {
        ReplayRecorder r = new ReplayRecorder("arena_order", 100);

        Match m1 = new Match();
        m1.setElapsedSeconds(50);

        Match m2 = new Match();
        m2.setElapsedSeconds(200);

        r.capture(m1);
        r.capture(m2);

        assertEquals(50, r.build().getFrames().get(0).getElapsedSeconds());
        assertEquals(200, r.build().getFrames().get(1).getElapsedSeconds());
    }
}
