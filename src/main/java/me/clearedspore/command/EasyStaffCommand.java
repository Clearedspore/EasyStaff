package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.filter.FilterManager;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.util.PS;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

@CommandAlias("easystaff")
@CommandPermission(PS.easystaff)
public class EasyStaffCommand extends BaseCommand {

    private final PunishmentManager punishmentManager;
    private final FilterManager filterManager;
    private final JavaPlugin plugin;

    public EasyStaffCommand(PunishmentManager punishmentManager, FilterManager filterManager, JavaPlugin plugin) {
        this.punishmentManager = punishmentManager;
        this.filterManager = filterManager;
        this.plugin = plugin;
    }


    @Subcommand("reload")
    @CommandPermission(PS.reload)
    private void onReload(CommandSender sender) {
        plugin.reloadConfig();

        File reasonsFile = new File(plugin.getDataFolder(), "reasons.yml");
        File filterFile = new File(plugin.getDataFolder(), "filter.yml");

        YamlConfiguration reasonsConfig = YamlConfiguration.loadConfiguration(reasonsFile);
        punishmentManager.setReasonsConfig(reasonsConfig);

        YamlConfiguration filterConfig = YamlConfiguration.loadConfiguration(filterFile);
        filterManager.setFilterConfig(filterConfig);

        sender.sendMessage(CC.sendBlue("Configuration files have been reloaded."));
    }

    @Subcommand("exempt")
    @CommandPermission(PS.exempt)
    private void onExempt(CommandSender player){
        player.sendMessage(CC.sendRed("Usage:"));
        player.sendMessage(CC.sendBlue("/exempt add (player)"));
        player.sendMessage(CC.sendBlue("/exempt remove (player)"));
        player.sendMessage(CC.sendBlue("/exempt list (player)"));
    }

    @Subcommand("exempt add")
    @CommandPermission(PS.exempt_add)
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
    @CommandPermission(PS.exempt_remove)
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
    @CommandPermission(PS.exempt)
    private void onExemptList(CommandSender player){
        player.sendMessage(CC.sendBlue("Exempt list:"));
        for(String exemptPlayers : punishmentManager.getExemptPlayers()){
            player.sendMessage(CC.sendWhite("- &#00CCDE" + exemptPlayers));
        }
    }

    @Subcommand("clearhistory")
    @CommandPermission(PS.clear_history)
    @CommandCompletion("@players")
    @Syntax("<player>")
    private void onClearHistory(CommandSender player, String targetName) {

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        punishmentManager.clearPunishments(player, target);
        player.sendMessage(CC.sendBlue("You have cleared " + targetName + "'s history"));
    }
}
