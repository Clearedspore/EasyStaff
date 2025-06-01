package me.clearedspore.storage;

import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.easyAPI.util.Logger;
import me.clearedspore.feature.alertManager.Alert;
import me.clearedspore.feature.alertManager.AlertManager;
import me.clearedspore.util.P;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class PlayerData implements Listener {

    private final JavaPlugin plugin;
    private final File playerDataFolder;
    private final Logger logger;
    private AlertManager alertManager;

    public PlayerData(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerstorage");
        this.logger = logger;

        if (!playerDataFolder.exists()) {
            boolean created = playerDataFolder.mkdirs();
            if (created) {
                logger.info("Player data directory created successfully.");
            } else {
                logger.warn("Failed to create player data directory.");
            }
        }
    }

    public void setAlertManager(AlertManager alertManager){
        this.alertManager = alertManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        initializePlayerData(player);

        FileConfiguration playerConfig = getPlayerConfig(player);
        new BukkitRunnable() {
            @Override
            public void run() {
                logger.info("BukkitRunnable started for player: " + player.getName());
                if (hasNewName(player)) {
                    logger.info("Player has a new name: " + player.getName());
                    for (Player players : Bukkit.getOnlinePlayers()) {
                        if (players.hasPermission(P.notify) && alertManager.hasAlertEnabled(players, Alert.STAFF)) {
                            String configPlayerName = playerConfig.getString("name");
                            players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEhas changed their name to &f" + configPlayerName));
                            logger.info("Sent name change alert to: " + players.getName());
                        }
                    }
                }
            }
        }.runTaskLater(plugin, 10);
    }

    public boolean hasPlayerData(UUID playerID) {
        File playerFile = new File(playerDataFolder, playerID + ".yml");
        return playerFile.exists();
    }


    public void initializePlayerData(OfflinePlayer player) {
        UUID playerUUID = player.getUniqueId();
        String playerName = player.getName();
        File playerFile = new File(playerDataFolder, playerUUID + ".yml");
        boolean isNewFile = false;

        if (!playerFile.exists()) {
            try {
                playerFile.createNewFile();
                isNewFile = true;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        boolean changed = false;

        if (!playerConfig.contains("name") || isNewFile) {
            playerConfig.set("name", playerName);
            changed = true;
            logger.info("Added status field for player " + playerName + ": " + playerName);
        }

        if (!playerConfig.contains("uuid") || isNewFile) {
            playerConfig.set("uuid", player.getUniqueId().toString());
            changed = true;
            logger.info("Added UUID field for player " + playerName + ": " + player.getUniqueId().toString());
        }

        if (!playerConfig.contains("first-join") || isNewFile) {
            long firstPlayed = player.getFirstPlayed();
            String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(firstPlayed));
            playerConfig.set("first-join", formattedDate);
            changed = true;
            logger.info("Added first-join field for player " + playerName + ": " + formattedDate);
        }

        if (player instanceof Player) {
            Player onlinePlayer = (Player) player;
            String ipAddress = onlinePlayer.getAddress().getAddress().getHostAddress();
            if (!playerConfig.contains("ip-address") || isNewFile) {
                playerConfig.set("ip-address", ipAddress);
                changed = true;
                logger.info("Added IP address field for player " + playerName + ": " + ipAddress);
            }
        }

        if (!playerConfig.contains("main-suffix") || isNewFile) {
            playerConfig.set("main-suffix", null);
            changed = true;
        }

        if (!playerConfig.contains("freezed") || isNewFile) {
            playerConfig.set("freezed", false);
            changed = true;
        }

        if (!playerConfig.contains("staffmode-data") || isNewFile) {
            playerConfig.createSection("staffmode-data");
            changed = true;
        }

        if (!playerConfig.contains("alerts") || isNewFile) {
            playerConfig.createSection("alerts");
            changed = true;
        }

        if (changed) {
            savePlayerData(playerConfig, playerFile);
            if (isNewFile) {
                logger.info("Created new player data file for " + playerName);
            } else {
                logger.info("Updated player data file for " + playerName + " with missing fields");
            }
        }
    }

    public File getPlayerDataFolder() {
        return playerDataFolder;
    }

    public FileConfiguration getPlayerConfig(OfflinePlayer player) {
        UUID playerID = player.getUniqueId();
        File playerFile = new File(playerDataFolder, playerID + ".yml");

        if (playerFile.exists()) {
            return YamlConfiguration.loadConfiguration(playerFile);
        }

        return null;
    }

    public File getPlayerFile(OfflinePlayer player){
        UUID playerID = player.getUniqueId();
        File playerFile = new File(playerDataFolder, playerID + ".yml");
        return playerFile;
    }

    public String getIP(OfflinePlayer player){
        FileConfiguration playerConfig = getPlayerConfig(player);
        String IP = playerConfig.getString("ip-address");
        return IP;
    }

    public void setMainSuffix(OfflinePlayer player, String prefix){
        FileConfiguration playerConfig = getPlayerConfig(player);
        playerConfig.set("main-suffix", prefix);
        savePlayerData(playerConfig, getPlayerFile(player));
    }

    public String getMainSuffix(OfflinePlayer player){
        FileConfiguration playerConfig = getPlayerConfig(player);
        String prefix = playerConfig.getString("main-suffix");
        return prefix;
    }


    public void setFreezed(OfflinePlayer player, boolean enabled){
        FileConfiguration playerConfig = getPlayerConfig(player);
        playerConfig.set("freezed", enabled);
        savePlayerData(playerConfig, getPlayerFile(player));
    }

    public boolean isFreezed(OfflinePlayer player){
        FileConfiguration playerConfig = getPlayerConfig(player);
        boolean freezed = playerConfig.getBoolean("freezed");
        return freezed;
    }

    public boolean hasNewName(OfflinePlayer player){
        FileConfiguration playerConfig = getPlayerConfig(player);
        String playerName = player.getName();
        String configPlayerName = playerConfig.getString("name");
        if(playerName.equals(configPlayerName)){
            return false;
        } else {
            playerConfig.set("name", playerName);
            savePlayerData(playerConfig, getPlayerFile(player));
            return true;
        }
    }


    public FileConfiguration reloadPlayerData(OfflinePlayer player) {
        UUID playerID = player.getUniqueId();
        String playerName = player.getName();
        File playerFile = new File(playerDataFolder, playerID + ".yml");
        if (playerFile.exists()) {
            FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
            boolean changed = false;


            if (!playerConfig.contains("name")) {
                playerConfig.set("name", playerName);
                changed = true;
                logger.info("Added status field for player " + playerName + ": " + playerName);
            }

            if (!playerConfig.contains("uuid")) {
                playerConfig.set("uuid", player.getUniqueId().toString());
                changed = true;
                logger.info("Added UUID field for player " + playerName + ": " + player.getUniqueId().toString());
            }

            if (!playerConfig.contains("first-join")) {
                long firstPlayed = player.getFirstPlayed();
                String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(firstPlayed));
                playerConfig.set("first-join", formattedDate);
                changed = true;
                logger.info("Added first-join field for player " + playerName + ": " + formattedDate);
            }

            if (player instanceof Player) {
                Player onlinePlayer = (Player) player;
                String ipAddress = onlinePlayer.getAddress().getAddress().getHostAddress();
                if (!playerConfig.contains("ip-address")) {
                    playerConfig.set("ip-address", ipAddress);
                    changed = true;
                    logger.info("Added IP address field for player " + playerName + ": " + ipAddress);
                }
            }

            if (!playerConfig.contains("main-suffix")) {
                playerConfig.set("main-suffix", null);
                logger.info("Added main-suffix field to player data: " + playerName + " = false");
                changed = true;
            }

            if (!playerConfig.contains("freezed")) {
                playerConfig.set("freezed", false);
                changed = true;
            }

            if (!playerConfig.contains("alerts")) {
                playerConfig.createSection("alerts");
                changed = true;
                logger.info("Added alerts section to player data for " + playerName);
            }

            if (changed) {
                savePlayerData(playerConfig, playerFile);
                logger.info("Updated missing fields in player data for " + playerName);
            }

            return playerConfig;
        }

        return null;
    }

    public List<String> getPlayerNamesWithSameIP(String targetIpAddress) {
        List<String> matchingPlayerNames = new ArrayList<>();

        for (File playerFile : playerDataFolder.listFiles()) {
            if (playerFile.isFile() && playerFile.getName().endsWith(".yml")) {
                FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
                String ipAddress = playerConfig.getString("ip-address");

                if (ipAddress != null && ipAddress.equals(targetIpAddress)) {
                    String playerName = playerConfig.getString("name");
                    if (playerName != null) {
                        matchingPlayerNames.add(playerName);
                    }
                }
            }
        }

        return matchingPlayerNames;
    }

    public boolean hasAlts(OfflinePlayer player) {
        String playerIpAddress = getIP(player);
        List<String> playerNamesWithSameIP = getPlayerNamesWithSameIP(playerIpAddress);

        playerNamesWithSameIP.remove(player.getName());
        return !playerNamesWithSameIP.isEmpty();
    }

    public void savePlayerData(FileConfiguration playerConfig, File playerFile) {
        try {
            playerConfig.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAllPlayerData() {
        for (File playerFile : playerDataFolder.listFiles()) {
            if (playerFile.isFile() && playerFile.getName().endsWith(".yml")) {
                FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
                try {
                    playerConfig.save(playerFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean resetPlayerData(OfflinePlayer player) {
        UUID playerID = player.getUniqueId();
        String playerName = player.getName();
        File playerFile = new File(playerDataFolder, playerID + ".yml");
        if (playerFile.exists()) {
            if (playerFile.delete()) {
                initializePlayerData(player);
                return true;
            } else {
                logger.warn("Failed to delete player data file for " + playerName);
                return false;
            }
        }
        return false;
    }
}