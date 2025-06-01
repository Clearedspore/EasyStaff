package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.notification.NotificationManager;
import me.clearedspore.util.P;
import org.bukkit.entity.Player;

import java.util.List;

@CommandAlias("notify")
@CommandPermission(P.punish_notify_high)
public class NotifyCommand extends BaseCommand {

    private final NotificationManager notificationManager;

    public NotifyCommand(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    @Subcommand("add")
    @CommandCompletion("@players")
    @Syntax("<player>")
    public void onAdd(Player player, String targetName) {
        boolean added = notificationManager.addPlayerToNotifyList(targetName);
        if (added) {
            player.sendMessage(CC.sendGreen("Added " + targetName + " to the notification list. You will be notified when someone checks their history."));
        } else {
            player.sendMessage(CC.sendRed(targetName + " is already on the notification list."));
        }
    }

    @Subcommand("remove")
    @CommandCompletion("@notifyPlayers")
    @Syntax("<player>")
    public void onRemove(Player player, String targetName) {
        boolean removed = notificationManager.removePlayerFromNotifyList(targetName);
        if (removed) {
            player.sendMessage(CC.sendGreen("Removed " + targetName + " from the notification list."));
        } else {
            player.sendMessage(CC.sendRed(targetName + " is not on the notification list."));
        }
    }

    @Subcommand("list")
    public void onList(Player player) {
        List<String> notifyList = notificationManager.getNotifyList();
        if (notifyList.isEmpty()) {
            player.sendMessage(CC.sendRed("The notification list is empty."));
            return;
        }

        player.sendMessage(CC.sendBlue("Players on the notification list:"));
        for (String name : notifyList) {
            player.sendMessage(CC.sendWhite("- " + name));
        }
    }

    @Default
    public void onDefault(Player player) {
        player.sendMessage(CC.sendBlue("Notification Commands:"));
        player.sendMessage(CC.sendWhite("/notify add <player> - Add a player to the notification list"));
        player.sendMessage(CC.sendWhite("/notify remove <player> - Remove a player from the notification list"));
        player.sendMessage(CC.sendWhite("/notify list - List all players on the notification list"));
    }
}