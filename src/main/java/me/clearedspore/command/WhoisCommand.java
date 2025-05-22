package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.storage.PlayerData;
import me.clearedspore.util.PS;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.List;

@CommandPermission(PS.whois)
@CommandAlias("whois|checkplayer")
public class WhoisCommand extends BaseCommand {

    private final PunishmentManager punishmentManager;
    private final PlayerData playerData;

    public WhoisCommand(PunishmentManager punishmentManager, PlayerData playerData) {
        this.punishmentManager = punishmentManager;
        this.playerData = playerData;
    }


    @Default
    @CommandCompletion("@players")
    @Syntax("<Player>")
    public void onWhois(Player player, String targetName, @Optional String arg2) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore()) {
            player.sendMessage(CC.sendRed("That player has never joined before!"));
            return;
        }

        boolean banned = punishmentManager.isPlayerBanned(target);
        boolean muted = punishmentManager.isPlayerMuted(target);
        boolean online = target.isOnline();
        String status;
        if (banned) {
            status = CC.sendRed("&lBanned");
        } else if (muted) {
            status = "&9&lMuted";
        } else if (online) {
            status = "&aOnline";
        } else {
            status = "&7Offline";
        }

        long playTimeTicks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long playTimeSeconds = playTimeTicks / 20;

        long days = playTimeSeconds / 86400;
        long hours = (playTimeSeconds % 86400) / 3600;
        long seconds = playTimeSeconds % 60;

        String playTimeFormatted = String.format("%d days, %02d hours, %02d seconds", days, hours, seconds);

        long sessions = target.getStatistic(Statistic.LEAVE_GAME);

        long lastPlayed = target.getLastPlayed();
        long currentTime = System.currentTimeMillis();
        long timeSinceLastJoin = currentTime - lastPlayed;

        long daysAgo = timeSinceLastJoin / (1000 * 60 * 60 * 24);
        long hoursSinceLastJoin = (timeSinceLastJoin / (1000 * 60 * 60)) % 24;
        long minutesSinceLastJoin = (timeSinceLastJoin / (1000 * 60)) % 60;

        String lastJoinFormatted = String.format("%d days, %02d hours, %02d minutes ago", daysAgo, hoursSinceLastJoin, minutesSinceLastJoin);

        String targetIP = playerData.getIP(target);
        boolean hasAlts = playerData.hasAlts(target);
        List<String> playerNamesWithSameIP = playerData.getPlayerNamesWithSameIP(targetIP);

        if (arg2 == null) {

            player.sendMessage(CC.sendBlue("Info for &f" + targetName));
            player.sendMessage(CC.send("&f----------------------------------------"));
            player.sendMessage(CC.sendBlue("UUID: &f" + target.getUniqueId()));
            player.sendMessage(CC.sendBlue("Status: " + status));
            player.sendMessage(CC.sendBlue(""));
            player.sendMessage(CC.sendBlue("Playtime: &f" + playTimeFormatted));
            player.sendMessage(CC.sendBlue("Sessions: &f" + sessions));
            player.sendMessage(CC.sendBlue("Last joined: &f" + lastJoinFormatted));
            player.sendMessage(CC.sendBlue(""));
            TextComponent alts = new TextComponent(" §c[Alts]");
            alts.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/alts " + targetName));
            alts.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to check " + targetName + "'s alts")));
            TextComponent history = new TextComponent(" §a[History]");
            history.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/history " + targetName));
            history.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to check " + targetName + "'s history")));
            TextComponent nextPage = new TextComponent(CC.sendBlue("[Next Page]"));
            nextPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/whois " + targetName + " 2"));
            nextPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to go to the next page")));
            if(player.hasPermission(PS.alts)) {
                nextPage.addExtra(alts);
            }
            if(player.hasPermission(PS.history_others)){
                nextPage.addExtra(history);
            }
            player.spigot().sendMessage(nextPage);
            player.sendMessage(CC.send("&f----------------------------------------"));


        } else if (arg2.equals("2")){


            player.sendMessage(CC.sendBlue("Info for &f" + targetName + " &7(Page 2)"));
            player.sendMessage(CC.send("&f----------------------------------------"));
            player.sendMessage(CC.sendBlue("Total punishments: &f" + punishmentManager.getAllPunishments(target).size()));
            player.sendMessage(CC.sendBlue("Bans: &f" + punishmentManager.getAllBans(target).size()));
            player.sendMessage(CC.sendBlue("Mutes: &f" + punishmentManager.getAllMutes(target).size()));
            player.sendMessage(CC.sendBlue("Warns: &f" + punishmentManager.getAllWarns(target).size()));
            player.sendMessage(CC.sendBlue("Kicks: &f" + punishmentManager.getAllKicks(target).size()));
            if(player.hasPermission(PS.alts)) {
                player.sendMessage(CC.sendBlue(""));
                player.sendMessage(CC.sendBlue("Alts: &a[Online] &7[Offline] &c[Banned] &9[Muted]"));
                if(!hasAlts) {
                    player.sendMessage(CC.sendRed("That player does not have any alts!"));
                } else {
                    StringBuilder altsList = new StringBuilder();
                    for (String altName : playerNamesWithSameIP) {
                        OfflinePlayer alt = Bukkit.getOfflinePlayer(altName);
                        boolean isBanned = punishmentManager.isPlayerBanned(alt);
                        boolean isMuted = punishmentManager.isPlayerMuted(alt);
                        boolean onlineAlt = alt.isOnline();

                        String colorCode;
                        if (isBanned) {
                            colorCode = "&c";
                        } else if (isMuted) {
                            colorCode = "&9";
                        } else if (onlineAlt) {
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
            player.sendMessage(CC.send("&f----------------------------------------"));
        }
    }

}
