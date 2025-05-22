package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.EasyStaff;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.easyAPI.util.TimeParser;
import me.clearedspore.feature.setting.SettingsManager;
import me.clearedspore.storage.PlayerData;
import me.clearedspore.manager.MaintenanceManager;
import me.clearedspore.util.PS;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

@CommandAlias("maintenance")
@CommandPermission(PS.maintenance)
public class MaintenanceCommand extends BaseCommand {

    private final MaintenanceManager maintenanceManager;
    private final JavaPlugin plugin;
    private int countdown;
    private BukkitRunnable countdownTask;

    public MaintenanceCommand(MaintenanceManager maintenanceManager, JavaPlugin plugin) {
        this.maintenanceManager = maintenanceManager;
        this.plugin = plugin;
    }

    @Subcommand("add")
    @CommandCompletion("@players")
    @CommandPermission(PS.maintenance_add)
    @Syntax("<player>")
    private void onMaintenanceAdd(CommandSender player, String targetName){

        List<String> exemptPlayers = maintenanceManager.getExemptPlayers();
        if(exemptPlayers.contains(targetName)){
            player.sendMessage(CC.sendRed("That player is already on the exempt list!"));
            return;
        }

        maintenanceManager.addExemptPlayer(targetName);
        player.sendMessage(CC.sendBlue("You have added &f" + targetName + " &#00CCDEto the exempt list"));
    }

    @Subcommand("remove")
    @CommandCompletion("@maintenanceexemptPlayers")
    @CommandPermission(PS.maintenance_remove)
    @Syntax("<player>")
    private void onMaintenanceRemove(CommandSender player, String targetName){

        List<String> exemptPlayers = maintenanceManager.getExemptPlayers();
        if(!exemptPlayers.contains(targetName)){
            player.sendMessage(CC.sendRed("That player is not on the exempt list!"));
            return;
        }

        maintenanceManager.removeExemptPlayer(targetName);
        player.sendMessage(CC.sendBlue("You have removed &f" + targetName + " &#00CCDEfrom the exempt list"));
    }

    @Subcommand("info")
    @CommandPermission(PS.maintenance_info)
    private void onMaintenanceInfo(CommandSender player) {
        boolean enabled = maintenanceManager.isEnabled();
        List<String> exemptPlayers = maintenanceManager.getExemptPlayers();

        player.sendMessage(CC.sendBlue("Maintenance info:"));
        player.sendMessage(CC.sendBlue("Enabled: " + (enabled ? "&aTrue" : "&cFalse")));
        if (exemptPlayers.isEmpty()) {
            player.sendMessage(CC.sendBlue("Exempt Players: &cNone"));
        } else {
            StringBuilder exemptPlayersFormatted = new StringBuilder();
            for (String exemptPlayerName : exemptPlayers) {
                OfflinePlayer exemptPlayer = Bukkit.getOfflinePlayer(exemptPlayerName);
                String colorCode = exemptPlayer.isOnline() ? "&a" : "&7";
                exemptPlayersFormatted.append(colorCode).append(exemptPlayerName).append("&r, ");
            }
            if (exemptPlayersFormatted.length() > 0) {
                exemptPlayersFormatted.setLength(exemptPlayersFormatted.length() - 2);
            }
            player.sendMessage(CC.sendBlue("Exempt Players: " + exemptPlayersFormatted.toString()));
        }
    }

        @Subcommand("toggle")
        @CommandPermission(PS.maintenance_toggle)
        private void onMaintenanceToggle(CommandSender player){

        boolean enabled = !maintenanceManager.isEnabled();

        maintenanceManager.setMaintenance(enabled);
        player.sendMessage(CC.sendBlue("You have " + (enabled ? "enabled" : "disabled") + " maintenance"));
            for (Player players : Bukkit.getOnlinePlayers()) {
                PlayerData playerData = EasyStaff.getInstance().getPlayerData();
                if (SettingsManager.isSettingEnabled(playerData, players, "maintenance_logs", true) && players.hasPermission(PS.maintenance_logs)) {
                    players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEhas " + (enabled ? "enabled" : "disabled") +  " maintenance"));
                }
            }
    }

        @Subcommand("off")
        @CommandPermission(PS.maintenance_toggle)
        private void onMaintenanceOn(CommandSender player){

        if(!maintenanceManager.isEnabled()){
            player.sendMessage(CC.sendRed("Maintenance is already disabled!"));
            return;
        }

        maintenanceManager.setMaintenance(false);
        player.sendMessage(CC.sendBlue("You have disabled maintenance"));
            for (Player players : Bukkit.getOnlinePlayers()) {
                PlayerData playerData = EasyStaff.getInstance().getPlayerData();
                if (SettingsManager.isSettingEnabled(playerData, players, "maintenance_logs", true) && players.hasPermission(PS.maintenance_logs)) {
                    players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEhas disabled maintenance"));
                }
            }
    }

        @Subcommand("on")
        @CommandPermission(PS.maintenance_toggle)
        private void onMaintenanceOff(CommandSender player) {

            if (maintenanceManager.isEnabled()) {
                player.sendMessage(CC.sendRed("Maintenance is already enabled!"));
                return;
            }

            maintenanceManager.setMaintenance(true);
            player.sendMessage(CC.sendBlue("You have enabled maintenance"));
            for (Player players : Bukkit.getOnlinePlayers()) {
                PlayerData playerData = EasyStaff.getInstance().getPlayerData();
                if (SettingsManager.isSettingEnabled(playerData, players, "maintenance_logs", true) && players.hasPermission(PS.maintenance_logs)) {
                    players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEhas enabled maintenance"));
                }
            }
    }

