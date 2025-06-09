package me.clearedspore.util.update;

import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.easyAPI.util.Logger;
import me.clearedspore.util.P;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker implements Listener {

    private final JavaPlugin plugin;
    private final int resourceId;
    private final Logger logger;
    private String latestVersion;
    private boolean updateAvailable = false;

    public UpdateChecker(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.resourceId = 125621;

        Bukkit.getPluginManager().registerEvents(this, plugin);

        checkForUpdates();
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {

                URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    latestVersion = reader.readLine();
                }

                String currentVersion = plugin.getDescription().getVersion();
                if (!currentVersion.equals(latestVersion)) {
                    updateAvailable = true;

                    logger.warn("A new update is available for EasyStaff!");
                    logger.info("Current version: " + currentVersion);
                    logger.info("Latest version: " + latestVersion);
                    logger.info("Download the update at: https://www.spigotmc.org/resources/easystaff.125621/");
                } else {
                    logger.info("EasyStaff is up to date! (Version: " + currentVersion + ")");
                }
                
            } catch (IOException e) {
                logger.error("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (updateAvailable && player.hasPermission(P.update_notify)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(CC.sendBlue("[EasyStaff] &fA new update is available!"));
                player.sendMessage(CC.sendBlue("[EasyStaff] &fCurrent version: &e" + plugin.getDescription().getVersion()));
                player.sendMessage(CC.sendBlue("[EasyStaff] &fLatest version: &e" + latestVersion));
                player.sendMessage(CC.sendBlue("[EasyStaff] &fDownload at: &ehttps://www.spigotmc.org/resources/easystaff.125621/"));
            }, 40L);
        }
    }


    public boolean isUpdateAvailable() {
        return updateAvailable;
    }


    public String getLatestVersion() {
        return latestVersion;
    }
}