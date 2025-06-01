package me.clearedspore.command.punishment;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.util.P;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

@CommandAlias("unban")
@CommandPermission(P.unban)
public class UnBanCommand extends BaseCommand {

    private final PunishmentManager punishmentManager;
    private final JavaPlugin plugin;

    public UnBanCommand(PunishmentManager punishmentManager, JavaPlugin plugin) {
        this.punishmentManager = punishmentManager;
        this.plugin = plugin;
    }


    @Default
    @Description("Unban a player")
    @Syntax("<player> [-s] [-hs] <reason>")
    @CommandCompletion("@players")
    public void onUnban(CommandSender player, String targetName, @Optional String... reasonParts) {
        if (targetName == null) {
            player.sendMessage(CC.sendRed("You must provide a player!"));
            return;
        }


        boolean silent = false;
        boolean hideStaffMessage = false;
        StringBuilder reasonBuilder = new StringBuilder();

        for (String part : reasonParts) {
            if ("-s".equalsIgnoreCase(part)) {
                if (player.hasPermission(P.silent)) {
                    silent = true;
                } else {
                    player.sendMessage(CC.send("&cYou don't have permission to use the silent flag."));
                    return;
                }
            } else if ("-hs".equalsIgnoreCase(part)) {
                if (player.hasPermission(P.high_silent)) {
                    hideStaffMessage = true;
                } else {
                    player.sendMessage(CC.send("&cYou don't have permission to use the high silent flag."));
                    return;
                }
            } else {
                if (reasonBuilder.length() > 0) {
                    reasonBuilder.append(" ");
                }
                reasonBuilder.append(part);
            }
        }

        String reason = reasonBuilder.length() > 0 ? reasonBuilder.toString() : "No reason specified";

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!plugin.getConfig().getBoolean("punishments.remove-own") && target.equals(player) && !player.hasPermission(P.removeown_bypass)) {
            player.sendMessage(CC.sendRed("You cannot remove your own punishment!"));
            return;
        }

        boolean unbanned = punishmentManager.unbanPlayer(player, target, reason, silent, hideStaffMessage);

        if (unbanned && !silent) {
            player.sendMessage(CC.sendBlue("You have unbanned &f" + target.getName() + ""));
        } else if (!unbanned) {
            player.sendMessage(CC.sendRed("That player is not banned!"));
        }
    }
}