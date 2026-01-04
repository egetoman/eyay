package application.history;

import application.MatchHistoryService;
import kuroyale.domain.MatchRecord;
import kuroyale.domain.MatchStats;
import kuroyale.infrastructure.MatchHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MatchHistoryService.getMatchStats() method.
 * 
 * Tests cover:
 * - Empty records list (edge case)
 * - Statistics calculation with wins and losses
 * - Win rate calculation accuracy
 * - Crown and gold aggregation
 * - Average crowns per match calculation
 */
public class MatchHistoryServiceTest {

    private MatchHistoryService historyService;
    private FakeMatchHistoryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new FakeMatchHistoryRepository();
        historyService = new MatchHistoryService(repository);
    }

    @Test
    void getMatchStats_emptyRecords_returnsZeroStats() {
        // Setup: No match records
        repository.setRecords(new ArrayList<>());

        // Execute
        MatchStats stats = historyService.getMatchStats();

        // Verify
        assertNotNull(stats);
        assertEquals(0, stats.getTotalMatches());
        assertEquals(0, stats.getWins());
        assertEquals(0, stats.getLosses());
        assertEquals(0.0, stats.getWinRate(), 0.01);
        assertEquals(0, stats.getTotalCrowns());
        assertEquals(0, stats.getTotalGoldEarned());
        assertEquals(0.0, stats.getAverageCrownsPerMatch(), 0.01);
    }

    @Test
    void getMatchStats_withWinsAndLosses_calculatesCorrectly() {
        // Setup: Records with wins and losses
        List<MatchRecord> records = new ArrayList<>();
        records.add(createRecord("match1", "Win", 3, 150));
        records.add(createRecord("match2", "Win", 2, 150));
        records.add(createRecord("match3", "Loss", 1, 50));
        records.add(createRecord("match4", "Loss", 0, 50));
        repository.setRecords(records);

        // Execute
        MatchStats stats = historyService.getMatchStats();

        // Verify
        assertNotNull(stats);
        assertEquals(4, stats.getTotalMatches());
        assertEquals(2, stats.getWins());
        assertEquals(2, stats.getLosses());
        assertEquals(50.0, stats.getWinRate(), 0.01); // 2 wins / 4 total = 50%
        assertEquals(6, stats.getTotalCrowns()); // 3 + 2 + 1 + 0 = 6
        assertEquals(400, stats.getTotalGoldEarned()); // 150 + 150 + 50 + 50 = 400
        assertEquals(1.5, stats.getAverageCrownsPerMatch(), 0.01); // 6 / 4 = 1.5
    }

    @Test
    void getMatchStats_allWins_calculatesPerfectWinRate() {
        // Setup: All wins
        List<MatchRecord> records = new ArrayList<>();
        records.add(createRecord("match1", "Win", 3, 150));
        records.add(createRecord("match2", "Win", 2, 150));
        records.add(createRecord("match3", "Win", 1, 150));
        repository.setRecords(records);

        // Execute
        MatchStats stats = historyService.getMatchStats();

        // Verify
        assertNotNull(stats);
        assertEquals(3, stats.getTotalMatches());
        assertEquals(3, stats.getWins());
        assertEquals(0, stats.getLosses());
        assertEquals(100.0, stats.getWinRate(), 0.01); // 3 wins / 3 total = 100%
        assertEquals(6, stats.getTotalCrowns()); // 3 + 2 + 1 = 6
        assertEquals(450, stats.getTotalGoldEarned()); // 150 + 150 + 150 = 450
        assertEquals(2.0, stats.getAverageCrownsPerMatch(), 0.01); // 6 / 3 = 2.0
    }

    @Test
    void getMatchStats_withDraws_ignoresDrawsInWinLossCount() {
        // Setup: Records with wins, losses, and draws
        List<MatchRecord> records = new ArrayList<>();
        records.add(createRecord("match1", "Win", 2, 150));
        records.add(createRecord("match2", "Loss", 1, 50));
        records.add(createRecord("match3", "Draw", 1, 75));
        records.add(createRecord("match4", "Win", 3, 150));
        repository.setRecords(records);

        // Execute
        MatchStats stats = historyService.getMatchStats();

        // Verify
        assertNotNull(stats);
        assertEquals(4, stats.getTotalMatches());
        assertEquals(2, stats.getWins()); // Only "Win" results counted
        assertEquals(1, stats.getLosses()); // Only "Loss" results counted
        assertEquals(50.0, stats.getWinRate(), 0.01); // 2 wins / 4 total = 50%
        assertEquals(7, stats.getTotalCrowns()); // 2 + 1 + 1 + 3 = 7
        assertEquals(425, stats.getTotalGoldEarned()); // 150 + 50 + 75 + 150 = 425
        assertEquals(1.75, stats.getAverageCrownsPerMatch(), 0.01); // 7 / 4 = 1.75
    }

    @Test
    void getMatchStats_withNegativeGoldChange_handlesCorrectly() {
        // Setup: Records including negative gold (shouldn't happen but test edge case)
        List<MatchRecord> records = new ArrayList<>();
        records.add(createRecord("match1", "Win", 2, 150));
        records.add(createRecord("match2", "Loss", 1, -50)); // Negative gold
        records.add(createRecord("match3", "Win", 3, 150));
        repository.setRecords(records);

        // Execute
        MatchStats stats = historyService.getMatchStats();

        // Verify
        assertNotNull(stats);
        assertEquals(3, stats.getTotalMatches());
        assertEquals(250, stats.getTotalGoldEarned()); // 150 + (-50) + 150 = 250
        assertEquals(6, stats.getTotalCrowns()); // 2 + 1 + 3 = 6
    }

    // Helper method to create test records
    private MatchRecord createRecord(String matchId, String result, int crowns, int goldChange) {
        return new MatchRecord(matchId, LocalDateTime.now(), "AI", result, crowns, goldChange, "Test Arena");
    }

    // Test double (Fake implementation for testing)
    private static class FakeMatchHistoryRepository extends MatchHistoryRepository {
        private List<MatchRecord> records = new ArrayList<>();

        public void setRecords(List<MatchRecord> records) {
            this.records = records != null ? new ArrayList<>(records) : new ArrayList<>();
        }

        @Override
        public List<MatchRecord> loadAll() {
            return new ArrayList<>(records);
        }

        @Override
        public void save(MatchRecord record) {
            if (record != null) {
                records.add(record);
            }
        }
    }
}

