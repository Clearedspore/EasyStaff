package me.clearedspore.feature.discord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.easyAPI.util.Logger;
import me.clearedspore.feature.channels.DiscordChannelInfo;
import me.clearedspore.feature.punishment.Punishment;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.feature.punishment.PunishmentType;
import me.clearedspore.feature.reports.Report;
import me.clearedspore.storage.PlayerData;
import me.clearedspore.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class DiscordManager {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final PlayerData playerData;
    
    private WebhookClient punishmentWebhook;
    private WebhookClient reportWebhook;
    private WebhookClient chatWebhook;
    private WebhookClient linkWebhook;

    private final Map<UUID, String> linkedDiscordIds = new HashMap<>();
    private File discordLinksFile;
    private FileConfiguration discordLinksConfig;
    private final Map<String, Long> messageIds = new HashMap<>();
    private final Map<String, String> punishmentEvidence = new HashMap<>();

    private final Map<String, Long> verificationMessageIds = new HashMap<>();
    private final Map<UUID, String> pendingVerifications = new HashMap<>();
    private final Map<String, UUID> verificationCodes = new HashMap<>();
    
    private boolean enabled;
    private boolean threadsEnabled;
    private boolean pingIssuer;
    private String serverId;
    private final PunishmentManager punishmentManager;

    public DiscordManager(JavaPlugin plugin, Logger logger, PlayerData playerData, PunishmentManager punishmentManager) {
        this.plugin = plugin;
        this.logger = logger;
        this.playerData = playerData;
        this.punishmentManager = punishmentManager;

        loadConfig();
        loadDiscordLinks();
    }
    
    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        this.enabled = config.getBoolean("discord.enabled", false);
        
        if (!enabled) {
            logger.info("Discord integration is disabled");
            return;
        }
        
        String punishmentWebhookUrl = config.getString("discord.webhooks.punishments", "");
        String reportWebhookUrl = config.getString("discord.webhooks.reports", "");
        String linkWebhookUrl = config.getString("discord.webhooks.linking", "");
        String chatWebhookUrl = config.getString("discord.webhooks.chat", "");
        
        this.serverId = config.getString("discord.server-id", "");
        this.threadsEnabled = config.getBoolean("discord.threads.enabled", true);
        this.pingIssuer = config.getBoolean("discord.ping-issuer", true);
        
        if (!punishmentWebhookUrl.isEmpty()) {
            this.punishmentWebhook = new WebhookClientBuilder(punishmentWebhookUrl).build();
            logger.info("Punishment webhook initialized");
        }
        
        if (!reportWebhookUrl.isEmpty()) {
            this.reportWebhook = new WebhookClientBuilder(reportWebhookUrl).build();
            logger.info("Report webhook initialized");
        }
        
        if (!chatWebhookUrl.isEmpty()) {
            this.chatWebhook = new WebhookClientBuilder(chatWebhookUrl).build();
            logger.info("Chat webhook initialized");
        }

        if (!linkWebhookUrl.isEmpty()) {
            this.linkWebhook = new WebhookClientBuilder(linkWebhookUrl).build();
            logger.info("Link webhook initialized");
        }
    }
    
    private void loadDiscordLinks() {
        discordLinksFile = new File(plugin.getDataFolder(), "discord-links.yml");
        if (!discordLinksFile.exists()) {
            try {
                discordLinksFile.createNewFile();
            } catch (IOException e) {
                logger.error("Failed to create discord-links.yml file");
                logger.error(e.getMessage());
                return;
            }
        }
        
        discordLinksConfig = YamlConfiguration.loadConfiguration(discordLinksFile);
        
        if (discordLinksConfig.contains("links")) {
            for (String uuidStr : discordLinksConfig.getConfigurationSection("links").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                String discordId = discordLinksConfig.getString("links." + uuidStr);
                linkedDiscordIds.put(uuid, discordId);
            }
        }
        
        logger.info("Loaded " + linkedDiscordIds.size() + " Discord account links");
    }
    
    private void saveDiscordLinks() {
        try {
            discordLinksConfig.save(discordLinksFile);
        } catch (IOException e) {
            logger.error("Failed to save discord-links.yml file");
            logger.error(e.getMessage());
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }

    public boolean initiateLinking(Player player, String discordId) {
        if (linkedDiscordIds.containsValue(discordId)) {
            player.sendMessage(CC.sendRed("This Discord account is already linked to another Minecraft account."));
            return false;
        }

        String verificationCode = generateVerificationCode();
        pendingVerifications.put(player.getUniqueId(), discordId);
        verificationCodes.put(verificationCode, player.getUniqueId());

        sendVerificationCodeToDiscord(discordId, verificationCode);

        return true;
    }


    private String generateVerificationCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
    }

    private void sendVerificationCodeToDiscord(String discordId, String verificationCode) {
        if (linkWebhook == null) {
            logger.error("Chat webhook is not initialized.");
            return;
        }

        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setColor(Color.BLUE.getRGB())
                .setTitle(new WebhookEmbed.EmbedTitle("Verification Code", null))
                .setDescription("Your verification code is: **" + verificationCode + "**")
                .setTimestamp(Instant.now());

        WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder()
                .setUsername("Verification Service")
                .addEmbeds(embedBuilder.build());

        CompletableFuture<ReadonlyMessage> messageFuture = linkWebhook.send(messageBuilder.build());

        messageFuture.thenAccept(message -> {
            long messageId = message.getId();
            verificationMessageIds.put(verificationCode, messageId);
            logger.info("Verification code sent with message ID: " + messageId);
        }).exceptionally(e -> {
            logger.error("Failed to send verification code to Discord");
            logger.error(e.getMessage());
            return null;
        });
    }


    public boolean confirmLinking(Player player, String verificationCode) {
        UUID playerUUID = verificationCodes.get(verificationCode);
        if (playerUUID == null || !playerUUID.equals(player.getUniqueId())) {
            player.sendMessage(CC.sendRed("Invalid verification code."));
            return false;
        }

        String discordId = pendingVerifications.remove(playerUUID);
        linkedDiscordIds.put(playerUUID, discordId);
        verificationCodes.remove(verificationCode);

        discordLinksConfig.set("links." + playerUUID.toString(), discordId);
        saveDiscordLinks();

        Long messageId = verificationMessageIds.remove(verificationCode);
        if (messageId != null) {
            linkWebhook.delete(messageId).thenRun(() ->
                    logger.info("Verification message ID: " + messageId + " deleted after confirmation.")
            ).exceptionally(e -> {
                logger.error("Failed to delete verification message ID: " + messageId);
                logger.error(e.getMessage());
                return null;
            });
        }

        player.sendMessage(CC.sendGreen("Your Discord account has been successfully linked."));
        return true;
    }

    
    public boolean unlinkDiscordAccount(Player player) {
        if (!enabled) return false;
        
        linkedDiscordIds.remove(player.getUniqueId());
        discordLinksConfig.set("links." + player.getUniqueId().toString(), null);
        saveDiscordLinks();
        return true;
    }
    
    public boolean isLinked(Player player) {
        return linkedDiscordIds.containsKey(player.getUniqueId());
    }
    
    public String getDiscordId(Player player) {
        return linkedDiscordIds.get(player.getUniqueId());
    }

    public void sendPunishmentNotification(Punishment punishment) {
        if (!enabled || punishmentWebhook == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                OfflinePlayer target = punishment.target();
                String issuerName = punishment.issuer() != null ? punishment.issuer().getName() : "Console";
                String targetName = target.getName() != null ? target.getName() : "Unknown";

                String skinUrl = "https://mc-heads.net/avatar/" + target.getUniqueId().toString() + "/100";
                String issuerSkinUrl = "https://mc-heads.net/avatar/Console/100";
                if (punishment.issuer() instanceof Player) {
                    Player issuerPlayer = (Player) punishment.issuer();
                    issuerSkinUrl = "https://mc-heads.net/avatar/" + issuerPlayer.getUniqueId().toString() + "/100";
                }

                WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
                        .setColor(getPunishmentColor(punishment.punishmentType()))
                        .setTitle(new WebhookEmbed.EmbedTitle(getPunishmentTitle(punishment.punishmentType()), null))
                        .setThumbnailUrl(skinUrl)
                        .setTimestamp(Instant.now())
                        .setFooter(new WebhookEmbed.EmbedFooter("Punishment ID: " + punishment.ID(), null));

                embed.addField(new WebhookEmbed.EmbedField(true, "Player", targetName));
                embed.addField(new WebhookEmbed.EmbedField(true, "Reason", punishment.reason()));
                embed.addField(new WebhookEmbed.EmbedField(true, "Staff", issuerName));
                embed.addField(new WebhookEmbed.EmbedField(true, "Duration", TimeUtil.formatRemainingTime(punishment.getRemainingTime())));
                embed.addField(new WebhookEmbed.EmbedField(false, "Evidence", punishmentEvidence.getOrDefault(punishment.ID(), "No evidence provided.")));

                WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder()
                        .setUsername(issuerName)
                        .setAvatarUrl(issuerSkinUrl)
                        .addEmbeds(embed.build());

                if (pingIssuer && punishment.issuer() instanceof Player) {
                    Player issuerPlayer = (Player) punishment.issuer();
                    String discordId = getDiscordId(issuerPlayer);
                    if (discordId != null && !discordId.isEmpty()) {
                        messageBuilder.setContent("<@" + discordId + ">");
                    }
                }

                CompletableFuture<ReadonlyMessage> messageFuture = punishmentWebhook.send(messageBuilder.build());

                messageFuture.thenAccept(message -> {
                    long messageId = message.getId();
                    logger.info("Punishment notification sent with message ID: " + messageId);

                    messageIds.put(punishment.ID(), messageId);

                    if (punishment.issuer() instanceof Player) {
                        if (plugin.getConfig().getBoolean("punishments.evidence-required")) {
                            Player issuerPlayer = (Player) punishment.issuer();
                            sendEvidenceRequestMessage(issuerPlayer, punishment.ID());
                        }
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to send punishment notification to Discord");
                logger.error(e.getMessage());
            }
        });
    }


    public void sendReportNotification(Report report) {
        if (!enabled || reportWebhook == null) return;
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                OfflinePlayer target = report.getSuspect();
                Player reporter = report.getIssuer();

                String skinUrl = "https://mc-heads.net/avatar/" + target.getUniqueId().toString() + "/100";

                WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
                        .setColor(0xFFAA00)
                        .setTitle(new WebhookEmbed.EmbedTitle("New Player Report", null))
                        .setThumbnailUrl(skinUrl)
                        .setTimestamp(Instant.now())
                        .setFooter(new WebhookEmbed.EmbedFooter("Report ID: " + report.getReportId(), null));

                embed.addField(new WebhookEmbed.EmbedField(true, "Reported Player", target.getName()));
                embed.addField(new WebhookEmbed.EmbedField(true, "Reporter", reporter.getName()));
                embed.addField(new WebhookEmbed.EmbedField(false, "Reason", report.getReason()));
                embed.addField(new WebhookEmbed.EmbedField(false, "Evidence", report.getEvidence()));

                WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder()
                        .setUsername("EasyStaff")
                        .addEmbeds(embed.build());
                
                reportWebhook.send(messageBuilder.build());
            } catch (Exception e) {
                logger.error("Failed to send report notification to Discord");
                logger.error(e.getMessage());
            }
        });
    }

    private String removeColorCodes(String text) {
        return text.replaceAll("§[0-9a-fA-Fk-orK-ORxX]", "");
    }

    public void sendReportHandledNotification(Report report, String handlerName, String reason, boolean accepted) {
        if (!enabled || reportWebhook == null) return;
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                OfflinePlayer target = report.getSuspect();
                Player reporter = report.getIssuer();

                String skinUrl = "https://mc-heads.net/avatar/" + target.getUniqueId().toString() + "/100";

                int embedColor = accepted ? 0x00FF00 : 0xFF0000;

                WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
                        .setColor(embedColor)
                        .setTitle(new WebhookEmbed.EmbedTitle("Report Handled", null))
                        .setThumbnailUrl(skinUrl)
                        .setTimestamp(Instant.now())
                        .setFooter(new WebhookEmbed.EmbedFooter("Report ID: " + report.getReportId(), null));


                embed.addField(new WebhookEmbed.EmbedField(true, "Reported Player", target.getName()));
                embed.addField(new WebhookEmbed.EmbedField(true, "Reporter", reporter.getName()));
                embed.addField(new WebhookEmbed.EmbedField(true, "Handled By", handlerName));
                embed.addField(new WebhookEmbed.EmbedField(false, "Original Reason", removeColorCodes(report.getReason())));
                embed.addField(new WebhookEmbed.EmbedField(false, "Resolution", removeColorCodes(reason)));
                
                WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder()
                        .setUsername("EasyStaff")
                        .addEmbeds(embed.build());
                
                reportWebhook.send(messageBuilder.build());
            } catch (Exception e) {
                logger.error("Failed to send report handled notification to Discord");
                logger.error(e.getMessage());
            }
        });
    }
    
    public void sendChatMessage(Player player, String channel, String message) {
        if (!enabled || chatWebhook == null) return;
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String skinUrl = "https://mc-heads.net/avatar/" + player.getUniqueId().toString() + "/100";
                
                WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder()
                        .setUsername(player.getName())
                        .setAvatarUrl(skinUrl)
                        .setContent(message);
                
                chatWebhook.send(messageBuilder.build());
            } catch (Exception e) {
                logger.error("Failed to send chat message to Discord");
                logger.error(e.getMessage());
            }
        });
    }

    private int getPunishmentColor(PunishmentType type) {
        switch (type) {
            case BAN:
                return 0xFF0000;
            case WARN:
                return 0xFFA500;
            case MUTE:
                return 0x0000FF;
            case KICK:
                return 0xFFFF00;
            default:
                return 0x000000;
        }
    }
    
    private String getPunishmentTitle(PunishmentType type) {
        switch (type) {
            case BAN:
                return "Player Banned";
            case MUTE:
                return "Player Muted";
            case WARN:
                return "Player Warned";
            case KICK:
                return "Player Kicked";
            default:
                return "Player Punished";
        }
    }
    
    private String formatDuration(long duration) {
        if (duration <= 0) {
            return "Permanent";
        }
        
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + " days";
        } else if (hours > 0) {
            return hours + " hours";
        } else if (minutes > 0) {
            return minutes + " minutes";
        } else {
            return seconds + " seconds";
        }
    }


    private boolean evidenceProvided(String punishmentId) {
        return punishmentEvidence.containsKey(punishmentId);
    }

    public boolean provideEvidence(Player player, String punishmentId, String evidenceLink) {
        if (!enabled || punishmentWebhook == null) return false;

        if (!messageIds.containsKey(punishmentId)) {
            player.sendMessage(CC.sendRed("Punishment not found or evidence already provided."));
            return false;
        }

        punishmentEvidence.put(punishmentId, evidenceLink);

        long messageId = messageIds.get(punishmentId);

        updatePunishmentMessageWithEvidence(messageId, punishmentId, evidenceLink);

        player.sendMessage(CC.sendGreen("Evidence provided successfully."));
        return true;
    }

    private void updatePunishmentMessageWithEvidence(long messageId, String punishmentId, String evidenceLink) {
        Punishment punishment = punishmentManager.getPunishmentById(punishmentId);
        if (punishment == null) {
            logger.error("Punishment not found for ID: " + punishmentId);
            return;
        }

        OfflinePlayer target = punishment.target();
        String issuerName = punishment.issuer() != null ? punishment.issuer().getName() : "Console";
        String targetName = target.getName() != null ? target.getName() : "Unknown";

        String skinUrl = "https://mc-heads.net/avatar/" + target.getUniqueId().toString() + "/100";

        WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
                .setColor(getPunishmentColor(punishment.punishmentType()))
                .setTitle(new WebhookEmbed.EmbedTitle(getPunishmentTitle(punishment.punishmentType()), null))
                .setThumbnailUrl(skinUrl)
                .setTimestamp(Instant.now())
                .setFooter(new WebhookEmbed.EmbedFooter("Punishment ID: " + punishment.ID(), null));

        embed.addField(new WebhookEmbed.EmbedField(true, "Player", targetName));
        embed.addField(new WebhookEmbed.EmbedField(true, "Reason", punishment.reason()));
        embed.addField(new WebhookEmbed.EmbedField(true, "Staff", issuerName));
        embed.addField(new WebhookEmbed.EmbedField(true, "Duration", TimeUtil.formatRemainingTime(punishment.getRemainingTime())));
        embed.addField(new WebhookEmbed.EmbedField(false, "Evidence", punishmentEvidence.getOrDefault(punishment.ID(), "No evidence provided.")));


        WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder()
                .setUsername(issuerName)
                .addEmbeds(embed.build());

        punishmentWebhook.edit(messageId, messageBuilder.build()).thenRun(() ->
                logger.info("Punishment message ID: " + messageId + " updated with new evidence.")
        ).exceptionally(e -> {
            logger.error("Failed to update punishment message ID: " + messageId);
            logger.error(e.getMessage());
            return null;
        });
    }
    

    private void sendEvidenceRequestMessage(Player player, String punishmentId) {
        net.kyori.adventure.text.Component message = net.kyori.adventure.text.Component.text()
                .content("Click here to provide evidence for punishment #" + punishmentId)
                .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand("/evidence " + punishmentId + " "))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        net.kyori.adventure.text.Component.text("Click to provide evidence link")
                ))
                .build();

        player.sendMessage("");
        player.sendMessage(CC.sendRed("&lEvidence required:"));
        player.sendMessage(message);
        player.sendMessage("");
    }
    
    public void shutdown() {
        if (punishmentWebhook != null) {
            punishmentWebhook.close();
        }
        if (reportWebhook != null) {
            reportWebhook.close();
        }
        if (chatWebhook != null) {
            chatWebhook.close();
        }
    }
}