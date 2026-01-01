package kuroyale.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MatchRecord {
    private String matchId;
    private LocalDateTime dateTime;
    private String opponentType; // AI, Local, Network
    private String result; // Win, Loss
    private int crowns;
    private int goldChange;
    private String arenaName;
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    public MatchRecord() {
        this.dateTime = LocalDateTime.now();
    }
    
    public MatchRecord(String matchId, LocalDateTime dateTime, String opponentType, 
                      String result, int crowns, int goldChange, String arenaName) {
        this.matchId = matchId;
        this.dateTime = dateTime != null ? dateTime : LocalDateTime.now();
        this.opponentType = opponentType;
        this.result = result;
        this.crowns = crowns;
        this.goldChange = goldChange;
        this.arenaName = arenaName;
    }
    
    public String getMatchId() {
        return matchId;
    }
    
    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }
    
    public LocalDateTime getDateTime() {
        return dateTime;
    }
    
    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime != null ? dateTime : LocalDateTime.now();
    }
    
    public String getDateTimeString() {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
    }
    
    public void setDateTimeString(String dateTimeString) {
        if (dateTimeString != null) {
            try {
                this.dateTime = LocalDateTime.parse(dateTimeString, DATE_TIME_FORMATTER);
            } catch (Exception e) {
                this.dateTime = LocalDateTime.now();
            }
        }
    }
    
    public String getOpponentType() {
        return opponentType;
    }
    
    public void setOpponentType(String opponentType) {
        this.opponentType = opponentType;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public int getCrowns() {
        return crowns;
    }
    
    public void setCrowns(int crowns) {
        this.crowns = crowns;
    }
    
    public int getGoldChange() {
        return goldChange;
    }
    
    public void setGoldChange(int goldChange) {
        this.goldChange = goldChange;
    }
    
    public String getArenaName() {
        return arenaName;
    }
    
    public void setArenaName(String arenaName) {
        this.arenaName = arenaName;
    }
}

