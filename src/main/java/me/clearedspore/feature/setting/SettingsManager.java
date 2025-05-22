package me.clearedspore.feature.setting;

import me.clearedspore.storage.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class SettingsManager {
    
    private final JavaPlugin plugin;
    private final List<Setting> globalSettings;
    private final PlayerData playerData;

    public SettingsManager(JavaPlugin plugin, PlayerData playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
        this.globalSettings = new ArrayList<>();
    }
    

    public void registerSetting(Setting setting) {
        globalSettings.add(setting);
    }
    

    public List<Setting> getSettings() {
        return new ArrayList<>(globalSettings);
    }

    public void openSettingsMenu(Player player) {
        SettingMenu menu = new SettingMenu(plugin, player, globalSettings);
        menu.open(player);
    }


    public static boolean isSettingEnabled(PlayerData playerData, Player player, String settingName, boolean defaultValue) {
        playerData.initializePlayerData(player);

        FileConfiguration playerConfig = playerData.getPlayerConfig(player);
        if (playerConfig == null) {
            return defaultValue;
        }

        if (!playerConfig.contains("settings." + settingName)) {
            playerConfig.set("settings." + settingName, defaultValue);
            File playerFile = new File(playerData.getPlayerDataFolder(), player.getUniqueId() + ".yml");
            playerData.savePlayerData(playerConfig, playerFile);
            return defaultValue;
        }

        return playerConfig.getBoolean("settings." + settingName);
    }

    public static void setSettingEnabled(PlayerData playerData, Player player, String settingName, boolean enabled) {
        FileConfiguration playerConfig = playerData.getPlayerConfig(player);
        if (playerConfig == null) {
            return;
        }

        playerConfig.set("settings." + settingName, enabled);
        File playerFile = new File(playerData.getPlayerDataFolder(), player.getUniqueId() + ".yml");
        playerData.savePlayerData(playerConfig, playerFile);
    }
}