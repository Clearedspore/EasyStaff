package me.clearedspore.feature.filter;

import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.easyAPI.util.Logger;
import me.clearedspore.feature.alertManager.Alert;
import me.clearedspore.feature.alertManager.AlertManager;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.util.P;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FilterManager implements Listener {
    private FileConfiguration filterConfig;
    private final JavaPlugin plugin;
    private final PunishmentManager punishmentManager;
    private final Logger logger;
    private final AlertManager alertManager;

    public FilterManager(FileConfiguration filterConfig, JavaPlugin plugin, PunishmentManager punishmentManager, Logger logger, AlertManager alertManager) {
        this.filterConfig = filterConfig;
        this.plugin = plugin;
        this.punishmentManager = punishmentManager;
        this.logger = logger;
        this.alertManager = alertManager;

        if(filterConfig.getBoolean("enabled")) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    public boolean enabled(){
        return filterConfig.getBoolean("enabled");
    }

    public void setFilterConfig(FileConfiguration newConfig) {
        this.filterConfig = newConfig;
    }

    public Set<String> getAllFilteredWords() {
        Set<String> allWords = new HashSet<>();
        for (String word : filterConfig.getKeys(false)) {
            allWords.add(word);
            List<String> variations = filterConfig.getStringList(word + ".variations");
            allWords.addAll(variations);
        }
        return allWords;
    }

    public String checkFilterSettings(String word) {
        if (!filterConfig.contains(word)) {
            return "&cWord not found in filter.";
        }

        boolean punishmentEnabled = filterConfig.getBoolean(word + ".punishment.enabled");
        boolean replacementEnabled = filterConfig.getBoolean(word + ".replacement.enabled");
        String replacement = filterConfig.getString(word + ".replacement.replace", "none");
        boolean cancelEnabled = filterConfig.getBoolean(word + ".cancel.enabled");
        boolean notifyStaffEnabled = filterConfig.getBoolean(word + ".notify-staff.enabled");

        return String.format("&cFilter settings for &b%s:\n" +
                        "&bPunishment: &e%s\n" +
                        "&bReplacement: &e%s\n" +
                        "&bCancel: &e%s\n" +
                        "&bNotify Staff: &e%s",
                word,
                punishmentEnabled ? "yes" : "no",
                replacementEnabled ? replacement : "none",
                cancelEnabled ? "yes" : "no",
                notifyStaffEnabled ? "yes" : "no");
    }

    public String replaceFilteredWords(String message) {
        String normalizedMessage = normalizeMessage(message);
        for (String word : getAllFilteredWords()) {
            List<String> variations = filterConfig.getStringList(word + ".variations");
            for (String variation : variations) {
                if (filterConfig.getBoolean(word + ".replacement.enabled")) {
                    String replacement = filterConfig.getString(word + ".replacement.replace", "****");
                    String normalizedVariation = normalizeMessage(variation);
                    String pattern = "(?i)" + String.join("[^a-zA-Z]*", normalizedVariation.split(""));
                    if (normalizedMessage.contains(normalizedVariation)) {
                        message = message.replaceAll(pattern, replacement);
                    }
                }
            }
        }
        return message;
    }

    private String normalizeMessage(String message) {
        return message.replaceAll("[^a-zA-Z]", "").toLowerCase();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        String normalizedMessage = normalizeMessage(message);

        if (punishmentManager.isPlayerMuted(player)) {
            return;
        }

        String filteredMessage = replaceFilteredWords(message);
        event.setMessage(filteredMessage);

        for (String configWord : filterConfig.getKeys(false)) {
            if (configWord.equals("enabled")) {
                continue;
            }

            String normalizedConfigWord = normalizeMessage(configWord);
            boolean wordFound = normalizedMessage.contains(normalizedConfigWord) && 
                               message.toLowerCase().contains(configWord.toLowerCase());

            List<String> variations = filterConfig.getStringList(configWord + ".variations");
            boolean variationFound = false;
            String foundVariation = "";
            
            for (String variation : variations) {
                String normalizedVariation = normalizeMessage(variation);
                if (normalizedMessage.contains(normalizedVariation) && 
                    message.toLowerCase().contains(variation.toLowerCase())) {
                    variationFound = true;
                    foundVariation = variation;
                    break;
                }
            }

            if (wordFound || variationFound) {
                String displayWord = wordFound ? configWord : foundVariation;

                if (filterConfig.getBoolean(configWord + ".cancel.enabled")) {
                    event.setCancelled(true);
                    
                    List<String> cancelMessageList = filterConfig.getStringList(configWord + ".cancel.message");
                    
                    if (!cancelMessageList.isEmpty()) {
                        for (String cancelMessage : cancelMessageList) {
                            cancelMessage = cancelMessage.replace("%player%", player.getName());
                            cancelMessage = cancelMessage.replace("%word%", displayWord);
                            cancelMessage = cancelMessage.replace("%message%", message);
                            player.sendMessage(CC.send(cancelMessage));
                        }
                    } else {
                        player.sendMessage(CC.sendRed("Your message was blocked due to inappropriate content."));
                    }
                }

                if (filterConfig.getBoolean(configWord + ".notify-staff.enabled")) {
                    List<String> notifyMessages = filterConfig.getStringList(configWord + ".notify-staff.message");
                    
                    if (!notifyMessages.isEmpty()) {
                        for (String notifyMessage : notifyMessages) {
                            notifyMessage = CC.send(notifyMessage
                                    .replace("%player%", player.getName())
                                    .replace("%word%", displayWord)
                                    .replace("%message%", message));
                            
                            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                if (onlinePlayer.hasPermission(P.filter_notify) && alertManager.hasAlertEnabled(player, Alert.STAFF)) {
                                    onlinePlayer.sendMessage(notifyMessage);
                                }
                            }
                        }
                    } else {
                        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            if (onlinePlayer.hasPermission(P.filter_notify) && alertManager.hasAlertEnabled(player, Alert.STAFF)) {
                                onlinePlayer.sendMessage("&b[Staff] &f" + player.getName() + " &bhas flagged the filter for saying &e'" + displayWord + "'");
                            }
                        }
                    }
                }

                if (filterConfig.getBoolean(configWord + ".punishment.enabled")) {
                    List<String> punishCommandList = filterConfig.getStringList(configWord + ".punishment.command");
                    for (String command : punishCommandList) {
                        String finalCommand = command
                                .replace("%player%", player.getName())
                                .replace("%word%", displayWord)
                                .replace("%message%", message);
                        
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                        });
                    }
                }

                break;
            }
        }
    }
}
