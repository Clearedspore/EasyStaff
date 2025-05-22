package me.clearedspore.command.punishment;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.feature.punishment.menu.punishplayer.PunishPlayerMenu;
import me.clearedspore.util.PS;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@CommandAlias("punish|p")
@CommandPermission(PS.punish)
public class PunishCommand extends BaseCommand {

    private final PunishmentManager punishmentManager;
    private final JavaPlugin plugin;

    public PunishCommand(PunishmentManager punishmentManager, JavaPlugin plugin) {
        this.punishmentManager = punishmentManager;
        this.plugin = plugin;
    }

    @Default
    @CommandCompletion("@players @punishmentReasons")
    @Syntax("<player> <reason> [-s] [-hs]")
    public void onPunish(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(CC.send("&cPlease specify a player."));
            return;
        }

        String targetName = args[0];
        OfflinePlayer target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            target = Bukkit.getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore()) {
                sender.sendMessage(CC.send("&cPlayer not found: " + targetName));
                return;
            }
        }

        if (args.length == 1) {
            if(sender instanceof  Player player) {
                new PunishPlayerMenu(plugin, punishmentManager, target).open(player);
                return;
            } else {
                sender.sendMessage(CC.sendRed("You must be a player to open the punish menu"));
            }
        }

        StringBuilder reasonBuilder = new StringBuilder();
        boolean silent = false;
        boolean hideStaffMessage = false;

        for (int i = 1; i < args.length; i++) {
            String part = args[i];
            if ("-s".equalsIgnoreCase(part)) {
                if (sender.hasPermission(PS.silent)) {
                    silent = true;
                } else {
                    sender.sendMessage(CC.send("&cYou don't have permission to use the silent flag."));
                    return;
                }
            } else if ("-hs".equalsIgnoreCase(part)) {
                if (sender.hasPermission(PS.high_silent)) {
                    hideStaffMessage = true;
                } else {
                    sender.sendMessage(CC.send("&cYou don't have permission to use the high silent flag."));
                    return;
                }
            } else {
                if (reasonBuilder.length() > 0) {
                    reasonBuilder.append(" ");
                }
                reasonBuilder.append(part);
            }
        }

        String reason = reasonBuilder.toString();

        if (!sender.hasPermission(PS.punish_ban) &&
                !sender.hasPermission(PS.punish_tempban) &&
                !sender.hasPermission(PS.punish_mute) &&
                !sender.hasPermission(PS.punish_tempmute) &&
                !sender.hasPermission(PS.punish_warn) &&
                !sender.hasPermission(PS.punish_kick)) {
            sender.sendMessage(CC.send("&cYou don't have permission to use this command."));
            return;
        }

        List<String> validReasons = punishmentManager.getPunishmentReasons();
        if (!validReasons.contains(reason)) {
            sender.sendMessage(CC.send("&cInvalid reason. Valid reasons: " + String.join(", ", validReasons)));
            return;
        }

        punishmentManager.punishPlayer(sender, target, reason, silent, hideStaffMessage);
    }
}