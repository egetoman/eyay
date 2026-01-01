package kuroyale.domain;

import java.util.List;

public class MatchStats {
    private int totalMatches;
    private int wins;
    private int losses;
    private double winRate;
    private int totalCrowns;
    private int totalGoldEarned;
    private double averageCrownsPerMatch;
    
    public MatchStats() {
    }
    
    public MatchStats(List<MatchRecord> records) {
        calculateFromRecords(records);
    }
    
    private void calculateFromRecords(List<MatchRecord> records) {
        this.totalMatches = records.size();
        this.wins = 0;
        this.losses = 0;
        this.totalCrowns = 0;
        this.totalGoldEarned = 0;
        
        for (MatchRecord record : records) {
            if ("Win".equalsIgnoreCase(record.getResult())) {
                wins++;
            } else if ("Loss".equalsIgnoreCase(record.getResult())) {
                losses++;
            }
            totalCrowns += record.getCrowns();
            totalGoldEarned += record.getGoldChange();
        }
        
        this.winRate = totalMatches > 0 ? (double) wins / totalMatches * 100.0 : 0.0;
        this.averageCrownsPerMatch = totalMatches > 0 ? (double) totalCrowns / totalMatches : 0.0;
    }
    
    public int getTotalMatches() {
        return totalMatches;
    }
    
    public void setTotalMatches(int totalMatches) {
        this.totalMatches = totalMatches;
    }
    
    public int getWins() {
        return wins;
    }
    
    public void setWins(int wins) {
        this.wins = wins;
    }
    
    public int getLosses() {
        return losses;
    }
    
    public void setLosses(int losses) {
        this.losses = losses;
    }
    
    public double getWinRate() {
        return winRate;
    }
    
    public void setWinRate(double winRate) {
        this.winRate = winRate;
    }
    
    public int getTotalCrowns() {
        return totalCrowns;
    }
    
    public void setTotalCrowns(int totalCrowns) {
        this.totalCrowns = totalCrowns;
    }
    
    public int getTotalGoldEarned() {
        return totalGoldEarned;
    }
    
    public void setTotalGoldEarned(int totalGoldEarned) {
        this.totalGoldEarned = totalGoldEarned;
    }
    
    public double getAverageCrownsPerMatch() {
        return averageCrownsPerMatch;
    }
    
    public void setAverageCrownsPerMatch(double averageCrownsPerMatch) {
        this.averageCrownsPerMatch = averageCrownsPerMatch;
    }
}

