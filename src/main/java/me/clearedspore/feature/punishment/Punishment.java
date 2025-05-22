package me.clearedspore.feature.punishment;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;

public record Punishment(
        CommandSender issuer,
        OfflinePlayer target,
        String reason,
        boolean active,
        PunishmentType punishmentType,
        String ID,
        CommandSender removalIssuer,
        String removalReason,
        long expirationTime,
        long creationTime
) {
    public Punishment(CommandSender issuer, OfflinePlayer target, String reason, boolean active, PunishmentType punishmentType, String ID) {
        this(issuer, target, reason, active, punishmentType, ID, null, null, -1, System.currentTimeMillis());
    }
    
    public Punishment(CommandSender issuer, OfflinePlayer target, String reason, boolean active, PunishmentType punishmentType, String ID, CommandSender removalIssuer, String removalReason) {
        this(issuer, target, reason, active, punishmentType, ID, removalIssuer, removalReason, -1, System.currentTimeMillis());
    }


    public boolean isTemporary() {
        return expirationTime > 0;
    }
    

    public boolean hasExpired() {
        return isTemporary() && System.currentTimeMillis() >= expirationTime;
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("d:M:y");
        return dateFormat.format(new Date(timestamp));
    }

    public String getFormattedCreationDate() {
        return formatDate(creationTime);
    }

    public long getRemainingTime() {
        if (!isTemporary()) {
            return -1;
        }
        
        long remaining = expirationTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
}