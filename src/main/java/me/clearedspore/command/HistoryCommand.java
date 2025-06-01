package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.notification.NotificationManager;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.feature.punishment.menu.history.HistoryMenu;
import me.clearedspore.util.P;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@CommandAlias("history|c|hist|checkhisotry|punishments|checkp")
@CommandPermission(P.history)
public class HistoryCommand extends BaseCommand {

    private final JavaPlugin plugin;
    private final PunishmentManager punishmentManager;
    private final NotificationManager notificationManager;

    public HistoryCommand(JavaPlugin plugin, PunishmentManager punishmentManager, NotificationManager notificationManager) {
        this.plugin = plugin;
        this.punishmentManager = punishmentManager;
        this.notificationManager = notificationManager;
    }

    @Default
    @CommandCompletion("@players")
    @Syntax("<player>")
    private void onHistory(Player player, @Optional String targetName){
        if(targetName == null){

            new HistoryMenu(plugin, player, player, punishmentManager).open(player);

        } else {

            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            if(!player.hasPermission(P.history_others)){
                player.sendMessage(CC.sendRed("You don't have permission to view other players their history"));
                return;
            }

            notificationManager.notifyHistoryCheck(player, target);
            
            new HistoryMenu(plugin, target, player, punishmentManager).open(player);

        }
    }
}
