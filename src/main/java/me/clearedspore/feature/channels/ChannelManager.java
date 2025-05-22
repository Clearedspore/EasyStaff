package me.clearedspore.feature.channels;

import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.easyAPI.util.Logger;
import me.clearedspore.feature.alertManager.Alert;
import me.clearedspore.feature.alertManager.AlertManager;
import me.clearedspore.feature.discord.DiscordManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;


public class ChannelManager implements Listener {
    private final Map<String, Channel> channels = new HashMap<>();
    private final Map<UUID, String> playerChannels = new HashMap<>();
    private final Map<String, List<String>> commandToChannelMap = new HashMap<>();
    private final Logger logger;
    private final JavaPlugin plugin;
    private final File channelsFile;
    private final FileConfiguration channelsConfig;
    private static final String GLOBAL_CHANNEL = "global";
    private DiscordManager discordManager;
    private final AlertManager alertManager;


    public ChannelManager(Logger logger, JavaPlugin plugin, AlertManager alertManager) {
        this.logger = logger;
        this.plugin = plugin;
        this.channelsFile = new File(plugin.getDataFolder(), "channels.yml");
        this.alertManager = alertManager;

        if (!channelsFile.exists()) {
            createChannelsFile();
        }

        this.channelsConfig = YamlConfiguration.loadConfiguration(channelsFile);
        logger.info("Loading channels from: " + channelsFile.getAbsolutePath());

        loadChannels();

        if (plugin instanceof me.clearedspore.EasyStaff) {
            this.discordManager = ((me.clearedspore.EasyStaff) plugin).getDiscordManager();
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    private void createChannelsFile() {
        try {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("channels.yml")) {
                if (in != null) {
                    Files.copy(in, channelsFile.toPath());
                    logger.info("channels.yml file created successfully.");
                } else {
                    logger.error("Could not find channels.yml in resources.");
                }
            }
        } catch (IOException e) {
            logger.error("An error occurred while creating the channels.yml file.");
            e.printStackTrace();
        }
    }

    public void loadChannels() {
        channels.clear();
        commandToChannelMap.clear();

        FileConfiguration config = YamlConfiguration.loadConfiguration(channelsFile);


        for (String channelId : config.getKeys(false)) {
            ConfigurationSection channelSection = config.getConfigurationSection(channelId);

            if (channelSection != null) {
                List<String> commands = channelSection.getStringList("commands");
                String name = channelSection.getString("name", channelId);
                String prefix = channelSection.getString("prefix", "");
                String permission = channelSection.getString("permission", "");
                String discordId = channelSection.getString("discord-id", null);


                Channel channel = new Channel(channelId, commands, discordId, name, prefix, permission);
                channels.put(channelId, channel);


                for (String command : commands) {
                    commandToChannelMap.put(command.toLowerCase(), Arrays.asList(channelId, command));
                }

                logger.info("Loaded channel: " + channelId + " with commands: " + commands + (discordId != null ? " and Discord ID: " + discordId : ""));
            }
        }
    }
    

    public void reloadChannels() {
        loadChannels();
    }


    public Channel getChannel(String channelId) {
        return channels.get(channelId);
    }

    public Collection<Channel> getAllChannels() {
        return channels.values();
    }


    public String getPlayerChannel(Player player) {
        return playerChannels.getOrDefault(player.getUniqueId(), GLOBAL_CHANNEL);
    }


    public boolean setPlayerChannel(Player player, String channelId) {
        if (channelId.equals(GLOBAL_CHANNEL)) {
            playerChannels.remove(player.getUniqueId());
            return true;
        }
        
        Channel channel = getChannel(channelId);
        if (channel == null) {
            return false;
        }
        
        if (!player.hasPermission(channel.getPermission())) {
            return false;
        }
        
        playerChannels.put(player.getUniqueId(), channelId);
        return true;
    }


    public boolean togglePlayerChannel(Player player, String channelId) {
        String currentChannel = getPlayerChannel(player);
        
        if (currentChannel.equals(channelId)) {
            playerChannels.remove(player.getUniqueId());
            return true;
        } else {
            return setPlayerChannel(player, channelId);
        }
    }


    public boolean sendChannelMessage(Player sender, String channelId, String message) {
        Channel channel = getChannel(channelId);
        if (channel == null) {
            return false;
        }

        if (!sender.hasPermission(channel.getPermission())) {
            sender.sendMessage(CC.sendRed("Since you don't have permission to send messages in this channel you will put in the global chat."));
            setPlayerChannel(sender, GLOBAL_CHANNEL);
            return false;
        }

        String formattedMessage = CC.translate(channel.getPrefix() + sender.getName() + " -> " + message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(channel.getPermission()) && alertManager.hasAlertEnabled(player, Alert.STAFF)) {
                player.sendMessage(formattedMessage);
            }
        }

        String dcMessage = removeColorCodes(message);

        String discordId = channel.getDiscordId();
        if (discordId != null && !discordId.isEmpty()) {
            discordManager.sendChatMessage(sender, discordId, dcMessage);
        }

        return true;
    }

    private String removeColorCodes(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "");
    }

    public Map<String, List<String>> getCommandToChannelMap() {
        return commandToChannelMap;
    }

    public String getChannelPermission(String channelId) {
        Channel channel = getChannel(channelId);
        return (channel != null) ? channel.getPermission() : null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String channelId = getPlayerChannel(player);

        if (!channelId.equals(GLOBAL_CHANNEL)) {
            event.setCancelled(true);
            sendChannelMessage(player, channelId, event.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerChannels.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerChannels.remove(event.getPlayer().getUniqueId());
    }
}