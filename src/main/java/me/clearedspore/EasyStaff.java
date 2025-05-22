package me.clearedspore;

import co.aikar.commands.PaperCommandManager;
import me.clearedspore.command.*;
import me.clearedspore.command.freeze.FreezeCommand;
import me.clearedspore.command.punishment.*;
import me.clearedspore.command.report.ReportCommand;
import me.clearedspore.command.report.ReportListCommand;
import me.clearedspore.command.channel.ChannelCommand;
import me.clearedspore.command.channel.DynamicCommandRegister;
import me.clearedspore.feature.alertManager.Alert;
import me.clearedspore.feature.channels.DiscordChannelInfo;
import me.clearedspore.feature.notification.NotificationManager;
import me.clearedspore.feature.punishment.HiddenPunishmentManager;
import me.clearedspore.feature.setting.settings.StaffNotfySetting;
import me.clearedspore.feature.staffmode.StaffModeManager;
import me.clearedspore.feature.staffmode.SilentChestListener;
import me.clearedspore.easyAPI.EasyAPI;
import me.clearedspore.easyAPI.util.Logger;
import me.clearedspore.feature.alertManager.AlertManager;
import me.clearedspore.feature.alertManager.XRayDetector;
import me.clearedspore.feature.filter.FilterManager;
import me.clearedspore.feature.setting.SettingsManager;
import me.clearedspore.feature.setting.settings.MaintenanceLogsSetting;
import me.clearedspore.feature.setting.settings.VanishOnJoinSetting;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.feature.reports.ReportManager;
import me.clearedspore.feature.staffmode.VanishManager;
import me.clearedspore.feature.channels.ChannelManager;
import me.clearedspore.hook.PlaceholderAPI;
import me.clearedspore.hook.luckperms.StaffModeContext;
import me.clearedspore.hook.luckperms.VanishContext;
import me.clearedspore.manager.NoteManager;
import me.clearedspore.storage.PlayerData;
import me.clearedspore.util.ChatInputHandler;
import me.clearedspore.manager.MaintenanceManager;
import me.clearedspore.util.PS;
import me.clearedspore.util.ServerPingManager;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.nametag.NameTagManager;
import me.neznamy.tab.api.tablist.TabListFormatManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EasyStaff extends JavaPlugin {

    private static EasyStaff instance;

    private Map<String, List<String>> reasonsMap;

    private PaperCommandManager commandManager;
    private Logger logger;
    private EasyAPI easyAPI;
    private PlayerData playerData;
    private PunishmentManager punishmentManager;
    private ChatInputHandler chatInputHandler;
    private VanishManager vanishManager;
    private NameTagManager nameTagManager;
    private TabListFormatManager tablist;
    private SettingsManager settingsManager;
    private ReportManager reportManager;
    private MaintenanceManager maintenanceManager;
    private ServerPingManager serverPingManager;
    private NoteManager noteManager;
    private FilterManager filterManager;
    private ChannelManager channelManager;
    private StaffModeManager staffModeManager;
    private AlertManager alertManager;
    private XRayDetector xRayDetector;
    private LuckPerms luckperms;
    private me.clearedspore.feature.discord.DiscordManager discordManager;
    private DiscordChannelInfo discordChannelInfo;
    private NotificationManager notificationManager;
    private HiddenPunishmentManager hiddenPunishmentManager;

    @Override
    public void onEnable() {
        instance = this;
        if(!Bukkit.getPluginManager().isPluginEnabled("EasyAPI")){
            getLogger().severe("Failed to load plugin.");
            getLogger().severe("EasyAPI is not installed. This is a required dependency!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }


        reloadConfig();
        saveDefaultConfig();
        this.commandManager = new PaperCommandManager(this);
        this.easyAPI = EasyAPI.getInstance();
        this.logger = new Logger(getName());
        logger.info("Starting plugin");
        this.playerData = new PlayerData(this, logger);
        FileConfiguration reasonsConfig = loadReasonsConfig();
        FileConfiguration filterConfig = loadFilterConfig();
        this.alertManager = new AlertManager(playerData, this);

        this.playerData.setAlertManager(alertManager);

        this.punishmentManager = new PunishmentManager(playerData, reasonsConfig, this, alertManager);
        this.filterManager = new FilterManager(filterConfig, this, punishmentManager, logger, alertManager);
        this.discordManager = new me.clearedspore.feature.discord.DiscordManager(this, logger, playerData, punishmentManager);
        this.reportManager = new ReportManager(logger, this, alertManager);
        this.maintenanceManager = new MaintenanceManager(this, playerData);
        this.serverPingManager = new ServerPingManager(this);
        this.channelManager = new ChannelManager(logger, this, alertManager);
        this.xRayDetector = new XRayDetector(this, alertManager);
        this.hiddenPunishmentManager = new HiddenPunishmentManager(this);

        this.punishmentManager.setDiscordManager(discordManager);
        this.punishmentManager.setHiddenPunishmentManager(hiddenPunishmentManager);

        this.maintenanceManager.setServerPingManager(serverPingManager);
        this.noteManager = new NoteManager(playerData, this);

        this.chatInputHandler = new ChatInputHandler(this);

        this.notificationManager = new NotificationManager(this);


        if (Bukkit.getPluginManager().getPlugin("TAB") != null && getConfig().getBoolean("vanish.tab")) {
            this.nameTagManager = TabAPI.getInstance().getNameTagManager();
            this.tablist = TabAPI.getInstance().getTabListFormatManager();
            this.vanishManager = new VanishManager(nameTagManager, tablist, channelManager, this, playerData);

            this.staffModeManager = new StaffModeManager(this, logger, playerData, vanishManager);

            getServer().getPluginManager().registerEvents(new SilentChestListener(this, staffModeManager), this);
        } else {
            logger.error("TAB plugin is not installed");
            logger.error("It is required if you have it enabled in the config");
            logger.error("Shutting down");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckperms = provider.getProvider();
        }

        if (luckperms != null) {
            ContextManager contextManager = luckperms.getContextManager();
            contextManager.registerCalculator(new VanishContext(vanishManager));
        }

        logger.info("registering permissions");
        try {
            PS.registerPermissions();
            logger.info("successfully registered all permissions");
        } catch (Exception e){
            logger.error("Failed to register permissions");
            logger.error(e.getMessage());
        }


        registerPlaceholders();
        loadDefaultFiles();
        setupSettings();
        registerCompletions();

        commandManager.registerCommand(new BlockNameCommand(this, reasonsConfig, alertManager));
        getServer().getPluginManager().registerEvents(new BlockNameCommand(this, reasonsConfig, alertManager), this);
        registerCommands();
        registerListeners();
    }

    private void registerPlaceholders(){
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPI(this, punishmentManager, reportManager, staffModeManager, vanishManager, channelManager, logger).register();
        }
    }

    public ChatInputHandler getChatInputHandler() {
        return chatInputHandler;
    }

    private void registerCommands(){
        logger.info("Registering commands");
        try {
            commandManager.registerCommand(new BanCommand(punishmentManager));
            commandManager.registerCommand(new UnBanCommand(punishmentManager));
            commandManager.registerCommand(new TempBanCommand(punishmentManager));
            commandManager.registerCommand(new MuteCommand(punishmentManager));
            commandManager.registerCommand(new KickCommand(punishmentManager));
            commandManager.registerCommand(new TempMuteCommand(punishmentManager));
            commandManager.registerCommand(new UnMuteCommand(punishmentManager));
            commandManager.registerCommand(new HistoryCommand(this, punishmentManager, notificationManager));
            commandManager.registerCommand(new WarnCommand(punishmentManager));
            commandManager.registerCommand(new EasyStaffCommand(punishmentManager, filterManager, this));
            commandManager.registerCommand(new AltsCommand(playerData, punishmentManager));
            commandManager.registerCommand(new VanishCommand(vanishManager));
            commandManager.registerCommand(new StaffSettingsCommand(settingsManager, this));
            commandManager.registerCommand(new PunishCommand(punishmentManager, this));
            commandManager.registerCommand(new StaffHelp());
            commandManager.registerCommand(new WhoisCommand(punishmentManager, playerData));
            if(getConfig().getBoolean("advanced-tp")) {
                commandManager.registerCommand(new StaffTPCommand());
            }
            commandManager.registerCommand(new StaffTphereCommand());
            commandManager.registerCommand(new ReportListCommand(reportManager, this));
            commandManager.registerCommand(new ReportCommand(this, reportManager));
            commandManager.registerCommand(new MaintenanceCommand(maintenanceManager, this));
            commandManager.registerCommand(new FreezeCommand(playerData));
            commandManager.registerCommand(new EvidenceCommand(discordManager));
            commandManager.registerCommand(new NoteCommand(noteManager));
            commandManager.registerCommand(new NotifyCommand(notificationManager));
            if(filterManager.enabled()) {
                commandManager.registerCommand(new FilterCommand(filterManager));
            }

            commandManager.registerCommand(new ChannelCommand(channelManager));
            DynamicCommandRegister.registerDynamicCommands(commandManager, channelManager, logger);

            commandManager.registerCommand(new CPScheckCommand(this));
            commandManager.registerCommand(new StaffModeCommand(staffModeManager));
            commandManager.registerCommand(new AlertsCommand(alertManager));

            if (discordManager.isEnabled()) {
                commandManager.registerCommand(new me.clearedspore.command.discord.StaffLinkCommand(discordManager, chatInputHandler));
                commandManager.registerCommand(new me.clearedspore.command.discord.StaffUnlinkCommand(discordManager, this));
                commandManager.registerCommand(new me.clearedspore.command.discord.VerifyCommand(discordManager));
            }

            logger.info("Commands registered");
        } catch (Exception e){
            logger.error("Failed to load commands");
            logger.error(e.getMessage());
        }
    }

    private void registerCompletions(){
        commandManager.getCommandCompletions().registerAsyncCompletion("punishmentReasons",
                context -> punishmentManager.getPunishmentReasons());

        commandManager.getCommandCompletions().registerAsyncCompletion("reportReasons", context -> {
            return getConfig().getStringList("report.reasons");
        });

        commandManager.getCommandCompletions().registerAsyncCompletion("exemptPlayers", context -> {
            return getConfig().getStringList("punishments.exempt-players");
        });

        commandManager.getCommandCompletions().registerAsyncCompletion("maintenanceexemptPlayers", context -> {
            return getConfig().getStringList("maintenance.exempt");
       });

        commandManager.getCommandCompletions().registerAsyncCompletion("noteNumbers", context -> {
            Player player = context.getPlayer();
            return noteManager.getNoteNumbers(player);
        });
        
        commandManager.getCommandCompletions().registerAsyncCompletion("staffModes", context -> {
            Player player = context.getPlayer();
            return staffModeManager.getAvailableModesForPlayer(player);
        });

        commandManager.getCommandCompletions().registerAsyncCompletion("blockedNames", context -> {
            return getConfig().getStringList("blocked-names");
        });

        commandManager.getCommandCompletions().registerAsyncCompletion("alerts", context -> {
            List<String> allAlerts = new ArrayList<>();
            for (Alert alert : Alert.values()) {
                allAlerts.add(alert.toString());
            }
            return allAlerts;
        });
        
        commandManager.getCommandCompletions().registerAsyncCompletion("notifyPlayers", context -> {
            return notificationManager.getNotifyList();
        });
    }

    private void registerListeners(){
        logger.info("Registering listeners");
        try {
            getServer().getPluginManager().registerEvents(playerData, this);
            getServer().getPluginManager().registerEvents(punishmentManager, this);
            getServer().getPluginManager().registerEvents(maintenanceManager, this);
            getServer().getPluginManager().registerEvents(serverPingManager, this);
            getServer().getPluginManager().registerEvents(new FreezeCommand(playerData), this);
            getServer().getPluginManager().registerEvents(new CPScheckCommand(this), this);
            getServer().getPluginManager().registerEvents(staffModeManager, this);
            getServer().getPluginManager().registerEvents(alertManager, this);
            logger.info("Listeners registered");
        } catch (Exception e){
            logger.error("Failed to load listeners");
            logger.error(e.getMessage());
        }
    }

    private FileConfiguration loadReasonsConfig() {
        File reasonsFile = new File(getDataFolder(), "reasons.yml");
        if (!reasonsFile.exists()) {
            getDataFolder().mkdirs();
            try (InputStream in = getResource("reasons.yml")) {
                if (in != null) {
                    Files.copy(in, reasonsFile.toPath());
                    logger.info("reasons.yml file created successfully.");
                } else {
                    logger.error("Could not find reasons.yml in resources.");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            logger.info("reasons.yml file already exists.");
        }

        return YamlConfiguration.loadConfiguration(reasonsFile);
    }

    private FileConfiguration loadFilterConfig() {
        File filterFile = new File(getDataFolder(), "filter.yml");
        if (!filterFile.exists()) {
            getDataFolder().mkdirs();
            try (InputStream in = getResource("filter.yml")) {
                if (in != null) {
                    Files.copy(in, filterFile.toPath());
                    logger.info("filter.yml file created successfully.");
                } else {
                    logger.error("Could not find filter.yml in resources.");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            logger.info("filter.yml file already exists.");
        }

        return YamlConfiguration.loadConfiguration(filterFile);
    }

    private void loadDefaultFiles() {
        loadFile("modes/staffmode.yml");
        loadFile("modes/adminmode.yml");
    }

    private void loadFile(String filePath) {
        File file = new File(getDataFolder(), filePath);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try (InputStream in = getResource(filePath)) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                    logger.info(filePath + " file created successfully.");
                } else {
                    logger.error("Could not find " + filePath + " in resources.");
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to create " + filePath, e);
            }
        } else {
            logger.info(filePath + " file already exists.");
        }
    }

    public static EasyStaff getInstance() {
        return instance;
    }

    private void setupSettings() {
        logger.info("Setting up settings system");
        try {
            this.settingsManager = new SettingsManager(this, playerData);
            settingsManager.registerSetting(new VanishOnJoinSetting(playerData));
            settingsManager.registerSetting(new MaintenanceLogsSetting(playerData));
            settingsManager.registerSetting(new StaffNotfySetting(playerData, alertManager));
            logger.info("Settings system initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize settings system");
            logger.error(e.getMessage());
        }
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public PlayerData getPlayerData(){
        return playerData;
    }
    
    public ChannelManager getChannelManager() {
        return channelManager;
    }
    
    public StaffModeManager getStaffModeManager() {
        return staffModeManager;
    }
    
    public me.clearedspore.feature.discord.DiscordManager getDiscordManager() {
        return discordManager;
    }

    @Override
    public void onDisable() {
        logger.info("Disabling plugin");
        if (vanishManager != null) {
            vanishManager.stopActionbar();
            for (Player players : Bukkit.getOnlinePlayers()) {
                if (vanishManager.isVanished(players)) {
                    vanishManager.setVanished(players, false);
                }
            }
        }
        
        if (staffModeManager != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (staffModeManager.isInStaffMode(player)) {
                    staffModeManager.disableStaffMode(player);
                }
            }
        }
        
        playerData.saveAllPlayerData();

        if (discordManager != null) {
            discordManager.shutdown();
        }
    }
}
