package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.storage.PlayerData;
import me.clearedspore.util.P;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

@CommandPermission(P.alts)
@CommandAlias("alts")
public class AltsCommand extends BaseCommand {

    private final PlayerData playerData;
    private final PunishmentManager punishmentManager;

    public AltsCommand(PlayerData playerData, PunishmentManager punishmentManager) {
        this.playerData = playerData;
        this.punishmentManager = punishmentManager;
    }

    @Default
    @CommandCompletion("@players")
    @Syntax("<player>")
    private void onAlts(Player player, String targetName){

        if(targetName == null){
            player.sendMessage(CC.sendRed("That player is not online!"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if(!target.hasPlayedBefore()){
            player.sendMessage(CC.sendRed("That player has not played before!"));
            return;
        }

        String targetIP = playerData.getIP(target);
        boolean hasAlts = playerData.hasAlts(target);


        if(!hasAlts){
            player.sendMessage(CC.sendRed("That player does not have any alts"));
            return;
        }

        List<String> playerNamesWithSameIP = playerData.getPlayerNamesWithSameIP(targetIP);
        player.sendMessage(CC.sendBlue("Alts for " + targetName));
        player.sendMessage(CC.send("&a[Online] &7[Offline] &c[Banned] &9[Muted]"));

        StringBuilder altsList = new StringBuilder();

        for (String altName : playerNamesWithSameIP) {
            OfflinePlayer alt = Bukkit.getOfflinePlayer(altName);
            boolean isBanned = punishmentManager.isPlayerBanned(alt);
            boolean isMuted = punishmentManager.isPlayerMuted(alt);
            boolean online = alt.isOnline();

            String colorCode;
            if (isBanned) {
                colorCode = "&c";
            } else if (isMuted) {
                colorCode = "&9";
            } else if (online) {
                colorCode = "&a";
            } else {
                colorCode = "&7";
            }

            altsList.append(colorCode).append(altName).append("&r, ");
        }

        if (altsList.length() > 0) {
            altsList.setLength(altsList.length() - 2);
        }

        player.sendMessage(CC.send(altsList.toString()));
    }
}
