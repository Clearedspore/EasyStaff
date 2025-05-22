package me.clearedspore.feature.punishment;

import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.alertManager.Alert;
import me.clearedspore.feature.alertManager.AlertManager;
import me.clearedspore.feature.discord.DiscordManager;
import me.clearedspore.storage.PlayerData;
import me.clearedspore.util.PS;
import me.clearedspore.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class PunishmentManager implements Listener {

    private final PlayerData playerData;
    private FileConfiguration reasonsConfig;
    private final Map<OfflinePlayer, List<Punishment>> playerPunishments = new HashMap<>();
    private final JavaPlugin plugin;
    private final List<String> exemptPlayers = new ArrayList<>();
    private DiscordManager discordManager;
    private final AlertManager alertManager;
    private HiddenPunishmentManager hiddenPunishmentManager;

    private final Map<Player, Long> lastMessageTime = new HashMap<>();

    public PlayerData getPlayerData() {
        return playerData;
    }

    public PunishmentManager(PlayerData playerData, FileConfiguration reasonsConfig, JavaPlugin plugin, AlertManager alertManager) {
        this.playerData = playerData;
        this.reasonsConfig = reasonsConfig;
        this.plugin = plugin;
        this.alertManager = alertManager;

        loadExistingBans();
        loadExemptPlayers();
    }
    
    public void setHiddenPunishmentManager(HiddenPunishmentManager hiddenPunishmentManager) {
        this.hiddenPunishmentManager = hiddenPunishmentManager;
    }
    
    public HiddenPunishmentManager getHiddenPunishmentManager() {
        return hiddenPunishmentManager;
    }
    
    public void setDiscordManager(DiscordManager discordManager) {
        this.discordManager = discordManager;
    }

    public void setReasonsConfig(FileConfiguration newConfig) {
        this.reasonsConfig = newConfig;
    }

    private String generateShortId(int length) {
        String characters = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder id = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            id.append(characters.charAt(random.nextInt(characters.length())));
        }
        return id.toString();
    }

    private void loadExistingBans() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayerBans(player);
        }
    }

    public void loadPlayerBans(OfflinePlayer player) {
        FileConfiguration playerConfig = playerData.getPlayerConfig(player);
        if (playerConfig == null || !playerConfig.contains("punishments.bans")) {
            return;
        }

        List<Punishment> punishments = new ArrayList<>();

        for (String banId : playerConfig.getConfigurationSection("punishments.bans").getKeys(false)) {
            boolean active = playerConfig.getBoolean("punishments.bans." + banId + ".active", false);
            String reason = playerConfig.getString("punishments.bans." + banId + ".reason", "Unknown reason");
            String issuerName = playerConfig.getString("punishments.bans." + banId + ".issuedBy", "Unknown");
            long expirationTime = playerConfig.getLong("punishments.bans." + banId + ".expirationTime", -1);

            if (active && expirationTime > 0 && System.currentTimeMillis() >= expirationTime) {
                playerConfig.set("punishments.bans." + banId + ".active", false);
                playerConfig.set("punishments.bans." + banId + ".unbannedBy", "CONSOLE");
                playerConfig.set("punishments.bans." + banId + ".unbanReason", "Expired");
                playerConfig.set("punishments.bans." + banId + ".unbannedAt", System.currentTimeMillis());
                playerData.savePlayerData(playerConfig, playerData.getPlayerFile(player));
                active = false;
            }

            String removalIssuerName = null;
            String removalReason = null;
            if (!active) {
                removalIssuerName = playerConfig.getString("punishments.bans." + banId + ".unbannedBy", null);
                removalReason = playerConfig.getString("punishments.bans." + banId + ".unbanReason", null);
            }

            Player issuer = Bukkit.getPlayer(issuerName);
            Player removalIssuer = removalIssuerName != null ? Bukkit.getPlayer(removalIssuerName) : null;

            CommandSender finalRemovalIssuer = removalIssuer;
            if (removalIssuer == null && removalIssuerName != null) {
                finalRemovalIssuer = Bukkit.getConsoleSender();
            }

            Punishment punishment = new Punishment(issuer, player, reason, active, PunishmentType.BAN, banId, 
                                                  finalRemovalIssuer, removalReason, expirationTime, System.currentTimeMillis());
            punishments.add(punishment);
        }

        playerPunishments.put(player, punishments);
    }

    private FileConfiguration loadReasonsConfig() {
        File reasonsFile = new File("plugins/EasyStaff/reasons.yml");
        if (!reasonsFile.exists()) {
            throw new RuntimeException("reasons.yml file not found!");
        }
        return YamlConfiguration.loadConfiguration(reasonsFile);
    }

    public void banPlayer(CommandSender player, OfflinePlayer target, String reason, boolean silent, boolean hideStaffMessage) {
        tempBanPlayer(player, target, reason, -1, silent, hideStaffMessage);
    }

    public void mutePlayer(CommandSender player, OfflinePlayer target, String reason,  boolean silent, boolean hideStaffMessage) {
        tempMutePlayer(player, target, reason, -1, silent, hideStaffMessage);
    }

    public void warnPlayer(CommandSender player, OfflinePlayer target, String reason, boolean silent, boolean hideStaffMessage){
        File playerFile = playerData.getPlayerFile(target);
        FileConfiguration playerConfig = playerData.getPlayerConfig(target);

        String warnID = generateShortId(5);

        Punishment punishment = new Punishment(
                player,
                target,
                reason,
                true,
                PunishmentType.WARN,
                warnID
        );

        List<Punishment> punishments = playerPunishments.getOrDefault(target, new ArrayList<>());
        punishments.add(punishment);
        playerPunishments.put(target, punishments);

        playerConfig.set("punishments.warns." + warnID + ".reason", reason);
        playerConfig.set("punishments.warns." + warnID + ".issuedBy", player.getName());
        playerConfig.set("punishments.warns." + warnID + ".timestamp", System.currentTimeMillis());
        playerConfig.set("punishments.warns." + warnID + ".active", true);

        int offenseCount = playerConfig.getInt("offenses." + reason, 0);
        playerConfig.set("punishments.warns." + warnID + ".offenseCount", offenseCount);

        playerData.savePlayerData(playerConfig, playerFile);

        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null) {
                List<String> reasonMessageList = reasonsConfig.getStringList("warns");
                if (reasonMessageList.isEmpty()) {
                    onlineTarget.sendMessage(CC.send("&c================================"));
                    onlineTarget.sendMessage(CC.send(""));
                    onlineTarget.sendMessage(CC.send("&cYou have been warned!"));
                    onlineTarget.sendMessage(CC.send(""));
                    onlineTarget.sendMessage(CC.send("&fReason: &e" + reason));
                    onlineTarget.sendMessage(CC.send(""));
                    onlineTarget.sendMessage(CC.send("&fIf you believe this is a mistake you can open a ticket in our discord."));
                    onlineTarget.sendMessage(CC.send("&fPunishment ID: &e" + warnID));
                    onlineTarget.sendMessage(CC.send("&c================================"));
                } else {
                    for (String message : reasonMessageList) {
                        onlineTarget.sendMessage(CC.send(message.replace("%reason%", reason).replace("%id%", warnID)));
                    }
                }
            }
        }

        for (Player players : Bukkit.getOnlinePlayers()) {
            if (reasonsConfig.getBoolean("punishmentnotify.enabled") && !silent) {
                List<String> messageList = reasonsConfig.getStringList("punishmentnotify.text");
                if(!messageList.isEmpty()){
                    for (String message : messageList) {
                        message = message.replace("%player%", target.getName());
                        message = message.replace("%reason%", reason);
                        message = message.replace("%issuer%", player.getName());
                        players.sendMessage(CC.send(message));
                    }
                } else {
                    players.sendMessage(CC.send("&cSaftey!"));
                    players.sendMessage(CC.send("&bfA player just got banned!"));
                    players.sendMessage(CC.send("&bfMake sure to use /report to keep the server safe!"));
                }
            }
        }

        if (!hideStaffMessage) {
            for (Player players : Bukkit.getOnlinePlayers()) {
                if (players.hasPermission(PS.punish_notify) && alertManager.hasAlertEnabled(players, Alert.STAFF)) {
                    players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEwarned &f" + target.getName() + " &#00CCDEfor &6(" + reason + ") " + (silent ? "&c(Silent)" : "")));
                }
            }
        }

        notifyHighStaff(player, target, reason, "warned", hideStaffMessage);

        if (discordManager != null && discordManager.isEnabled()) {
            if (player instanceof Player) {
                Player staffPlayer = (Player) player;
                if (!discordManager.isLinked(staffPlayer) && plugin.getConfig().getBoolean("discord.threads.ping-issuer", true)) {
                    staffPlayer.sendMessage(CC.send(plugin.getConfig().getString("discord.messages.link-required",
                            "&c[Discord] &fYou need to link your Discord account to use this feature. Use &b/staff-link&f to link your account.")));
                }
            }

            discordManager.sendPunishmentNotification(punishment);
        }
    }

    public void kickPlayer(CommandSender player, Player target, String reason, boolean silent, boolean hideStaffMessage){
        File playerFile = playerData.getPlayerFile(target);
        FileConfiguration playerConfig = playerData.getPlayerConfig(target);

        String kickID = generateShortId(5);

        List<String> reasonList = reasonsConfig.getStringList("kicks");
        if (reasonList.isEmpty()) {
            reasonList.add("&cYou have been kicked!");
            reasonList.add("");
            reasonList.add("&fReason: &e%reason%");
            reasonList.add("");
            reasonList.add("&fAppeal at &bdiscord.gg/");
            reasonList.add("&fIf you believe this is a mistake you can open a ticket in our discord.");
            reasonList.add("&fPunishment ID: &e%id%");
        }

        String formattedReason = String.join("\n", reasonList);
        formattedReason = formattedReason.replace("%reason%", reason);
        formattedReason = formattedReason.replace("%id%", kickID);

        Punishment punishment = new Punishment(
                player,
                target,
                reason,
                true,
                PunishmentType.KICK,
                kickID
        );

        List<Punishment> punishments = playerPunishments.getOrDefault(target, new ArrayList<>());
        punishments.add(punishment);
        playerPunishments.put(target, punishments);

        playerConfig.set("punishments.kicks." + kickID + ".reason", reason);
        playerConfig.set("punishments.kicks." + kickID + ".issuedBy", player.getName());
        playerConfig.set("punishments.kicks." + kickID + ".timestamp", System.currentTimeMillis());
        playerConfig.set("punishments.kicks." + kickID + ".active", false);

        int offenseCount = playerConfig.getInt("offenses." + reason, 0);
        playerConfig.set("punishments.kicks." + kickID + ".offenseCount", offenseCount);

        playerData.savePlayerData(playerConfig, playerFile);

        target.kickPlayer(CC.translate(formattedReason));

        for (Player players : Bukkit.getOnlinePlayers()) {
            if (reasonsConfig.getBoolean("punishmentnotify.enabled") && !silent) {
                List<String> messageList = reasonsConfig.getStringList("punishmentnotify.text");
                if(!messageList.isEmpty()){
                    for (String message : messageList) {
                        message = message.replace("%player%", target.getName());
                        message = message.replace("%reason%", reason);
                        message = message.replace("%issuer%", player.getName());
                        players.sendMessage(CC.send(message));
                    }
                } else {
                    players.sendMessage(CC.send("&cSaftey!"));
                    players.sendMessage(CC.send("&bfA player just got banned!"));
                    players.sendMessage(CC.send("&bfMake sure to use /report to keep the server safe!"));
                }
            }
        }

        if (!hideStaffMessage) {
            for (Player players : Bukkit.getOnlinePlayers()) {
                if (players.hasPermission(PS.punish_notify) && alertManager.hasAlertEnabled(players, Alert.STAFF)) {
                    players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEkicked &f" + target.getName() + " &#00CCDEfor &6(" + reason + ") " + (silent ? "&c(Silent)" : "")));
                }
            }
        }

        notifyHighStaff(player, target, reason, "kicked", hideStaffMessage);

        if (discordManager != null && discordManager.isEnabled()) {
            if (player instanceof Player) {
                Player staffPlayer = (Player) player;
                if (!discordManager.isLinked(staffPlayer) && plugin.getConfig().getBoolean("discord.threads.ping-issuer", true)) {
                    staffPlayer.sendMessage(CC.send(plugin.getConfig().getString("discord.messages.link-required",
                            "&c[Discord] &fYou need to link your Discord account to use this feature. Use &b/staff-link&f to link your account.")));
                }
            }

            discordManager.sendPunishmentNotification(punishment);
        }
    }

    public List<Punishment> getAllKicks(OfflinePlayer player) {
        List<Punishment> kicks = new ArrayList<>();
        FileConfiguration playerConfig = playerData.getPlayerConfig(player);

        if (playerConfig == null || !playerConfig.contains("punishments.kicks")) {
            return kicks;
        }

        for (String kickID : playerConfig.getConfigurationSection("punishments.kicks").getKeys(false)) {
            boolean active = playerConfig.getBoolean("punishments.kicks." + kickID + ".active", false);
            String reason = playerConfig.getString("punishments.kicks." + kickID + ".reason", "Unknown reason");
            String issuerName = playerConfig.getString("punishments.kicks." + kickID + ".issuedBy", "Unknown");
            long timestamp = playerConfig.getLong("punishments.kicks." + kickID + ".timestamp", 0);



            Player issuer = Bukkit.getPlayer(issuerName);

            Punishment punishment;
            if (!active) {
                punishment = new Punishment(
                        issuer,
                        player,
                        reason,
                        active,
                        PunishmentType.KICK,
                        kickID
                );
            } else {
                punishment = new Punishment(issuer,
                        player,
                        reason,
                        active,
                        PunishmentType.KICK,
                        kickID,
                        null,
                        null
                );
            }

            kicks.add(punishment);
        }

        kicks.sort((p1, p2) -> {
            if (p1.active() != p2.active()) {
                return p1.active() ? -1 : 1;
            }
            return Long.compare(
                    playerConfig.getLong("punishments.kicks." + p2.ID() + ".timestamp"),
                    playerConfig.getLong("punishments.kicks." + p1.ID() + ".timestamp")
            );
        });

        return kicks;
    }

    public boolean removeWarning(CommandSender player, OfflinePlayer target, String warnID, String reason,  boolean silent, boolean hideStaffMessage) {
        if (playerPunishments.containsKey(target)) {
            List<Punishment> punishments = playerPunishments.get(target);
            List<Punishment> updatedPunishments = new ArrayList<>();

            for (Punishment punishment : punishments) {
                if (punishment.punishmentType() == PunishmentType.WARN && punishment.active() && punishment.ID().equals(warnID)) {
                    Punishment inactivePunishment = new Punishment(
                            punishment.issuer(),
                            punishment.target(),
                            punishment.reason(),
                            false,
                            punishment.punishmentType(),
                            punishment.ID(),
                            player,
                            reason
                    );
                    updatedPunishments.add(inactivePunishment);
                } else {
                    updatedPunishments.add(punishment);
                }

                if(target.isOnline()){
                    Player onlineTarget = target.getPlayer();
                    if(punishment.ID().equals(warnID)) {
                        onlineTarget.sendMessage(CC.sendBlue("================================"));
                        onlineTarget.sendMessage(CC.sendBlue("Your warning has been removed!"));
                        onlineTarget.sendMessage(CC.send(""));
                        onlineTarget.sendMessage(CC.send("&fWarning Reason: " + punishment.reason()));
                        onlineTarget.sendMessage(CC.send("&fWarning ID: " + punishment.ID()));
                        onlineTarget.sendMessage(CC.send(""));
                        onlineTarget.sendMessage(CC.send("&fRemoval Reason: " + reason));
                        onlineTarget.sendMessage(CC.send(""));
                        onlineTarget.sendMessage(CC.sendBlue("================================"));
                    }
                }
            }

            playerPunishments.put(target, updatedPunishments);
        }

        FileConfiguration playerConfig = playerData.getPlayerConfig(target);
        File playerFile = playerData.getPlayerFile(target);

        if (playerConfig.contains("punishments.warns." + warnID)) {
            boolean active = playerConfig.getBoolean("punishments.warns." + warnID + ".active", false);
            if (active) {
                playerConfig.set("punishments.warns." + warnID + ".active", false);
                playerConfig.set("punishments.warns." + warnID + ".unwarnedBy", player.getName());
                playerConfig.set("punishments.warns." + warnID + ".unwarnReason", reason);
                playerConfig.set("punishments.warns." + warnID + ".unwarnedAt", System.currentTimeMillis());
            }
        }

        playerData.savePlayerData(playerConfig, playerFile);

        if (!hideStaffMessage) {
            for (Player players : Bukkit.getOnlinePlayers()) {
                if (players.hasPermission(PS.punish_notify) && alertManager.hasAlertEnabled(players, Alert.STAFF)) {
                    players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEunwarned " + target.getName() + "&#00CCDE for &6(" + reason + ") " + (silent ? "&c(Silent)" : "")));
                }
            }
        }

        return true;
    }

    public boolean isWarnActive(OfflinePlayer target, String warnID) {
        FileConfiguration playerConfig = playerData.getPlayerConfig(target);

        if (playerConfig == null) {
            return false;
        }

        if (playerConfig.contains("punishments.warns." + warnID)) {
            return playerConfig.getBoolean("punishments.warns." + warnID + ".active", false);
        }

        return false;
    }

    public List<Punishment> getActiveWarns(OfflinePlayer player) {
        return getAllWarns(player).stream()
                .filter(Punishment::active)
                .toList();
    }

    public List<Punishment> getAllWarns(OfflinePlayer player) {
        List<Punishment> warns = new ArrayList<>();
        FileConfiguration playerConfig = playerData.getPlayerConfig(player);

        if (playerConfig == null || !playerConfig.contains("punishments.warns")) {
            return warns;
        }

        for (String warnID : playerConfig.getConfigurationSection("punishments.warns").getKeys(false)) {
            boolean active = playerConfig.getBoolean("punishments.warns." + warnID + ".active", false);
            String reason = playerConfig.getString("punishments.warns." + warnID + ".reason", "Unknown reason");
            String issuerName = playerConfig.getString("punishments.warns." + warnID + ".issuedBy", "Unknown");
            long timestamp = playerConfig.getLong("punishments.warns." + warnID + ".timestamp", 0);


            String removalIssuerName = null;
            String removalReason = null;
            if (!active) {
                removalIssuerName = playerConfig.getString("punishments.warns." + warnID + ".unwarnedBy", null);
                removalReason = playerConfig.getString("punishments.warns." + warnID + ".unwarnReason", null);
            }

            Player issuer = Bukkit.getPlayer(issuerName);
            Player removalIssuer = removalIssuerName != null ? Bukkit.getPlayer(removalIssuerName) : null;
            CommandSender finalRemovalIssuer = removalIssuer;
            if (removalIssuer == null && removalIssuerName != null) {
                finalRemovalIssuer = Bukkit.getConsoleSender();
            }

            Punishment punishment;
            if (!active && finalRemovalIssuer != null && removalReason != null) {
                punishment = new Punishment(
                        issuer,
                        player,
                        reason,
                        active,
                        PunishmentType.WARN,
                        warnID,
                        finalRemovalIssuer,
                        removalReason
                );
            } else if (!active) {
                punishment = new Punishment(
                        issuer,
                        player,
                        reason,
                        active,
                        PunishmentType.WARN,
                        warnID,
                        removalIssuerName != null ? Bukkit.getConsoleSender() : null,
                        removalReason
                );
            } else {
                punishment = new Punishment(issuer,
                        player,
                        reason,
                        active,
                        PunishmentType.WARN,
                        warnID,
                        null,
                        null
                );
            }

            warns.add(punishment);
        }

        warns.sort((p1, p2) -> {
            if (p1.active() != p2.active()) {
                return p1.active() ? -1 : 1;
            }
            return Long.compare(
                    playerConfig.getLong("punishments.warns." + p2.ID() + ".timestamp"),
                    playerConfig.getLong("punishments.warns." + p1.ID() + ".timestamp")
            );
        });

        return warns;
    }

    public Punishment getPunishmentById(String punishmentId) {
        for (List<Punishment> punishments : playerPunishments.values()) {
            for (Punishment punishment : punishments) {
                if (punishment.ID().equals(punishmentId)) {
                    return punishment;
                }
            }
        }
        return null;
    }

    public void clearPunishments(CommandSender player, OfflinePlayer target) {
        playerPunishments.remove(target);

        FileConfiguration playerConfig = playerData.getPlayerConfig(target);
        File playerFile = playerData.getPlayerFile(target);

        if (playerConfig != null) {
            playerConfig.set("punishments.bans", null);
            playerConfig.set("punishments.mutes", null);
            playerConfig.set("punishments.warns", null);
            playerConfig.set("punishments.kicks", null);
            playerConfig.set("offenses", null);

            playerData.savePlayerData(playerConfig, playerFile);
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission(PS.punish_notify_high)) {
                onlinePlayer.sendMessage(CC.sendBlue("[High Staff] &f" + player.getName() + " &#00CCDEhas cleared &f" + target.getName() + "'s &#00CCDEpunishment history"));
            }
        }
    }

    public void tempMutePlayer(CommandSender player, OfflinePlayer target, String reason, long expirationTime, boolean silent, boolean hideStaffMessage) {
        File playerFile = playerData.getPlayerFile(target);
        FileConfiguration playerConfig = playerData.getPlayerConfig(target);

        String muteId = generateShortId(5);

        String timeLeftFormatted;
        if (expirationTime > 0) {
            long remainingTime = expirationTime - System.currentTimeMillis();
            timeLeftFormatted = TimeUtil.formatDuration(remainingTime);
        } else {
            timeLeftFormatted = "Permanent";
        }

        Punishment punishment = new Punishment(player,
                target,
                reason,
                true,
                PunishmentType.MUTE,
                muteId,
                null,
                null,
                expirationTime,
                System.currentTimeMillis()
        );

        List<Punishment> punishments = playerPunishments.getOrDefault(target, new ArrayList<>());
        punishments.add(punishment);
        playerPunishments.put(target, punishments);

        playerConfig.set("punishments.mutes." + muteId + ".reason", reason);
        playerConfig.set("punishments.mutes." + muteId + ".issuedBy", player.getName());
        playerConfig.set("punishments.mutes." + muteId + ".timestamp", System.currentTimeMillis());
        playerConfig.set("punishments.mutes." + muteId + ".active", true);

        int offenseCount = playerConfig.getInt("offenses." + reason, 0);
        playerConfig.set("punishments.mutes." + muteId + ".offenseCount", offenseCount);

        if (expirationTime > 0) {
            playerConfig.set("punishments.mutes." + muteId + ".expirationTime", expirationTime);
        }

        playerData.savePlayerData(playerConfig, playerFile);

        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null) {
                List<String> reasonMessageList = reasonsConfig.getStringList("mutes");
                if (reasonMessageList.isEmpty()) {
                    onlineTarget.sendMessage(CC.send("&c================================"));
                    onlineTarget.sendMessage(CC.send(""));
                    onlineTarget.sendMessage(CC.send("&cYou have been muted!"));
                    onlineTarget.sendMessage(CC.send(""));
                    onlineTarget.sendMessage(CC.send("&fReason: &e" + reason));
                    onlineTarget.sendMessage(CC.send("&fExpires in: &e" + timeLeftFormatted));
                    onlineTarget.sendMessage(CC.send(""));
                    onlineTarget.sendMessage(CC.send("&fIf you believe this is a mistake you can open a ticket in our discord."));
                    onlineTarget.sendMessage(CC.send("&fPunishment ID: &e" + muteId));
                    onlineTarget.sendMessage(CC.send("&c================================"));
                } else {
                    for (String message : reasonMessageList) {
                        message = message.replace("%reason%", reason);
                        message = message.replace("%id%", muteId);
                        message = message.replace("%time_left%", timeLeftFormatted);
                        onlineTarget.sendMessage(CC.send(message));
                    }
                }
            }
        }

        for (Player players : Bukkit.getOnlinePlayers()) {
            if (reasonsConfig.getBoolean("punishmentnotify.enabled") && !silent) {
                List<String> messageList = reasonsConfig.getStringList("punishmentnotify.text");
                if(!messageList.isEmpty()){
                    for (String message : messageList) {
                        message = message.replace("%player%", target.getName());
                        message = message.replace("%reason%", reason);
                        message = message.replace("%issuer%", player.getName());
                        players.sendMessage(CC.send(message));
                    }
                } else {
                    players.sendMessage(CC.send("&cSaftey!"));
                    players.sendMessage(CC.send("&bfA player just got banned!"));
                    players.sendMessage(CC.send("&bfMake sure to use /report to keep the server safe!"));
                }
            }
        }

        String muteType = expirationTime > 0 ? "temporarily muted" : "muted";
        if (!hideStaffMessage) {
            for (Player players : Bukkit.getOnlinePlayers()) {
                if (players.hasPermission(PS.punish_notify) && alertManager.hasAlertEnabled(players, Alert.STAFF)) {
                    if (expirationTime > 0) {
                        players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDE" + muteType + " &f" + target.getName() + " &#00CCDEfor &6(" + reason + ") &#00CCDEfor &c(" + timeLeftFormatted + ") " + (silent ? "&c(Silent)" : "")));
                    } else {
                        players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDE" + muteType + " &f" + target.getName() + " &#00CCDEfor &6(" + reason + ") " + (silent ? "&c(Silent)" : "")));
                    }
                }
            }
        }
        notifyHighStaff(player, target, reason, muteType, hideStaffMessage);

        if (discordManager != null && discordManager.isEnabled()) {
            if (player instanceof Player) {
                Player staffPlayer = (Player) player;
                if (!discordManager.isLinked(staffPlayer) && plugin.getConfig().getBoolean("discord.threads.ping-issuer", true)) {
                    staffPlayer.sendMessage(CC.send(plugin.getConfig().getString("discord.messages.link-required", 
                        "&c[Discord] &fYou need to link your Discord account to use this feature. Use &b/staff-link&f to link your account.")));
                }
            }
            discordManager.sendPunishmentNotification(punishment);
        }
    }

    public boolean isPlayerMuted(OfflinePlayer player) {
        return isPlayerMuted(player, true);
    }

    private boolean isPlayerMuted(OfflinePlayer player, boolean handleExpired) {
        if (playerPunishments.containsKey(player)) {
            List<Punishment> punishments = playerPunishments.get(player);
            for (Punishment punishment : punishments) {
                if (punishment.punishmentType() == PunishmentType.MUTE && punishment.active()) {
                    if (handleExpired && punishment.isTemporary() && punishment.hasExpired()) {
                        handleExpiredMute(player, punishment.ID());
                        return false;
                    }
                    return true;
                }
            }
        }

        FileConfiguration playerConfig = playerData.getPlayerConfig(player);
        if (playerConfig == null) {
            return false;
        }

        if (playerConfig.contains("punishments.mutes")) {
            for (String muteId : playerConfig.getConfigurationSection("punishments.mutes").getKeys(false)) {
                boolean active = playerConfig.getBoolean("punishments.mutes." + muteId + ".active", false);
                if (active) {
                    long expirationTime = playerConfig.getLong("punishments.mutes." + muteId + ".expirationTime", -1);
                    if (handleExpired && expirationTime > 0 && System.currentTimeMillis() >= expirationTime) {
                        handleExpiredMute(player, muteId);
                        continue;
                    }
                    return true;
                }
            }
        }

        return false;
    }


    public List<Punishment> getAllMutes(OfflinePlayer player) {
        List<Punishment> mutes = new ArrayList<>();
        FileConfiguration playerConfig = playerData.getPlayerConfig(player);

        if (playerConfig == null || !playerConfig.contains("punishments.mutes")) {
            return mutes;
        }

        for (String muteId : playerConfig.getConfigurationSection("punishments.mutes").getKeys(false)) {
            boolean active = playerConfig.getBoolean("punishments.mutes." + muteId + ".active", false);
            String reason = playerConfig.getString("punishments.mutes." + muteId + ".reason", "Unknown reason");
            String issuerName = playerConfig.getString("punishments.mutes." + muteId + ".issuedBy", "Unknown");
            long timestamp = playerConfig.getLong("punishments.mutes." + muteId + ".timestamp", 0);
            long expirationTime = playerConfig.getLong("punishments.mutes." + muteId + ".expirationTime", -1);

            if (active && expirationTime > 0 && System.currentTimeMillis() >= expirationTime) {
                handleExpiredMute(player, muteId);
                active = false;
            }

            String removalIssuerName = null;
            String removalReason = null;
            if (!active) {
                removalIssuerName = playerConfig.getString("punishments.mutes." + muteId + ".unmutedBy", null);
                removalReason = playerConfig.getString("punishments.mutes." + muteId + ".unmuteReason", null);
            }

            Player issuer = Bukkit.getPlayer(issuerName);
            Player removalIssuer = removalIssuerName != null ? Bukkit.getPlayer(removalIssuerName) : null;
            CommandSender finalRemovalIssuer = removalIssuer;
            if (removalIssuer == null && removalIssuerName != null) {
                finalRemovalIssuer = Bukkit.getConsoleSender();
            }

            Punishment punishment;
            if (!active && finalRemovalIssuer != null && removalReason != null) {
                punishment = new Punishment(
                        issuer,
                        player,
                        reason,
                        active,
                        PunishmentType.MUTE,
                        muteId,
                        finalRemovalIssuer,
                        removalReason,
                        expirationTime,
                        timestamp
                );
            } else if (!active) {
                punishment = new Punishment(
                        issuer,
                        player,
                        reason,
                        active,
                        PunishmentType.MUTE,
                        muteId,
                        removalIssuerName != null ? Bukkit.getConsoleSender() : null,
                        removalReason != null ? removalReason : "Unknown", expirationTime,
                        timestamp
                );
            } else {
                punishment = new Punishment(issuer,
                        player,
                        reason,
                        active,
                        PunishmentType.MUTE,
                        muteId,
                        null,
                        null,
                        expirationTime,
                        timestamp
                );
            }

            mutes.add(punishment);
        }

        mutes.sort((p1, p2) -> Long.compare(
                playerConfig.getLong("punishments.mutes." + p2.ID() + ".timestamp"),
                playerConfig.getLong("punishments.mutes." + p1.ID() + ".timestamp")
        ));

        return mutes;
    }

    private void handleExpiredMute(OfflinePlayer player, String muteId) {
        FileConfiguration playerConfig = playerData.getPlayerConfig(player);
        if (playerConfig != null) {
            playerConfig.set("punishments.mutes." + muteId + ".active", false);
            playerConfig.set("punishments.mutes." + muteId + ".unmutedBy", "System");
            playerConfig.set("punishments.mutes." + muteId + ".unmuteReason", "Mute expired");
            playerConfig.set("punishments.mutes." + muteId + ".unmutedAt", System.currentTimeMillis());
            playerData.savePlayerData(playerConfig, playerData.getPlayerFile(player));

            if (playerPunishments.containsKey(player)) {
                List<Punishment> punishments = playerPunishments.get(player);
                List<Punishment> updatedPunishments = new ArrayList<>();
                
                for (Punishment punishment : punishments) {
                    if (punishment.punishmentType() == PunishmentType.MUTE && 
                        punishment.active() && 
                        punishment.ID().equals(muteId)) {
                        
                        Punishment inactivePunishment = new Punishment(
                                punishment.issuer(),
                                punishment.target(),
                                punishment.reason(),
                                false,
                                punishment.punishmentType(),
                                punishment.ID(),
                                Bukkit.getConsoleSender(),
                                "Mute expired"
                        );
                        updatedPunishments.add(inactivePunishment);
                    } else {
                        updatedPunishments.add(punishment);
                    }
                }
                
                playerPunishments.put(player, updatedPunishments);
            }
        }
    }

    public List<Punishment> getAllPunishments(OfflinePlayer player) {
        List<Punishment> allPunishments = new ArrayList<>();

        allPunishments.addAll(getAllBans(player));

        allPunishments.addAll(getAllWarns(player));

        allPunishments.addAll(getAllKicks(player));

        allPunishments.addAll(getAllMutes(player));

        return allPunishments;
    }

    public boolean unMutePlayer(CommandSender player, OfflinePlayer target, String reason, boolean silent, boolean hideStaffMessage) {
        if (!isPlayerMuted(target, false)) {
            return false;
        }

        if (playerPunishments.containsKey(target)) {
            List<Punishment> punishments = playerPunishments.get(target);
            List<Punishment> updatedPunishments = new ArrayList<>();

            for (Punishment punishment : punishments) {
                if (punishment.punishmentType() == PunishmentType.MUTE && punishment.active()) {
                    Punishment inactivePunishment = new Punishment(
                            punishment.issuer(),
                            punishment.target(),
                            punishment.reason(),
                            false,
                            punishment.punishmentType(),
                            punishment.ID(),
                            player,
                            reason
                    );
                    updatedPunishments.add(inactivePunishment);
                } else {
                    updatedPunishments.add(punishment);
                }
            }

            playerPunishments.put(target, updatedPunishments);
        }

        FileConfiguration playerConfig = playerData.getPlayerConfig(target);
        File playerFile = playerData.getPlayerFile(target);

        if (playerConfig.contains("punishments.mutes")) {
            for (String muteId : playerConfig.getConfigurationSection("punishments.mutes").getKeys(false)) {
                boolean active = playerConfig.getBoolean("punishments.mutes." + muteId + ".active", false);
                if (active) {
                    playerConfig.set("punishments.mutes." + muteId + ".active", false);
                    playerConfig.set("punishments.mutes." + muteId + ".unmutedBy", player.getName());
                    playerConfig.set("punishments.mutes." + muteId + ".unmuteReason", reason);
                    playerConfig.set("punishments.mutes." + muteId + ".unmutedAt", System.currentTimeMillis());
                }
            }
        }

        playerData.savePlayerData(playerConfig, playerFile);


        if(target.isOnline()){
            Player onlineTarget = target.getPlayer();
            onlineTarget.sendMessage(CC.sendBlue("================================"));
            onlineTarget.sendMessage(CC.sendBlue("You have been unmuted!"));
            onlineTarget.sendMessage(CC.send(""));
            onlineTarget.sendMessage(CC.send("&fReason: " + reason));
            onlineTarget.sendMessage(CC.send(""));
            onlineTarget.sendMessage(CC.sendBlue("================================"));
        }

        if (!hideStaffMessage) {
            for (Player players : Bukkit.getOnlinePlayers()) {
                if (players.hasPermission(PS.punish_notify) && alertManager.hasAlertEnabled(players, Alert.STAFF)) {
                    players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEunmuted " + target.getName() + "&#00CCDE for &6(" + reason + ") " + (silent ? "&c(Silent)" : "")));
                }
            }
        }

        notifyHighStaff(player, target, reason, "unmuted", hideStaffMessage);

        return true;
    }

    public List<Punishment> getActiveMutes(OfflinePlayer player) {
        return getAllMutes(player).stream()
                .filter(Punishment::active)
                .toList();
    }


    public void tempBanPlayer(CommandSender player, OfflinePlayer target, String reason, long expirationTime, boolean silent, boolean hideStaffMessage) {
        File playerFile = playerData.getPlayerFile(target);
        FileConfiguration playerConfig = playerData.getPlayerConfig(target);

        String banId = generateShortId(5);

        List<String> reasonList = reasonsConfig.getStringList("bans");
        if (reasonList.isEmpty()) {
            reasonList.add("&cYou have been banned!");
            reasonList.add("");
            reasonList.add("&fReason: &e%reason%");
            reasonList.add("&fExpires in: &e%time_left%");
            reasonList.add("");
            reasonList.add("&fAppeal at &bdiscord.gg/");
            reasonList.add("&fIf you believe this is a mistake you can open a ticket in our discord.");
            reasonList.add("&fPunishment ID: &e%id%");
        }

        String formattedReason = String.join("\n", reasonList);
        formattedReason = formattedReason.replace("%reason%", reason);
        formattedReason = formattedReason.replace("%id%", banId);
        String timeLeftFormatted;
        if (expirationTime > 0) {
            long remainingTime = expirationTime - System.currentTimeMillis();
            timeLeftFormatted = TimeUtil.formatDuration(remainingTime);
        } else {
            timeLeftFormatted = "Permanent";
        }
        formattedReason = formattedReason.replace("%time_left%", timeLeftFormatted);

        Punishment punishment = new Punishment(player,
                target,
                reason,
                true,
                PunishmentType.BAN,
                banId,
                null,
                null,
                expirationTime,
                System.currentTimeMillis()
        );

        List<Punishment> punishments = playerPunishments.getOrDefault(target, new ArrayList<>());
        punishments.add(punishment);
        playerPunishments.put(target, punishments);

        playerConfig.set("punishments.bans." + banId + ".reason", reason);
        playerConfig.set("punishments.bans." + banId + ".issuedBy", player.getName());
        playerConfig.set("punishments.bans." + banId + ".timestamp", System.currentTimeMillis());
        playerConfig.set("punishments.bans." + banId + ".active", true);

        int offenseCount = playerConfig.getInt("offenses." + reason, 0);
        playerConfig.set("punishments.bans." + banId + ".offenseCount", offenseCount);

        if (expirationTime > 0) {
            playerConfig.set("punishments.bans." + banId + ".expirationTime", expirationTime);
        }

        playerData.savePlayerData(playerConfig, playerFile);

        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null) {
                onlineTarget.kickPlayer(CC.translate(formattedReason));
            }
        }

        for (Player players : Bukkit.getOnlinePlayers()) {
            if (reasonsConfig.getBoolean("punishmentnotify.enabled") && !silent) {
                List<String> messageList = reasonsConfig.getStringList("punishmentnotify.text");
                if(!messageList.isEmpty()){
                for (String message : messageList) {
                    message = message.replace("%player%", target.getName());
                    message = message.replace("%reason%", reason);
                    message = message.replace("%issuer%", player.getName());
                    players.sendMessage(CC.send(message));
                }
                } else {
                    players.sendMessage(CC.send("&cSaftey!"));
                    players.sendMessage(CC.send("&bfA player just got banned!"));
                    players.sendMessage(CC.send("&bfMake sure to use /report to keep the server safe!"));
                }
            }
        }

        String banType = expirationTime > 0 ? "temporarily banned" : "banned";
        if (!hideStaffMessage) {
            for (Player players : Bukkit.getOnlinePlayers()) {
                if (players.hasPermission(PS.punish_notify) && alertManager.hasAlertEnabled(players, Alert.STAFF)) {
                    if (expirationTime > 0) {
                        players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDE" + banType + " &f" + target.getName() + " &#00CCDEfor &6(" + reason + ") &#00CCDEfor &c(" + timeLeftFormatted + ") " + (silent ? "&c(Silent)" : "")));
                    } else {
                        players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDE" + banType + " &f" + target.getName() + " &#00CCDEfor &6(" + reason + ") " + (silent ? "&c(Silent)" : "")));
                    }
                }
            }
        }
        notifyHighStaff(player, target, reason, banType, hideStaffMessage);


        if (discordManager != null && discordManager.isEnabled()) {
            if (player instanceof Player) {
                Player staffPlayer = (Player) player;
                if (!discordManager.isLinked(staffPlayer) && plugin.getConfig().getBoolean("discord.threads.ping-issuer", true)) {
                    staffPlayer.sendMessage(CC.send(plugin.getConfig().getString("discord.messages.link-required", 
                        "&c[Discord] &fYou need to link your Discord account to use this feature. Use &b/staff-link&f to link your account.")));
                }
            }
            
            discordManager.sendPunishmentNotification(punishment);
        }
    }

    public boolean isPlayerBanned(OfflinePlayer player) {
        if (playerPunishments.containsKey(player)) {
            List<Punishment> punishments = playerPunishments.get(player);
            for (Punishment punishment : punishments) {
                if (punishment.punishmentType() == PunishmentType.BAN && punishment.active()) {
                    if (punishment.isTemporary() && punishment.hasExpired()) {
                        unbanPlayer(Bukkit.getConsoleSender(), player, "Ban expired", false, false);
                        return false;
                    }
                    return true;
                }
            }
        }

        FileConfiguration playerConfig = playerData.getPlayerConfig(player);
        if (playerConfig == null) {
            return false;
        }

        if (playerConfig.contains("punishments.bans")) {
            for (String banId : playerConfig.getConfigurationSection("punishments.bans").getKeys(false)) {
                boolean active = playerConfig.getBoolean("punishments.bans." + banId + ".active", false);
                if (active) {
                    long expirationTime = playerConfig.getLong("punishments.bans." + banId + ".expirationTime", -1);
                    if (expirationTime > 0 && System.currentTimeMillis() >= expirationTime) {
                        playerConfig.set("punishments.bans." + banId + ".active", false);
                        playerConfig.set("punishments.bans." + banId + ".unbannedBy", "System");
                        playerConfig.set("punishments.bans." + banId + ".unbanReason", "Ban expired");
                        playerConfig.set("punishments.bans." + banId + ".unbannedAt", System.currentTimeMillis());
                        playerData.savePlayerData(playerConfig, playerData.getPlayerFile(player));
                        continue;
                    }
                    return true;
                }
            }
        }

        return false;
    }

    public List<Punishment> getAllBans(OfflinePlayer player) {
        List<Punishment> bans = new ArrayList<>();
        FileConfiguration playerConfig = playerData.getPlayerConfig(player);

        if (playerConfig == null || !playerConfig.contains("punishments.bans")) {
            return bans;
        }

        for (String banId : playerConfig.getConfigurationSection("punishments.bans").getKeys(false)) {
            boolean active = playerConfig.getBoolean("punishments.bans." + banId + ".active", false);
            String reason = playerConfig.getString("punishments.bans." + banId + ".reason", "Unknown reason");
            String issuerName = playerConfig.getString("punishments.bans." + banId + ".issuedBy", "Unknown");
            long timestamp = playerConfig.getLong("punishments.bans." + banId + ".timestamp", 0);
            long expirationTime = playerConfig.getLong("punishments.bans." + banId + ".expirationTime", -1);

            if (active && expirationTime > 0 && System.currentTimeMillis() >= expirationTime) {
                playerConfig.set("punishments.bans." + banId + ".active", false);
                playerConfig.set("punishments.bans." + banId + ".unbannedBy", "System");
                playerConfig.set("punishments.bans." + banId + ".unbanReason", "Ban expired");
                playerConfig.set("punishments.bans." + banId + ".unbannedAt", System.currentTimeMillis());
                playerData.savePlayerData(playerConfig, playerData.getPlayerFile(player));
                active = false;
            }

            String removalIssuerName = null;
            String removalReason = null;
            if (!active) {
                removalIssuerName = playerConfig.getString("punishments.bans." + banId + ".unbannedBy", null);
                removalReason = playerConfig.getString("punishments.bans." + banId + ".unbanReason", null);
            }

            Player issuer = Bukkit.getPlayer(issuerName);
            Player removalIssuer = removalIssuerName != null ? Bukkit.getPlayer(removalIssuerName) : null;
            CommandSender finalRemovalIssuer = removalIssuer;
            if (removalIssuer == null && removalIssuerName != null) {
                finalRemovalIssuer = Bukkit.getConsoleSender();
            }

            Punishment punishment;
            if (!active && finalRemovalIssuer != null && removalReason != null) {
                punishment = new Punishment(
                        issuer,
                        player,
                        reason,
                        active,
                        PunishmentType.BAN,
                        banId,
                        finalRemovalIssuer,
                        removalReason,
                        expirationTime,
                        timestamp
                );
            } else if (!active) {
                punishment = new Punishment(
                        issuer,
                        player,
                        reason,
                        active,
                        PunishmentType.BAN,
                        banId,
                    removalIssuerName != null ? Bukkit.getConsoleSender() : null,
                    removalReason != null ? removalReason : "Unknown", expirationTime,
                        timestamp
                );
            } else {
                punishment = new Punishment(issuer,
                        player,
                        reason,
                        active,
                        PunishmentType.BAN,
                        banId,
                        null,
                        null,
                        expirationTime,
                        timestamp
                );
            }

            bans.add(punishment);
        }

        bans.sort((p1, p2) -> Long.compare(
                playerConfig.getLong("punishments.bans." + p2.ID() + ".timestamp"),
                playerConfig.getLong("punishments.bans." + p1.ID() + ".timestamp")
        ));

        return bans;
    }



    public List<Punishment> getActiveBans(OfflinePlayer player) {
        return getAllBans(player).stream()
                .filter(Punishment::active)
                .toList();
    }

    public boolean unbanPlayer(CommandSender player, OfflinePlayer target, String reason, boolean silent, boolean hideStaffMessage) {
        if (!isPlayerBanned(target)) {
            return false;
        }

        if (playerPunishments.containsKey(target)) {
            List<Punishment> punishments = playerPunishments.get(target);
            List<Punishment> updatedPunishments = new ArrayList<>();

            for (Punishment punishment : punishments) {
                if (punishment.punishmentType() == PunishmentType.BAN && punishment.active()) {
                    Punishment inactivePunishment = new Punishment(
                            punishment.issuer(),
                            punishment.target(),
                            punishment.reason(),
                            false,
                            punishment.punishmentType(),
                            punishment.ID(),
                            player,
                            reason
                    );
                    updatedPunishments.add(inactivePunishment);
                } else {
                    updatedPunishments.add(punishment);
                }
            }

            playerPunishments.put(target, updatedPunishments);
        }

        FileConfiguration playerConfig = playerData.getPlayerConfig(target);
        File playerFile = playerData.getPlayerFile(target);

        if (playerConfig.contains("punishments.bans")) {
            for (String banId : playerConfig.getConfigurationSection("punishments.bans").getKeys(false)) {
                boolean active = playerConfig.getBoolean("punishments.bans." + banId + ".active", false);
                if (active) {
                    playerConfig.set("punishments.bans." + banId + ".active", false);
                    playerConfig.set("punishments.bans." + banId + ".unbannedBy", player.getName());
                    playerConfig.set("punishments.bans." + banId + ".unbanReason", reason);
                    playerConfig.set("punishments.bans." + banId + ".unbannedAt", System.currentTimeMillis());
                }
            }
        }

        playerData.savePlayerData(playerConfig, playerFile);


        if (!hideStaffMessage) {
            for (Player players : Bukkit.getOnlinePlayers()) {
                if (players.hasPermission(PS.punish_notify) && alertManager.hasAlertEnabled(players, Alert.STAFF)) {
                    players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEunbanned " + target.getName() + "&#00CCDE for &6(" + reason + ") " + (silent ? "&c(Silent)" : "")));
                }
            }
        }

        notifyHighStaff(player, target, reason, "unbanned", hideStaffMessage);

        return true;
    }

    private void notifyHighStaff(CommandSender player, OfflinePlayer target, String reason, String action, boolean hideStaffMessage) {
        if (hideStaffMessage) {
            for (Player players : Bukkit.getOnlinePlayers()) {
                if (players.hasPermission(PS.punish_notify_high) && alertManager.hasAlertEnabled(players, Alert.STAFF)) {
                    players.sendMessage(CC.sendBlue("[High Staff] &f" + player.getName() + " &#00CCDE" + action + " &f" + target.getName() + " &#00CCDEfor &6(" + reason + ")"));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        if (isPlayerBanned(player)) {
            String reason = "You are banned from this server.";
            List<Punishment> activeBans = getActiveBans(player);
            if (!activeBans.isEmpty()) {
                reason = activeBans.get(0).reason();
            }

            List<String> reasonList = reasonsConfig.getStringList("bans");
            if (reasonList.isEmpty()) {
                reasonList.add("&cYou have been banned!");
                reasonList.add("");
                reasonList.add("&fReason: &e%reason%");
                reasonList.add("&fExpires in: &e%time_left%");
                reasonList.add("");
                reasonList.add("&fIf you believe this is a mistake you can open a ticket in our discord.");
                reasonList.add("&fAppeal at &bdiscord.gg/");
                reasonList.add("&fPunishment ID: &e%id%");
            }

            long remainingTime = activeBans.get(0).getRemainingTime();

            String formattedReason = String.join("\n", reasonList);
            formattedReason = formattedReason.replace("%reason%", reason);
            formattedReason = formattedReason.replace("%id%", activeBans.get(0).ID());
            String timeLeftFormatted = remainingTime > 0 ?
                    TimeUtil.formatDuration(remainingTime) :
                    "Permanent";
            formattedReason = formattedReason.replace("%time_left%", timeLeftFormatted);
            formattedReason = CC.translate(formattedReason);

            String finalReason = reason;
            new BukkitRunnable() {
                @Override
                public void run() {
                    long currentTime = System.currentTimeMillis();
                    long cooldown = 500;

                    for (Player players : Bukkit.getOnlinePlayers()) {
                        if (players.hasPermission(PS.punish_notify) && alertManager.hasAlertEnabled(players, Alert.STAFF)) {
                            long lastSent = lastMessageTime.getOrDefault(players, 0L);

                            if (currentTime - lastSent >= cooldown) {
                                players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEtried to join while being banned for &f" + finalReason + " &c(" + timeLeftFormatted + " remaining)"));
                                lastMessageTime.put(players, currentTime);
                            }
                        }
                    }
                }
            }.runTaskLater(plugin, 10L);

            event.disallow(Result.KICK_BANNED, formattedReason);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        loadPlayerBans(player);


        if(isPlayerMuted(player, true)){
            for(Punishment punishment : getActiveMutes(player)) {
                long expirationTime = punishment.expirationTime();
                String timeLeftFormatted;
                if (expirationTime > 0) {
                    long remainingTime = expirationTime - System.currentTimeMillis();
                    timeLeftFormatted = TimeUtil.formatDuration(remainingTime);
                } else {
                    timeLeftFormatted = "Permanent";
                }
                player.sendMessage(CC.send("&c================================"));
                player.sendMessage(CC.send(""));
                player.sendMessage(CC.send("&cYou are muted!"));
                player.sendMessage(CC.send(""));
                player.sendMessage(CC.send("&fReason: &e" + punishment.reason()));
                player.sendMessage(CC.send("&fExpires in: &e" + timeLeftFormatted));
                player.sendMessage(CC.send(""));
                player.sendMessage(CC.send("&fIf you believe this is a mistake you can open a ticket in our discord."));
                player.sendMessage(CC.send("&fPunishment ID: &e" + punishment.ID()));
                player.sendMessage(CC.send("&c================================"));
            }
        }
    }

    @EventHandler
    public void onChatEvent(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();

        if(isPlayerMuted(player, true)) {
            for (Punishment punishment : getActiveMutes(player)) {
                long expirationTime = punishment.expirationTime();
                String timeLeftFormatted;
                if (expirationTime > 0) {
                    long remainingTime = expirationTime - System.currentTimeMillis();
                    timeLeftFormatted = TimeUtil.formatDuration(remainingTime);
                } else {
                    timeLeftFormatted = "Permanent";
                }
                event.setCancelled(true);
                player.sendMessage(CC.send("&c================================"));
                player.sendMessage(CC.send(""));
                player.sendMessage(CC.send("&cYou are muted!"));
                player.sendMessage(CC.send(""));
                player.sendMessage(CC.send("&fReason: &e" + punishment.reason()));
                player.sendMessage(CC.send("&fExpires in: &e" + timeLeftFormatted));
                player.sendMessage(CC.send(""));
                player.sendMessage(CC.send("&fIf you believe this is a mistake you can open a ticket in our discord."));
                player.sendMessage(CC.send("&fPunishment ID: &e" + punishment.ID()));
                player.sendMessage(CC.send("&c================================"));
            }
        }
    }

    @EventHandler
    public void onCMD(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();


        if (isPlayerMuted(player)) {
            List<String> blockedCMDS = plugin.getConfig().getStringList("punishments.blocked-cmds");

            for (String blockedCommand : blockedCMDS) {
                if (command.startsWith(blockedCommand)) {
                    event.setCancelled(true);
                    player.sendMessage(CC.sendRed("You are not allowed to use this command while muted!"));
                    break;
                }
            }
        }
    }

    /*
    Punish with offenses feature
     */


    public boolean punishPlayer(CommandSender sender, OfflinePlayer target, String reason, boolean silent, boolean hideStaffMessage) {
        if (isExempt(target.getName())) {
            sender.sendMessage(CC.send("&cThis player is exempt from punishments."));
            return false;
        }

        if (!reasonsConfig.contains("offenses." + reason)) {
            sender.sendMessage(CC.send("&cInvalid punishment reason. This reason is not configured in reasons.yml."));
            return false;
        }

        FileConfiguration playerConfig = playerData.getPlayerConfig(target);
        int offenseCount = playerConfig.getInt("offenses." + reason, 0) + 1;

        String offenseKey = String.valueOf(offenseCount);
        if (!reasonsConfig.contains("offenses." + reason + "." + offenseKey)) {
            if (reasonsConfig.contains("offenses." + reason + ".final")) {
                offenseKey = "final";
            } else {
                int highestOffense = 0;
                for (String key : reasonsConfig.getConfigurationSection("offenses." + reason).getKeys(false)) {
                    if (key.equals("final")) continue;
                    try {
                        int level = Integer.parseInt(key);
                        if (level > highestOffense) {
                            highestOffense = level;
                        }
                    } catch (NumberFormatException ignored) {}
                }
                
                if (highestOffense > 0) {
                    offenseKey = String.valueOf(highestOffense);
                } else {
                    sender.sendMessage(CC.send("&cNo punishment configuration found for this offense."));
                    return false;
                }
            }
        }

        String punishmentTypeStr = reasonsConfig.getString("offenses." + reason + "." + offenseKey + ".type", "WARN");
        String durationStr = reasonsConfig.getString("offenses." + reason + "." + offenseKey + ".duration", "");

        String customReason = reasonsConfig.getString("offenses." + reason + "." + offenseKey + ".reason", null);
        if (customReason != null) {
            reason = customReason;
        }
        
        PunishmentType punishmentType;
        try {
            punishmentType = PunishmentType.valueOf(punishmentTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(CC.send("&cInvalid punishment type in configuration: " + punishmentTypeStr));
            return false;
        }

        String permissionSuffix;
        switch (punishmentType) {
            case BAN:
                permissionSuffix = durationStr.isEmpty() || durationStr.equalsIgnoreCase("permanent") ? "ban" : "tempban";
                break;
            case MUTE:
                permissionSuffix = durationStr.isEmpty() || durationStr.equalsIgnoreCase("permanent") ? "mute" : "tempmute";
                break;
            case WARN:
                permissionSuffix = "warn";
                break;
            case KICK:
                permissionSuffix = "kick";
                break;
            default:
                permissionSuffix = punishmentType.name().toLowerCase();
        }

        if (isPlayerMuted(target) && (permissionSuffix.equals("mute") || permissionSuffix.equals("tempmute"))) {
            sender.sendMessage(CC.sendRed("That player is already muted!"));
            return false;
        }

        if (isPlayerBanned(target) && (permissionSuffix.equals("ban") || permissionSuffix.equals("tempban"))) {
            sender.sendMessage(CC.sendRed("That player is already banned!"));
            return false;
        }


        String permission = PS.punish + "." + permissionSuffix;
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(CC.send("&cYou don't have permission to issue this type of punishment."));
            return false;
        }

        long duration = -1;
        if (!durationStr.isEmpty() && !durationStr.equalsIgnoreCase("permanent")) {
            duration = TimeUtil.parseTime(durationStr);
            if (duration <= 0) {
                sender.sendMessage(CC.send("&cInvalid duration format in configuration: " + durationStr));
                return false;
            }
            duration = System.currentTimeMillis() + duration;
        }

        playerConfig.set("offenses." + reason, offenseCount);
        playerData.savePlayerData(playerConfig, playerData.getPlayerFile(target));
        
        boolean success = true;
        switch (punishmentType) {
            case BAN:
                tempBanPlayer(sender, target, reason, duration, silent, hideStaffMessage);
                break;
            case MUTE:
                tempMutePlayer(sender, target, reason, duration, silent, hideStaffMessage);
                break;
            case WARN:
                warnPlayer(sender, target, reason, silent, hideStaffMessage);
                break;
            case KICK:
                if (target.isOnline() && target.getPlayer() != null) {
                    kickPlayer(sender, target.getPlayer(), reason, silent, hideStaffMessage);
                } else {
                    sender.sendMessage(CC.send("&cCannot kick an offline player."));
                    success = false;
                }
                break;
            default:
                sender.sendMessage(CC.send("&cUnsupported punishment type: " + punishmentType));
                success = false;
        }
        
        if (success) {
            String punishmentName = getPunishmentTypeName(punishmentType, duration);
            sender.sendMessage(CC.send("&aSuccessfully " + punishmentName + " &f" + target.getName() + 
                    " &afor &f" + reason + " &a(Offense #" + offenseCount + ")"));
        }
        
        return success;
    }
    
    private String getPunishmentTypeName(PunishmentType type, long duration) {
        switch (type) {
            case BAN:
                return duration > 0 ? "temporarily banned" : "banned";
            case MUTE:
                return duration > 0 ? "temporarily muted" : "muted";
            case WARN:
                return "warned";
            case KICK:
                return "kicked";
            default:
                return "punished";
        }
    }

    public List<String> getPunishmentReasons() {
        if (reasonsConfig.contains("offenses")) {
            return new ArrayList<>(reasonsConfig.getConfigurationSection("offenses").getKeys(false));
        }
        return new ArrayList<>();
    }


    /*
     Exempt feature settings
     */


    private void loadExemptPlayers() {
        exemptPlayers.addAll(plugin.getConfig().getStringList("punishments.exempt-players"));
    }

    public boolean isExempt(String playerName){
        plugin.reloadConfig();
        List<String> configPlayers = plugin.getConfig().getStringList("punishments.exempt-players");
        if(configPlayers.contains(playerName)){
            return true;
        }
        return false;
    }

    public List<String> getExemptPlayers(){
        plugin.reloadConfig();
        List<String> exemptPlayers = plugin.getConfig().getStringList("punishments.exempt-players");
        return exemptPlayers;
    }

    public void addExemptPlayer(String playerName) {
        if (!exemptPlayers.contains(playerName)) {
            exemptPlayers.add(playerName);
            updateExemptPlayersConfig();
        }
    }

    private void updateExemptPlayersConfig() {
        plugin.reloadConfig();
        plugin.getConfig().set("punishments.exempt-players", exemptPlayers);
        plugin.saveConfig();
    }

    public void removeExemptPlayer(String playerName) {
            exemptPlayers.remove(playerName);
            updateExemptPlayersConfig();
        }
    }
