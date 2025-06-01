package me.clearedspore.command.punishment;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.util.P;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

@CommandPermission(P.mute)
@CommandAlias("mute")
public class MuteCommand extends BaseCommand {

    private final PunishmentManager punishmentManager;

    public MuteCommand(PunishmentManager punishmentManager) {
        this.punishmentManager = punishmentManager;
    }

    @Default
    @CommandCompletion("@players")
    @Syntax("<player> <reason> [-s] [-hs]")
    private void onMutePlayer(CommandSender player, String targetName, String... reasonParts) {
        if (targetName == null) {
            player.sendMessage(CC.sendRed("You must provide a player!"));
            return;
        }

        if (reasonParts == null || reasonParts.length == 0) {
            player.sendMessage(CC.sendRed("You must provide a reason!"));
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

        String reason = reasonBuilder.toString();

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore()) {
            player.sendMessage(CC.sendRed("Player not found!"));
            return;
        }

        if (punishmentManager.isExempt(targetName)) {
            player.sendMessage(CC.sendRed("You cannot punish that player!"));
            return;
        }

        if (punishmentManager.isPlayerMuted(target)) {
            player.sendMessage(CC.sendRed("That player is already muted!"));
            return;
        }

        punishmentManager.mutePlayer(player, target, reason, silent, hideStaffMessage);
        player.sendMessage(CC.sendBlue("You have muted &f" + target.getName() + " &#00CCDEfor &6(" + reason + ")"));
    }
}
