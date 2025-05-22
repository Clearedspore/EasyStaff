package me.clearedspore.hook;

import me.clearedspore.easyAPI.util.Logger;
import me.clearedspore.feature.channels.Channel;
import me.clearedspore.feature.channels.ChannelManager;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.feature.reports.ReportManager;
import me.clearedspore.feature.staffmode.StaffModeManager;
import me.clearedspore.feature.staffmode.VanishManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderAPI extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private final PunishmentManager punishmentManager;
    private final ReportManager reportManager;
    private final StaffModeManager staffModeManager;
    private final VanishManager vanishManager;
    private final ChannelManager channelManager;
    private final Logger logger;

    public PlaceholderAPI(JavaPlugin plugin, PunishmentManager punishmentManager, ReportManager reportManager, StaffModeManager staffModeManager, VanishManager vanishManager, ChannelManager channelManager, Logger logger) {
        this.plugin = plugin;
        this.punishmentManager = punishmentManager;
        this.reportManager = reportManager;
        this.staffModeManager = staffModeManager;
        this.vanishManager = vanishManager;
        this.channelManager = channelManager;
        this.logger = logger;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "ClearedSpore";
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "easystaff";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if(player == null){
            return "";
        }

        if (params.equalsIgnoreCase("punishments.bans")) {
            return String.valueOf(punishmentManager.getAllBans(player).size());
        }

        if (params.equalsIgnoreCase("punishments.warns")) {
            return String.valueOf(punishmentManager.getAllWarns(player).size());
        }

        if (params.equalsIgnoreCase("punishments.kicks")) {
            return String.valueOf(punishmentManager.getAllKicks(player).size());
        }

        if (params.equalsIgnoreCase("punishments.mutes")) {
            return String.valueOf(punishmentManager.getAllMutes(player).size());
        }

        if (params.equalsIgnoreCase("punishments.active_bans")) {
            return String.valueOf(punishmentManager.getActiveBans(player).size());
        }

        if (params.equalsIgnoreCase("punishments.active_mutes")) {
            return String.valueOf(punishmentManager.getActiveMutes(player).size());
        }

        if (params.equalsIgnoreCase("punishments.active_warns")) {
            return String.valueOf(punishmentManager.getActiveBans(player).size());
        }

        if (params.equalsIgnoreCase("reports")) {
            return String.valueOf(reportManager.getAllReports().size());
        }

        if (params.equalsIgnoreCase("vanished")) {
            return String.valueOf(vanishManager.isVanished(player));
        }

        String channelID = channelManager.getPlayerChannel(player);
        Channel channel = channelManager.getChannel(channelID);
        String channelName = (channel != null) ? channel.getName() : "&aglobal";

        if (params.equalsIgnoreCase("currentchannel")) {
            return channelName;
        }

        if (params.equalsIgnoreCase("staffmode_enabled")) {
            return String.valueOf(staffModeManager.isInStaffMode(player));
        }


        return null;
    }
}