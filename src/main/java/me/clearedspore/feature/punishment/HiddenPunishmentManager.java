package me.clearedspore.feature.punishment;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HiddenPunishmentManager {

    private final JavaPlugin plugin;
    private final Map<String, List<String>> hiddenPunishments;

    public HiddenPunishmentManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.hiddenPunishments = new HashMap<>();
        loadHiddenPunishments();
    }

    private void loadHiddenPunishments() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("hidden-punishments")) {
            for (String uuid : config.getConfigurationSection("hidden-punishments").getKeys(false)) {
                List<String> ids = config.getStringList("hidden-punishments." + uuid);
                hiddenPunishments.put(uuid, ids);
            }
        }
    }

    private void saveHiddenPunishments() {
        FileConfiguration config = plugin.getConfig();
        for (Map.Entry<String, List<String>> entry : hiddenPunishments.entrySet()) {
            config.set("hidden-punishments." + entry.getKey(), entry.getValue());
        }
        plugin.saveConfig();
    }

    public boolean isPunishmentHidden(OfflinePlayer player, String punishmentId) {
        String uuid = player.getUniqueId().toString();
        return hiddenPunishments.containsKey(uuid) && hiddenPunishments.get(uuid).contains(punishmentId);
    }

    public boolean togglePunishmentVisibility(OfflinePlayer player, String punishmentId) {
        String uuid = player.getUniqueId().toString();
        
        if (!hiddenPunishments.containsKey(uuid)) {
            hiddenPunishments.put(uuid, new ArrayList<>());
        }
        
        List<String> ids = hiddenPunishments.get(uuid);
        
        if (ids.contains(punishmentId)) {
            ids.remove(punishmentId);
            saveHiddenPunishments();
            return false; // Now visible
        } else {
            ids.add(punishmentId);
            saveHiddenPunishments();
            return true; // Now hidden
        }
    }
    
    public List<String> getHiddenPunishments(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        return hiddenPunishments.getOrDefault(uuid, new ArrayList<>());
    }
}