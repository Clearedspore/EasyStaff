package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.alertManager.Alert;
import me.clearedspore.feature.alertManager.AlertManager;
import me.clearedspore.util.PS;
import org.bukkit.entity.Player;

@CommandPermission(PS.alerts)
@CommandAlias("alerts|togglealerts|staff-alerts")
public class AlertsCommand extends BaseCommand {

    private final AlertManager alertManager;

    public AlertsCommand(AlertManager alertManager) {
        this.alertManager = alertManager;
    }

    @Default
    @CommandCompletion("@alerts")
    @Syntax("<alert>")
    private void onAlerts(Player player, Alert alert){
        if (player.hasPermission("easystaff.alert." + alert.toString().toLowerCase())) {
            boolean currentStatus = alertManager.hasAlertEnabled(player, alert);
            alertManager.setAlertEnabled(player, alert, !currentStatus);
            player.sendMessage(CC.sendBlue("Alert " + alert.toString().toLowerCase() + " has been " + (!currentStatus ? "enabled" : "disabled") + "."));
        } else {
            player.sendMessage(CC.sendRed("You do not have permission to toggle this alert."));
        }
    }
    
    @Subcommand("status")
    @CommandCompletion("@alerts")
    @Syntax("<alert>")
    private void onAlertStatus(Player player, Alert alert) {
        boolean hasPermission = player.hasPermission("easystaff.alert." + alert.toString().toLowerCase());
        boolean isEnabled = alertManager.hasAlertEnabled(player, alert);
        
        if (hasPermission) {
            player.sendMessage(CC.sendBlue("Alert " + alert.toString().toLowerCase() + " is currently " + 
                (isEnabled ? "enabled" : "disabled") + "."));
        } else {
            player.sendMessage(CC.sendRed("You do not have permission to view this alert's status."));
        }
    }
    
    @Subcommand("list")
    private void onAlertList(Player player) {
        player.sendMessage(CC.sendBlue("Available alerts:"));
        
        for (Alert alert : Alert.values()) {
            boolean hasPermission = player.hasPermission("easystaff.alert." + alert.toString().toLowerCase());
            boolean isEnabled = alertManager.hasAlertEnabled(player, alert);
            
            String status = hasPermission ? 
                (isEnabled ? "§aEnabled" : "§cDisabled") : 
                "§7No Permission";
                
            player.sendMessage(CC.send("&b- " + alert.toString().toLowerCase() + ": " + status));
        }
    }
    
    @Subcommand("reload")
    @CommandPermission(PS.admin)
    private void onAlertReload(Player player) {
        alertManager.initializeAlerts(player);
        player.sendMessage(CC.sendBlue("Your alerts have been reloaded based on your permissions."));
    }
}
