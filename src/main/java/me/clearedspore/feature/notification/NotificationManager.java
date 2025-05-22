package me.clearedspore.feature.notification;

import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.util.PS;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class NotificationManager {

    private final JavaPlugin plugin;
    private final List<String> notifyList;

    public NotificationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.notifyList = new ArrayList<>();
        loadNotifyList();
    }

    private void loadNotifyList() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("notifications.players")) {
            notifyList.addAll(config.getStringList("notifications.players"));
        } else {
            config.set("notifications.players", new ArrayList<>());
            plugin.saveConfig();
        }
    }

    private void saveNotifyList() {
        FileConfiguration config = plugin.getConfig();
        config.set("notifications.players", notifyList);
        plugin.saveConfig();
    }

    public boolean addPlayerToNotifyList(String playerName) {
        if (notifyList.contains(playerName.toLowerCase())) {
            return false;
        }
        notifyList.add(playerName.toLowerCase());
        saveNotifyList();
        return true;
    }

    public boolean removePlayerFromNotifyList(String playerName) {
        boolean removed = notifyList.remove(playerName.toLowerCase());
        if (removed) {
            saveNotifyList();
        }
        return removed;
    }

    public List<String> getNotifyList() {
        return new ArrayList<>(notifyList);
    }

    public void notifyHistoryCheck(Player checker, OfflinePlayer target) {
        if (notifyList.contains(target.getName().toLowerCase())) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission(PS.punish_notify_high)) {
                    player.sendMessage(CC.sendBlue("[High Staff] &f" + checker.getName() + " &#00CCDEis checking &f" + target.getName() + "'s &#00CCDEhistory"));
                }
            }
        }
    }
}