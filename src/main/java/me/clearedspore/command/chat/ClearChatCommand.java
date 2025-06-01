package me.clearedspore.command.chat;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.util.P;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@CommandAlias("clearchat|chatclear|")
@CommandPermission(P.chat_clear)
public class ClearChatCommand extends BaseCommand {

    private final JavaPlugin plugin;

    public ClearChatCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    @Default
    private void onChatClear(Player player) {
        for (Player targetPlayer : Bukkit.getOnlinePlayers()) {
            if (!targetPlayer.hasPermission(P.chat_clear_bypass)) {
                int lines = plugin.getConfig().getInt("chat.clear-lines");
                for (int i = 0; i < lines; i++) {
                    targetPlayer.sendMessage("");
                }
                targetPlayer.sendMessage(CC.sendBlue("The chat has been cleared"));
            }

            if(targetPlayer.hasPermission(P.notify)){
                targetPlayer.sendMessage(CC.sendBlue("[Staff] The chat has been cleared by &f" + player.getName()));
            }
        }
    }
}