        @Subcommand("kickall")
        @CommandPermission(PS.maintenance_kickall)
        private void onMaintenanceKickAll(CommandSender player){

        if(!maintenanceManager.isEnabled()){
            player.sendMessage(CC.sendRed("Maintenance must be enabled to kick everyone!"));
            return;
        }

        maintenanceManager.kickAll(player);
        player.sendMessage(CC.sendBlue("You have kicked everyone that wasn't on the maintenance whitelist!"));
    }


    @Subcommand("timer start")
    @CommandPermission(PS.maintenance_toggle)
    @Syntax("<time>")
    public void onStart(CommandSender sender, String time, @Optional String... reasonParts) {
        String reason = String.join(" ", reasonParts);
        if (countdownTask != null && !countdownTask.isCancelled()) {
            sender.sendMessage(CC.sendRed("A maintenance timer is already in progress!"));
            return;
        }
        int parsedTime = (int) TimeParser.parseTimeSeconds(time);
        sender.sendMessage(CC.sendBlue("Maintenance enabling in " + time));
        countdown = parsedTime;

        int tenPercent = (int) (parsedTime * 0.1);
        int twentyPercent = (int) (parsedTime * 0.2);
        int fiftyPercent = (int) (parsedTime * 0.5);
        int seventyFivePercent = (int) (parsedTime * 0.75);
        String formattedTime = formatTimeRemaining(countdown);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if(reason.isEmpty()) {
                player.sendTitle(CC.sendRed("&lMaintenance!"), CC.sendWhite("&l" + formattedTime + " remaining!"));
                player.sendMessage(CC.sendRed(""));
                player.sendMessage(CC.sendRed("&lMaintenance!"));
                player.sendMessage(CC.sendWhite("&l" + formattedTime + " remaining!"));
                player.sendMessage(CC.sendRed(""));
            } else {
                player.sendTitle(CC.sendRed("&lMaintenance!"), CC.sendWhite("&l" + formattedTime + " remaining! &f(" + reason + ")"));
                player.sendMessage(CC.sendRed(""));
                player.sendMessage(CC.sendRed("&lMaintenance!"));
                player.sendMessage(CC.sendWhite("&l" + formattedTime + " remaining!"));
                player.sendMessage(CC.sendWhite("Reason: &l" + reason));
                player.sendMessage(CC.sendRed(""));
            }
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 1.0f);
        }

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (countdown > 0) {
                    String formattedTime = formatTimeRemaining(countdown);
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendActionBar(CC.sendRed("Enabling maintenance in: &f" + formattedTime));
                    }

                    if (countdown == tenPercent || countdown == twentyPercent || countdown == fiftyPercent || countdown == seventyFivePercent || countdown <= 5) {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if(reason.isEmpty()) {
                                player.sendTitle(CC.sendRed("&lMaintenance!"), CC.sendWhite("&l" + formattedTime + " remaining!"));
                                player.sendTitle(CC.sendRed("&lMaintenance!"), CC.sendWhite("&l" + formattedTime + " remaining!"));
                                player.sendMessage(CC.sendRed(""));
                                player.sendMessage(CC.sendRed("&lMaintenance!"));
                                player.sendMessage(CC.sendWhite("&l" + formattedTime + " remaining!"));
                                player.sendMessage(CC.sendRed(""));
                            } else {
                                player.sendTitle(CC.sendRed("&lMaintenance!"), CC.sendWhite("&l" + formattedTime + " remaining! &f(" + reason + ")"));
                                player.sendTitle(CC.sendRed("&lMaintenance!"), CC.sendWhite("&l" + formattedTime + " remaining! &f(" + reason + ")"));
                                player.sendMessage(CC.sendRed(""));
                                player.sendMessage(CC.sendRed("&lMaintenance!"));
                                player.sendMessage(CC.sendWhite("&l" + formattedTime + " remaining!"));
                                player.sendMessage(CC.sendWhite("Reason: &l" + reason));
                                player.sendMessage(CC.sendRed(""));
                            }
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 1.0f);
                        }
                    }
                    countdown--;
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "maintenance on");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "maintenance kickall");
                    this.cancel();
                }
            }
        };
        countdownTask.runTaskTimer(plugin, 0L, 20L);
    }

    @Subcommand("timer cancel")
    @CommandPermission(PS.maintenance_toggle)
    public void onCancel(CommandSender sender) {
        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle(CC.sendRed("&lMaintenance Cancelled"), CC.sendWhite("The Maintenance has been cancelled!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
            sender.sendMessage(CC.sendBlue("Maintenance cancelled successfully."));
        } else {
            sender.sendMessage(CC.sendRed("No maintenance countdown is currently in progress."));
        }
    }

    private String formatTimeRemaining(int totalSeconds) {
        int days = totalSeconds / 86400;
        int hours = (totalSeconds % 86400) / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        StringBuilder timeString = new StringBuilder();
        if (days > 0) {
            timeString.append(days).append("d ");
        }
        if (hours > 0) {
            timeString.append(hours).append("h ");
        }
        if (minutes > 0) {
            timeString.append(minutes).append("m ");
        }
        if (seconds > 0 || timeString.length() == 0) {
            timeString.append(seconds).append("s");
        }

        return timeString.toString().trim();
    }


}
