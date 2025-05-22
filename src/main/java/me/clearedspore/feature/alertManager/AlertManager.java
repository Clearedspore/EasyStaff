package me.clearedspore.feature.alertManager;

import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.storage.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.BatchUpdateException;

public class AlertManager implements Listener {
    private final PlayerData playerData;
    private final JavaPlugin plugin;

    public AlertManager(PlayerData playerData, JavaPlugin plugin) {
        this.playerData = playerData;
        this.plugin = plugin;
    }

    public boolean hasAlertEnabled(Player player, Alert alert){
        File playerFile = playerData.getPlayerFile(player);
        FileConfiguration playerConfig = playerData.getPlayerConfig(player);

        String alertPath = "alerts." + alert.toString().toLowerCase();
        if (!playerConfig.contains(alertPath)) {
            boolean hasPermission = player.hasPermission("easystaff.alert." + alert.toString().toLowerCase());
            playerConfig.set(alertPath, hasPermission);
            playerData.savePlayerData(playerConfig, playerFile);
        }
        
        boolean alertBoolean = playerConfig.getBoolean(alertPath);
        boolean hasPermission = player.hasPermission("easystaff.alert." + alert.toString().toLowerCase());
        return alertBoolean && hasPermission;
    }

    public void setAlertEnabled(Player player, Alert alert, boolean enabled){
        File playerFile = playerData.getPlayerFile(player);
        FileConfiguration playerConfig = playerData.getPlayerConfig(player);
        playerConfig.set("alerts." + alert.toString().toLowerCase(), enabled);
        playerData.savePlayerData(playerConfig, playerFile);
    }


    public void initializeAlerts(Player player) {
        File playerFile = playerData.getPlayerFile(player);
        FileConfiguration playerConfig = playerData.getPlayerConfig(player);
        boolean changed = false;
        
        for (Alert alert : Alert.values()) {
            String alertPath = "alerts." + alert.toString().toLowerCase();
            if (!playerConfig.contains(alertPath)) {
                boolean hasPermission = player.hasPermission("easystaff.alert." + alert.toString().toLowerCase());
                playerConfig.set(alertPath, hasPermission);
                changed = true;
            }
        }
        
        if (changed) {
            playerData.savePlayerData(playerConfig, playerFile);
        }
    }

    private Component colorize(String text) {
        return Component.text(ChatColor.translateAlternateColorCodes('&', text));
    }
    
    public void altAlert(Player player) {
        if (playerData.hasAlts(player)) {
            for (Player players : Bukkit.getOnlinePlayers()) {
                if (hasAlertEnabled(players, Alert.ALT) && plugin.getConfig().getBoolean("alerts.alt-alert")) {
                    Component button = Component.text("[check]")
                            .color(NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.runCommand("/alts " + player.getName()))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to check " + player.getName() + "'s alts")));

                    String message = plugin.getConfig().getString("alerts.alt-message");
                    if (message != null) {
                        message = message.replace("%player%", player.getName());

                        int buttonIndex = message.indexOf("%button%");
                        if (buttonIndex != -1) {
                            String beforeButton = message.substring(0, buttonIndex);
                            String afterButton = message.substring(buttonIndex + 8);

                            Component completeMessage = colorize(beforeButton)
                                    .append(button)
                                    .append(colorize(afterButton));
                            
                            players.sendMessage(completeMessage);
                        } else {
                            players.sendMessage(colorize(message));
                            players.sendMessage(button);
                        }
                    } else {
                        Component defaultMessage = colorize("&b[Staff]&f " + player.getName() + "&b has been detected with an alt! ")
                                .append(button);
                        players.sendMessage(defaultMessage);
                    }
                }
            }
        }
    }

    public void xRayAlert(Player player, int veinSize, Block block){
        for(Player staff : Bukkit.getOnlinePlayers()){
            if(hasAlertEnabled(staff, Alert.XRAY) && plugin.getConfig().getBoolean("alerts.xray-alert")){
                String message = plugin.getConfig().getString("alerts.xray-message");
                if(message != null) {
                    message = message.replace("%player%", player.getName());
                    message = message.replace("%blocks%", block.getType().toString());
                    message = message.replace("%amount%", String.valueOf(veinSize));
                    staff.sendMessage(CC.send(message));
                } else {
                    staff.sendMessage(CC.send("&b[Staff X-ray] &f" + player.getName() + " &bhas found &e" + veinSize + " &a" + block.getType().toString()));
                }
            }
        }
    }

    public void clientAlert(Player player){
            for(Player players : Bukkit.getOnlinePlayers()){
                if(hasAlertEnabled(players, Alert.PLAYER_CLIENT_NAME) && plugin.getConfig().getBoolean("alerts.client-alert")){
                    String message = plugin.getConfig().getString("alerts.client-message");
                    if(message != null) {
                        message = message.replace("%player%", player.getName());
                        message = message.replace("%client%", player.getClientBrandName());
                        players.sendMessage(CC.send(message));
                    } else {
                        players.sendMessage(CC.send("&b[Staff] &f%player% &b has joined using &f" + player.getClientBrandName()));
                    }
                }
            }
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        initializeAlerts(player);
        clientAlert(player);
        altAlert(player);
    }
}
