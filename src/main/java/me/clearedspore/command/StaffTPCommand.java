package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.util.PS;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@CommandAlias("tp")
@CommandPermission(PS.teleport)
public class StaffTPCommand extends BaseCommand {

    @Default
    @CommandCompletion("@players @players")
    @Syntax("<player|x y z> [player]")
    private void onTeleport(Player player, String arg1, @Optional String arg2, @Optional String arg3) {
        if (arg2 == null && arg3 == null) {
            Player target = Bukkit.getPlayerExact(arg1);
            if (target != null && target.isOnline()) {
                if (target.equals(player)) {
                    player.sendMessage(CC.sendRed("You can't teleport to yourself!"));
                    return;
                }

                player.teleport(target.getLocation());
                player.sendMessage(CC.sendBlue("Teleported to &f" + target.getName()));
                for (Player players : Bukkit.getOnlinePlayers()) {
                    if (players.hasPermission(PS.notify)) {
                        players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEteleported to &f" + target.getName()));
                    }
                }
            } else {
                player.sendMessage(CC.sendRed("That player is not online!"));
            }
        } else if (arg2 != null && arg3 != null) {
            if (!player.hasPermission(PS.teleport_cords)) {
                player.sendMessage(CC.sendRed("You don't have permission to teleport to coordinates!"));
                return;
            }
            try {
                double x = Double.parseDouble(arg1);
                double y = Double.parseDouble(arg2);
                double z = Double.parseDouble(arg3);
                Location location = new Location(player.getWorld(), x, y, z);
                player.teleport(location);
                player.sendMessage(CC.sendBlue("Teleported to coordinates: &f" + x + ", " + y + ", " + z));
                for(Player players : Bukkit.getOnlinePlayers()){
                    if(players.hasPermission(PS.notify)){
                        players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEteleported to &f" + x + ", " + y + ", " + z));
                    }
                }
            } catch (NumberFormatException e) {
                player.sendMessage(CC.sendRed("Invalid coordinates!"));
            }
        } else if (arg2 != null) {
            if (!player.hasPermission(PS.teleport_others)) {
                player.sendMessage(CC.sendRed("You don't have permission to teleport players to yourself!"));
                return;
            }
            Player target1 = Bukkit.getPlayerExact(arg1);
            Player target2 = Bukkit.getPlayerExact(arg2);
            if (target1 != null && target1.isOnline() && target2 != null && target2.isOnline()) {
                target1.teleport(target2.getLocation());
                player.sendMessage(CC.sendBlue("Teleported &f" + target1.getName() + " &bto &f" + target2.getName()));
                for(Player players : Bukkit.getOnlinePlayers()){
                    if(players.hasPermission(PS.notify)){
                        players.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEteleported &f" + target1.getName() + " &#00CCDEto &f" + target2.getName()));
                    }
                }
            } else {
                player.sendMessage(CC.sendRed("One or both players are not online!"));
            }
        }
    }
}
