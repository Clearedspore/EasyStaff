package me.clearedspore.command.chat;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.util.P;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;


@CommandPermission(P.chat)
@CommandAlias("chat|managechat")
public class ChatCommand extends BaseCommand implements Listener {

    private final JavaPlugin plugin;

    public ChatCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Subcommand("clear")
    @CommandPermission(P.chat_clear)
    private void onChatClear(CommandSender player){
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

    @Subcommand("toggle")
    @CommandPermission(P.chat_toggle)
    public void onChatToggle(CommandSender player){
        boolean enabled = plugin.getConfig().getBoolean("chat.enabled");

        plugin.getConfig().set("chat.enabled", !enabled);
        player.sendMessage(CC.sendBlue("You have " + (enabled ? "&cdisabled" : "&aenabled") + "&#00CCDE the chat"));
        for(Player targetPlayer : Bukkit.getOnlinePlayers()){
            if(targetPlayer.hasPermission(P.notify)){
                targetPlayer.sendMessage(CC.sendBlue("[Staff] The chat has been " + (enabled ? "&cdisabled": "&aenabled") + "&#00CCDE by &f" + player.getName()));
            }
        }
        plugin.saveConfig();
    }

    @EventHandler
    private void onChatMessage(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();
        boolean enabled = plugin.getConfig().getBoolean("chat.enabled");

        if(!enabled && !player.hasPermission(P.chat_toggle_bpyass)){
            event.setCancelled(true);
            player.sendMessage(CC.sendRed("The chat is currently muted!"));
        }

    }

}
