package application;

import java.util.List;
import kuroyale.domain.MatchRecord;
import kuroyale.domain.MatchStats;
import kuroyale.infrastructure.MatchHistoryRepository;

public class MatchHistoryService {
    
    private final MatchHistoryRepository historyRepository;
    
    public MatchHistoryService(MatchHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }
    
    public List<MatchRecord> getMatchHistory() {
        return historyRepository.loadAll();
    }
    
    /**
     * Calculates and returns match statistics from all stored match records.
     * 
     * Requires:
     *   - MatchHistoryRepository is accessible and can load records
     * 
     * Modifies:
     *   - None (read-only operation)
     * 
     * Effects:
     *   - Returns a MatchStats object containing:
     *     - Total number of matches
     *     - Number of wins and losses
     *     - Win rate percentage (wins / total * 100)
     *     - Total crowns earned across all matches
     *     - Total gold earned across all matches
     *     - Average crowns per match
     *   - If no records exist: returns MatchStats with all values at 0
     *   - Win rate and average crowns are calculated as 0.0 if totalMatches is 0
     */
    public MatchStats getMatchStats() {
        List<MatchRecord> records = historyRepository.loadAll();
        return new MatchStats(records);
    }
    
    public void recordMatch(MatchRecord record) {
        if (record != null) {
            historyRepository.save(record);
        }
    }
}

