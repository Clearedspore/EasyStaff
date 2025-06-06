package me.clearedspore.command.punishment;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.util.P;
import me.clearedspore.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

@CommandPermission(P.tempmute)
@CommandAlias("tempmute")
public class TempMuteCommand extends BaseCommand {

    private final PunishmentManager punishmentManager;

    public TempMuteCommand(PunishmentManager punishmentManager) {
        this.punishmentManager = punishmentManager;
    }

    @Default
    @CommandCompletion("@players")
    @Syntax("<player> <duration> <reason> [-s] [-hs]")
    private void onTempMutePlayer(CommandSender player, String targetName, String duration, String... reasonParts) {
        if (targetName == null) {
            player.sendMessage(CC.sendRed("You must provide a player!"));
            return;
        }

        if (reasonParts == null || reasonParts.length == 0) {
            player.sendMessage(CC.sendRed("You must provide a reason!"));
            return;
        }

        if (duration == null) {
            player.sendMessage(CC.sendRed("You must provide a duration! (e.g. 1d, 2h, 30m, 45s)"));
            return;
        }

        long durationMillis = TimeUtil.parseTime(duration);
        if (durationMillis <= 0) {
            player.sendMessage(CC.sendRed("Invalid duration format! Use s, m, h, d, M(month), y (e.g. 1 year, 1 month, 1d, 2h, 30m, 45s)"));
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

        long expirationTime = System.currentTimeMillis() + durationMillis;
        punishmentManager.tempMutePlayer(player, target, reason, expirationTime, silent, hideStaffMessage);

        String formattedDuration = TimeUtil.formatDuration(durationMillis);
        player.sendMessage(CC.sendBlue("You have temporarily muted &f" + target.getName() +
                " &#00CCDEfor &6" + formattedDuration + " &#00CCDEwith reason: &6(" + reason + ")"));
    }
}