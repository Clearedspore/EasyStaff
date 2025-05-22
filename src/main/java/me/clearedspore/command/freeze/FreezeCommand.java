package me.clearedspore.command.freeze;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.storage.PlayerData;
import me.clearedspore.util.PS;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@CommandAlias("freeze")
@CommandPermission(PS.freeze)
public class FreezeCommand extends BaseCommand implements Listener {

    private final PlayerData playerData;

    public FreezeCommand(PlayerData playerData) {
        this.playerData = playerData;
    }

    @Default
    @CommandCompletion("@players")
    @Syntax("<player>")
    private void onFreeze(CommandSender player, String targetName){
        Player target = Bukkit.getPlayerExact(targetName);

        if(!target.isOnline()){
            player.sendMessage(CC.sendRed("That player is not online"));
            return;
        }

        if(playerData.isFreezed(target)){
            playerData.setFreezed(target, false);
            target.sendMessage(CC.sendBlue("You have been unfrozen"));
            player.sendMessage(CC.sendBlue("You have unfrozen " + targetName));
        } else {
        playerData.setFreezed(target, true);
        player.sendMessage(CC.sendRed("You have frozen &f" + targetName));
        target.sendMessage(CC.sendRed("&lYou have been frozen!"));
        target.sendTitle(CC.sendRed("&lYou have been frozen!!"), CC.sendRed("Do not log out!"));
        }
    }

    @EventHandler
    private void onPlayerLeave(PlayerQuitEvent event){
        Player player = event.getPlayer();

        if(playerData.isFreezed(player)){
            for(Player players : Bukkit.getOnlinePlayers()){
                if(players.hasPermission(PS.freeze_notify)){
                    players.sendMessage(CC.sendBlue(""));
                    players.sendMessage(CC.sendBlue("&l" + player.getName() + " has logged out while frozen!"));
                    players.sendMessage(CC.sendBlue(""));
                    playerData.setFreezed(player, false);
                }
            }
        }
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event){
        Player player = event.getPlayer();

        if(playerData.isFreezed(player)){
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onInteract(PlayerInteractEvent event){
        Player player = event.getPlayer();

        if(playerData.isFreezed(player)){
            player.sendMessage(CC.sendRed("You are currently frozen and cannot do that!"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onInventoryOpen(InventoryOpenEvent event){
        Player player = (Player) event.getPlayer();

        if(playerData.isFreezed(player)){
            player.sendMessage(CC.sendRed("You are currently frozen and cannot do that!"));
            player.closeInventory();
        }
    }
}
