package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.util.PS;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@CommandPermission(PS.teleport_others)
@CommandAlias("tphere")
public class StaffTphereCommand extends BaseCommand {


    @Default
    @CommandCompletion("@players")
    @Syntax("<player>")
    private void onTphere(Player player, String targetName){

        Player target = Bukkit.getPlayerExact(targetName);

        if(!target.isOnline()){
            player.sendMessage(CC.sendRed(target.getName() + " is not online!"));
            return;
        }

        if(target == player){
            player.sendMessage(CC.sendRed("You can't teleport to yourself!"));
            return;
        }

        Location playerLocation = player.getLocation();
        target.teleport(playerLocation);
        player.sendMessage(CC.sendBlue("Teleported " + target.getName() + " to yourself"));
        for(Player players : Bukkit.getOnlinePlayers()){
            if(players.hasPermission(PS.notify)){
                players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEteleported &f" + targetName + " &#00CCDEto themself"));
            }
        }
    }
}

