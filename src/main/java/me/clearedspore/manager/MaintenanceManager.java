package me.clearedspore.manager;

import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.setting.SettingsManager;
import me.clearedspore.storage.PlayerData;
import me.clearedspore.util.PS;
import me.clearedspore.util.ServerPingManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MaintenanceManager implements Listener {
    private final JavaPlugin plugin;
    private boolean enabled;
    private final PlayerData playerData;
    private final List<String> exemptPlayers = new ArrayList<>();
    private ServerPingManager serverPingManager;

    private List<String> originalMotd;

    public MaintenanceManager(JavaPlugin plugin, PlayerData playerData) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("maintenance.enabled");
        this.playerData = playerData;

        loadExemptPlayers();
        plugin.reloadConfig();

        List<String> defaultMotd = plugin.getConfig().getStringList("maintenance.motd.default");
        if (defaultMotd != null && !defaultMotd.isEmpty()) {
            originalMotd = defaultMotd;
        } else {
            originalMotd = Collections.singletonList(Bukkit.getMotd());
            plugin.getConfig().set("maintenance.motd.default", originalMotd);
            plugin.saveConfig();
        }

        if (enabled && plugin.getConfig().getBoolean("maintenance.motd.enabled")) {
            List<String> maintenancemotd = plugin.getConfig().getStringList("maintenance.motd.text");
            Bukkit.setMotd(CC.translate(String.join("\n", maintenancemotd)));
        }
    }
    

    public void setServerPingManager(ServerPingManager serverPingManager) {
        this.serverPingManager = serverPingManager;

        if (this.enabled && serverPingManager != null) {
            serverPingManager.setMaintenanceMode(true);
        }
    }



    public void setMaintenance(boolean enabled) {
        this.enabled = enabled;
        plugin.reloadConfig();
        plugin.getConfig().set("maintenance.enabled", enabled);

        if (enabled && plugin.getConfig().getBoolean("maintenance.motd.enabled")) {
            if (plugin.getConfig().getStringList("maintenance.motd.default").isEmpty()) {
                originalMotd = Collections.singletonList(Bukkit.getMotd());
                plugin.getConfig().set("maintenance.motd.default", originalMotd);
            }

            List<String> maintenancemotd = plugin.getConfig().getStringList("maintenance.motd.text");
            Bukkit.setMotd(CC.translate(String.join("\n", maintenancemotd)));
        } else if (!enabled && plugin.getConfig().getBoolean("maintenance.motd.enabled")) {
            if (originalMotd != null && !originalMotd.isEmpty()) {
                Bukkit.setMotd(CC.translate(String.join("\n", originalMotd)));
            }
        }

        if (serverPingManager != null) {
            serverPingManager.setMaintenanceMode(enabled);
        }
        
        plugin.saveConfig();
    }


    public void kickAll(CommandSender player){
        for(Player players : Bukkit.getOnlinePlayers()){
            if(!isExempt(players.getName())) {
                List<String> messageList = plugin.getConfig().getStringList("maintenance.message");
                if (messageList.isEmpty()) {
                    messageList.add("&cMaintenance!");
                    messageList.add("");
                    messageList.add("&fMaintenance has been enabled please be patient while we resolve the issues");
                    messageList.add("&fJoin our discord for updates: &bdiscord.gg/");
                }

                String formattedMessage = String.join("\n", messageList);

                players.kickPlayer(CC.send(formattedMessage));
            }
                if(SettingsManager.isSettingEnabled(playerData, players, "maintenance_logs", true) && players.hasPermission(PS.maintenance_logs)) {
                    players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEhas kicked everyone that wasn't on the maintenance list"));
                }
        }
    }

    public boolean isEnabled(){
        return plugin.getConfig().getBoolean("maintenance.enabled");
    }

    public void addExemptPlayer(String playerName) {
        if (!exemptPlayers.contains(playerName)) {
            exemptPlayers.add(playerName);
            updateExemptPlayers();
        }
    }
    private void updateExemptPlayers() {
        plugin.reloadConfig();
        plugin.getConfig().set("maintenance.exempt", exemptPlayers);
        plugin.saveConfig();
    }

    private void loadExemptPlayers() {
        exemptPlayers.addAll(plugin.getConfig().getStringList("maintenance.exempt"));
    }

    public boolean isExempt(String playerName){
        plugin.reloadConfig();
        List<String> configPlayers = plugin.getConfig().getStringList("maintenance.exempt");
        if(configPlayers.contains(playerName)){
            return true;
        }
        return false;
    }

    public List<String> getExemptPlayers(){
        plugin.reloadConfig();
        List<String> exemptPlayers = plugin.getConfig().getStringList("maintenance.exempt");
        return exemptPlayers;
    }
    
    public List<String> getOriginalMotd() {
        List<String> defaultMotd = plugin.getConfig().getStringList("maintenance.motd.default");
        if (defaultMotd != null && !defaultMotd.isEmpty()) {
            return defaultMotd;
        }
        return Collections.singletonList(Bukkit.getMotd());
    }

    public void removeExemptPlayer(String playerName) {
        exemptPlayers.remove(playerName);
        updateExemptPlayers();
    }

    @EventHandler
    public void onPlayerJoin(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        List<String> messageList = plugin.getConfig().getStringList("maintenance.message");
        if (messageList.isEmpty()) {
            messageList.add("&cMaintenance!");
            messageList.add("");
            messageList.add("&fMaintenance has been enabled please be patient while we resolve the issues");
            messageList.add("&fJoin our discord for updates: &bdiscord.gg/");
        }

        String formattedMessage = String.join("\n", messageList);

        if (isEnabled() && !isExempt(player.getName())) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, CC.send(formattedMessage));
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player players : Bukkit.getOnlinePlayers()) {
                        if (SettingsManager.isSettingEnabled(playerData, players, "maintenance_logs", true) && players.hasPermission(PS.maintenance_logs)) {
                            players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEhas tried to join while maintenance being enabled!"));
                        }
                    }
                }
            }.runTaskLater(plugin, 10L);
        } else if (isEnabled() && isExempt(player.getName())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player players : Bukkit.getOnlinePlayers()) {
                        if (SettingsManager.isSettingEnabled(playerData, players, "maintenance_logs", true) && players.hasPermission(PS.maintenance_logs)) {
                            players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEhas joined while maintenance being enabled!"));
                        }
                    }
                }
            }.runTaskLater(plugin, 10L);
        }
    }

}
