package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.util.PS;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("helpop|requesthelp|helpstaff")
@CommandPermission(PS.requesthelp)
public class StaffHelp extends BaseCommand {


    @Default
    private void onRequestHelp(Player player, String... reasonParts) {
        String reason = String.join(" ", reasonParts);

        TextColor blueColor = TextColor.fromHexString("#00CCDE");

        if(reason.isEmpty()){
            player.sendMessage(CC.sendRed("You must provide a reason!"));
            return;
        }

        Component message = Component.text(player.getName() + " requested help\n")
                .color(blueColor)
                .append(Component.text("Reason: ").color(NamedTextColor.YELLOW))
                .append(Component.text(reason).color(NamedTextColor.WHITE))
                .append(Component.text("\nAction: ").color(NamedTextColor.YELLOW))
                .append(Component.text("[Teleport] ")
                        .color(NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/staff-tp " + player.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to teleport to " + player.getName()))))
                .append(Component.text("[Message]")
                        .color(NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.suggestCommand("/msg " + player.getName() + " "))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to message " + player.getName()))));

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(PS.staff_view)) {
                staff.sendMessage("");
                staff.sendMessage(message);
                staff.sendMessage("");
            }
        }
    }
}
