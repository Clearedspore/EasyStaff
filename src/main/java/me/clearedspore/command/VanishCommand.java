package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.staffmode.VanishManager;
import me.clearedspore.util.PS;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("vanish|v")
@CommandPermission(PS.vanish)
public class VanishCommand extends BaseCommand {

    private final VanishManager vanishManager;

    public VanishCommand(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @Default
    @CommandCompletion("@players")
    @Syntax("<player>")
    private void onVanish(Player player, @Optional String targetName){
        if(targetName == null){

            boolean isvanished = (vanishManager.isVanished(player));
            vanishManager.toggleVanished(player);
            player.sendMessage(CC.sendBlue("You have " + (!isvanished ? "&aEnabled" : "&cDisabled") + "&#00CCDE vanish"));
            for (Player players : Bukkit.getOnlinePlayers()){
                if(players.hasPermission(PS.notify) && !players.equals(player)){
                    players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + "&#00CCDE " + (!isvanished ? "&aEnabled" : "&cDisabled") + " &#00CCDEvanish "));
                }
            }
        } else {

            if(!player.hasPermission(PS.vanish_others)){
                player.sendMessage(CC.sendRed("You don't have permission to toggle other players their vanish"));
                return;
            }

            Player target = Bukkit.getPlayerExact(targetName);
            boolean isvanished = (vanishManager.isVanished(target));

            vanishManager.toggleVanished(target);
            player.sendMessage(CC.sendBlue("You have " + (!isvanished ? "&aEnabled" : "&cDisabled") + "&#00CCDE vanish for &f" + targetName));
            for (Player players : Bukkit.getOnlinePlayers()){
                if(players.hasPermission(PS.notify) && !players.equals(player)){
                    players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + "&#00CCDE " + (!isvanished ? "&aEnabled" : "&cDisabled") + " &#00CCDEvanish for &f" + targetName));
                }
            }

        }
    }
}
