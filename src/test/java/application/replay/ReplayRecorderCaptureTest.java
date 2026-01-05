package application.replay;

import kuroyale.domain.Match;
import kuroyale.domain.ReplayFrame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReplayRecorderCaptureTest {

    private ReplayRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new ReplayRecorder("arena_test", 100);
        assertTrue(recorder.repOk());
    }

    @Test
    void capture_nullMatch_noFrameAdded() {
        recorder.capture(null);
        assertEquals(0, recorder.build().getFrames().size());
    }

    @Test
    void capture_copiesTimeAndPhase_triple() {
        Match match = new Match();
        match.setElapsedSeconds(310); // TRIPLE phase

        recorder.capture(match);

        ReplayFrame frame = recorder.build().getFrames().get(0);
        assertEquals(310, frame.getElapsedSeconds());
        assertEquals("TRIPLE", frame.getPhase());
    }

    @Test
    void capture_nullPlayers_elixirDefaultsToZero() {
        Match match = new Match();
        match.setPlayer(null);
        match.setOpponent(null);
        match.setElapsedSeconds(10);

        recorder.capture(match);

        ReplayFrame frame = recorder.build().getFrames().get(0);
        assertEquals(0, frame.getPlayerElixir());
        assertEquals(0, frame.getOpponentElixir());
    }

    @Test
    void capture_nullArena_resultsInEmptySnapshots() {
        Match match = new Match();
        match.setArena(null);

        recorder.capture(match);

        ReplayFrame frame = recorder.build().getFrames().get(0);
        assertNotNull(frame.getTowers());
        assertNotNull(frame.getUnits());
        assertTrue(frame.getTowers().isEmpty());
        assertTrue(frame.getUnits().isEmpty());
    }

    @Test
    void capture_finishedMatch_setsFinishedFlag() {
        Match match = new Match();
        match.setElapsedSeconds(6 * 60); // timeout -> isOver true

        recorder.capture(match);

        ReplayFrame frame = recorder.build().getFrames().get(0);
        assertTrue(frame.isFinished());
    }
}
