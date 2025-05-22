package me.clearedspore.util;

import me.clearedspore.easyAPI.util.CC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatInputHandler implements Listener {
    
    private final JavaPlugin plugin;
    private final Map<UUID, Consumer<String>> awaitingInput = new HashMap<>();
    
    public ChatInputHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    

    public void awaitChatInput(Player player, Consumer<String> callback) {
        awaitingInput.put(player.getUniqueId(), callback);
        player.sendMessage(CC.sendBlue("Please type your message in chat. Type 'cancel' to cancel."));
    }

    public void cancelAwaitingInput(Player player) {
        awaitingInput.remove(player.getUniqueId());
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (awaitingInput.containsKey(playerId)) {
            event.setCancelled(true);
            String message = event.getMessage();
            
            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(CC.sendRed("Input cancelled."));
                awaitingInput.remove(playerId);
                return;
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Consumer<String> callback = awaitingInput.remove(playerId);
                if (callback != null) {
                    callback.accept(message);
                }
            });
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        awaitingInput.remove(event.getPlayer().getUniqueId());
    }
}