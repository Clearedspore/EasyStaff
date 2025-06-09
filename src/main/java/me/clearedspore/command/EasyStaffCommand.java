package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.filter.FilterManager;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.feature.staffmode.StaffModeManager;
import me.clearedspore.storage.PlayerData;
import me.clearedspore.util.P;
import me.clearedspore.util.update.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

@CommandAlias("easystaff")
@CommandPermission(P.easystaff)
public class EasyStaffCommand extends BaseCommand {

    private final PunishmentManager punishmentManager;
    private final StaffModeManager staffModeManager;
    private final FilterManager filterManager;
    private final JavaPlugin plugin;
    private final UpdateChecker updateChecker;
    private final PlayerData playerData;

    public EasyStaffCommand(PunishmentManager punishmentManager, StaffModeManager staffModeManager, FilterManager filterManager, JavaPlugin plugin, UpdateChecker updateChecker, PlayerData playerData) {
        this.punishmentManager = punishmentManager;
        this.staffModeManager = staffModeManager;
        this.filterManager = filterManager;
        this.plugin = plugin;
        this.updateChecker = updateChecker;
        this.playerData = playerData;
    }


    @Subcommand("reload")
    @CommandPermission(P.reload)
    private void onReload(CommandSender sender) {
        plugin.reloadConfig();

        File reasonsFile = new File(plugin.getDataFolder(), "reasons.yml");
        File filterFile = new File(plugin.getDataFolder(), "filter.yml");

        YamlConfiguration reasonsConfig = YamlConfiguration.loadConfiguration(reasonsFile);
        punishmentManager.setReasonsConfig(reasonsConfig);

        YamlConfiguration filterConfig = YamlConfiguration.loadConfiguration(filterFile);
        filterManager.setFilterConfig(filterConfig);

        staffModeManager.loadStaffModes();

        sender.sendMessage(CC.sendBlue("Configuration files have been reloaded."));
    }

    @Subcommand("exempt")
    @CommandPermission(P.exempt)
    private void onExempt(CommandSender player){
        player.sendMessage(CC.sendRed("Usage:"));
        player.sendMessage(CC.sendBlue("/exempt add (player)"));
        player.sendMessage(CC.sendBlue("/exempt remove (player)"));
        player.sendMessage(CC.sendBlue("/exempt list (player)"));
    }

    @Subcommand("exempt add")
    @CommandPermission(P.exempt_add)
    @CommandCompletion("@players")
    @Syntax("<player>")
    private void onExemptAdd(CommandSender player, String targetName){

        if(punishmentManager.isExempt(targetName)){
            player.sendMessage(CC.sendRed("That player is already exempt!"));
            return;
        }

        punishmentManager.addExemptPlayer(targetName);
        player.sendMessage(CC.sendBlue("You have added &f" + targetName + " &#00CCDEas an exempt"));
    }

    @Subcommand("exempt remove")
    @CommandPermission(P.exempt_remove)
    @CommandCompletion("@exemptPlayers")
    @Syntax("<player>")
    private void onExemptRemove(CommandSender player, String targetName){

        if(!punishmentManager.isExempt(targetName)){
            player.sendMessage(CC.sendRed("That player not on the exempt list!"));
            return;
        }

        punishmentManager.removeExemptPlayer(targetName);
        player.sendMessage(CC.sendBlue("You have remove &f" + targetName + " &#00CCDEfrom the exempt list"));
    }

    @Subcommand("exempt list")
    @CommandPermission(P.exempt)
    private void onExemptList(CommandSender player){
        player.sendMessage(CC.sendBlue("Exempt list:"));
        for(String exemptPlayers : punishmentManager.getExemptPlayers()){
            player.sendMessage(CC.sendWhite("- &#00CCDE" + exemptPlayers));
        }
    }

    @Subcommand("clearhistory")
    @CommandPermission(P.clear_history)
    @CommandCompletion("@players")
    @Syntax("<player>")
    private void onClearHistory(CommandSender player, String targetName) {

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        punishmentManager.clearPunishments(player, target);
        player.sendMessage(CC.sendBlue("You have cleared " + targetName + "'s history"));
    }

    @Subcommand("version")
    @CommandPermission(P.version)
    public void onVersion(CommandSender sender) {
        sender.sendMessage(CC.sendBlue("EasyStaff version: &f" + plugin.getDescription().getVersion()));
    }

    @Subcommand("playerdata generatefile")
    @CommandCompletion("@players")
    @CommandPermission(P.playerdata_generate)
    @Syntax("<player>")
    public void onGenerateFile(CommandSender sender, @Single String targetName) {
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            sender.sendMessage(CC.sendRed("Target player must be online!"));
            return;
        }

        if (playerData.hasPlayerData(target.getUniqueId())) {
            sender.sendMessage(CC.sendRed("Player already has a data file!"));
            return;
        }

        playerData.initializePlayerData(target);
        sender.sendMessage(CC.sendBlue("Successfully generated data file for &f" + target.getName()));
    }

    @Subcommand("playerdata reset")
    @CommandCompletion("@players")
    @CommandPermission(P.playerdata_reset)
    @Syntax("<player>")
    public void onReset(CommandSender sender, @Single OfflinePlayer target) {
        String targetName = target.getName();

        if (!playerData.hasPlayerData(target.getUniqueId())) {
            sender.sendMessage(CC.sendRed("&cPlayer does not have a data file!"));
            return;
        }

        boolean success = playerData.resetPlayerData(target);
        if (success) {
            sender.sendMessage(CC.sendBlue("Reset &f" + targetName + "&#00CCDE's data file to default values"));
        } else {
            sender.sendMessage(CC.sendRed("Failed to reset data file for &f" + targetName));
        }
    }


    @Subcommand("update check")
    @CommandPermission(P.update_notify)
    public void onDefault(CommandSender sender) {
        sender.sendMessage(CC.sendBlue("[EasyStaff] &fChecking for updates..."));

        updateChecker.checkForUpdates();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (updateChecker.isUpdateAvailable()) {
                sender.sendMessage(CC.sendBlue("[EasyStaff] &fA new update is available!"));
                sender.sendMessage(CC.sendBlue("[EasyStaff] &fCurrent version: &e" + plugin.getDescription().getVersion()));
                sender.sendMessage(CC.sendBlue("[EasyStaff] &fLatest version: &e" + updateChecker.getLatestVersion()));
                sender.sendMessage(CC.sendBlue("[EasyStaff] &fDownload at: &ehttps://www.spigotmc.org/resources/easystaff.125621/"));
            } else {
                sender.sendMessage(CC.sendBlue("[EasyStaff] &fYou are running the latest version of EasyStaff."));
            }
        }, 60L);
    }
}
