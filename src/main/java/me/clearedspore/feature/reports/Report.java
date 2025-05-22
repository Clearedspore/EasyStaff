package me.clearedspore.feature.reports;
import me.clearedspore.util.TimeUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class Report {
    private final String reportId;
    private final Player issuer;
    private final String reason;
    private final OfflinePlayer suspect;
    private final long creationTime;
    private final String evidence;

    public Report(String reportId, Player issuer, String reason, OfflinePlayer suspect, long creationTime, String evidence) {
        this.reportId = reportId;
        this.issuer = issuer;
        this.reason = reason;
        this.suspect = suspect;
        this.creationTime = creationTime;
        this.evidence = evidence;
    }

    public String getReportId() {
        return reportId;
    }

    public Player getIssuer() {
        return issuer;
    }

    public String getReason() {
        return reason;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public OfflinePlayer getSuspect() {
        return suspect;
    }

    public String getEvidence() {
        return evidence;
    }

    public boolean hasEvidence(){
        if(evidence.equals("none") || evidence.isEmpty()){
            return false;
        } else {
            return true;
        }
    }

    public String getTimeSinceCreation() {
        long currentTime = System.currentTimeMillis();
        long timeElapsed = currentTime - creationTime;
        return TimeUtil.formatRemainingTime(timeElapsed) + " ago";
    }

}
