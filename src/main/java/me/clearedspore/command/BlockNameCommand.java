package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.alertManager.Alert;
import me.clearedspore.feature.alertManager.AlertManager;
import me.clearedspore.util.P;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@CommandPermission(P.block_name)
@CommandAlias("blockname|removename|badname|banname")
public class BlockNameCommand extends BaseCommand implements Listener {

    private final JavaPlugin plugin;
    private final FileConfiguration reasonsConfig;
    private final AlertManager alertManager;

    public BlockNameCommand(JavaPlugin plugin, FileConfiguration reasonsConfig, AlertManager alertManager) {
        this.plugin = plugin;
        this.reasonsConfig = reasonsConfig;
        this.alertManager = alertManager;
    }

    @Default
    @CommandCompletion("@players")
    @Syntax("<name>")
    public void onBlockName(CommandSender player, String name) {
        FileConfiguration config = plugin.getConfig();
        List<String> blockedNameList = config.getStringList("blocked-names");

        if (blockedNameList.contains(name)) {
            player.sendMessage(CC.sendRed("That name is already blocked!"));
            return;
        }

        blockedNameList.add(name);
        config.set("blocked-names", blockedNameList);
        plugin.saveConfig();

        player.sendMessage(CC.sendBlue("You have blocked the name &f" + name));
        notifyPlayers(player, name, "blocked");
    for(Player players : Bukkit.getOnlinePlayers()) {
        if (blockedNameList.contains(players.getName())) {
            List<String> reason = reasonsConfig.getStringList("blocked-name");
            if (reason.isEmpty()) {
                reason.add("&cYou have a name that is blocked!");
                reason.add("");
                reason.add("&fYour minecraft name has been blocked from this minecraft server");
                reason.add("&fYou may join again once your name has been changed!");
                reason.add("");
                reason.add("&fCurrent Name: &e" + players.getName());
                reason.add("");
                reason.add("&fJoin our discord server if you believe this is a mistake");
                reason.add("&bDiscord.gg/");
            }
            for (String message : reason) {
                message.replace("%name%", players.getName());
                String formattedReason = String.join("\n", reason);
                players.kickPlayer(CC.translate(formattedReason));
            }
        }
    }
    }

    @Subcommand("remove")
    @CommandCompletion("@blockedNames")
    @CommandPermission(P.remove_blocked_name)
    @Syntax("<name>")
    public void onBlockedNameRemove(CommandSender player, String name) {
        FileConfiguration config = plugin.getConfig();
        List<String> blockedNameList = config.getStringList("blocked-names");

        if (!blockedNameList.contains(name)) {
            player.sendMessage(CC.sendRed("That name is not blocked!"));
            return;
        }

        blockedNameList.remove(name);
        config.set("blocked-names", blockedNameList);
        plugin.saveConfig();

        player.sendMessage(CC.sendBlue("You have un-blocked the name &f" + name));
        notifyPlayers(player, name, "un-blocked");
    }

    private void notifyPlayers(CommandSender player, String name, String action) {
        for (Player players : Bukkit.getOnlinePlayers()) {
            if (players.hasPermission(P.punish_notify) && alertManager.hasAlertEnabled(players, Alert.STAFF)) {
                players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEhas " + action + " the name &f" + name));
            }
        }
    }

    @Subcommand("list")
    @CommandPermission(P.blocked_name_list)
    public void onBlockedNameList(CommandSender player){
        FileConfiguration config = plugin.getConfig();
        List<String> blockedNameList = config.getStringList("blocked-names");
        player.sendMessage(CC.sendBlue("Blocked names:"));
        for(String blockedNames : blockedNameList){
            player.sendMessage(CC.send("&f- &#00CCDE" + blockedNames));
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = plugin.getConfig();
        List<String> blockedNameList = config.getStringList("blocked-names");

        if (blockedNameList.stream().map(String::toLowerCase).anyMatch(player.getName().toLowerCase()::equals)) {
            List<String> reason = reasonsConfig.getStringList("blocked-name");
            if (reason.isEmpty()) {
                reason.add("&cYou have a name that is blocked!");
                reason.add("");
                reason.add("&fYour minecraft name has been blocked from this minecraft server");
                reason.add("&fYou may join again once your name has been changed!");
                reason.add("");
                reason.add("&fCurrent Name: &e" + player.getName());
                reason.add("");
                reason.add("&fJoin our discord server if you believe this is a mistake");
                reason.add("&bDiscord.gg/");
            }
            for (String message : reason) {
                message.replace("%name%", player.getName());
                String formattedReason = String.join("\n", reason);
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, CC.translate(formattedReason));
            }
        }
    }
}
