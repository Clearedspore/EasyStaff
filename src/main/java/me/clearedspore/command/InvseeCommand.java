package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import me.clearedspore.feature.invsee.InvseeManager;
import me.clearedspore.util.P;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("checkinv|invsee|openinv")
@CommandPermission(P.invsee)
public class InvseeCommand extends BaseCommand {

    private final InvseeManager invseeManager;

    public InvseeCommand(InvseeManager invseeManager) {
        this.invseeManager = invseeManager;
    }

    @Default
    @CommandCompletion("@players")
    @Description("View and interact with a player's inventory")
    @Syntax("<player>")
    public void onInvsee(Player player, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            player.sendMessage("§cPlayer not found.");
            return;
        }
        
        invseeManager.openInventory(player, target);
    }
}
