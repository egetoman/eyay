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

