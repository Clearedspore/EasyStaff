package me.clearedspore.feature.staffmode;

import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.channels.Channel;
import me.clearedspore.feature.channels.ChannelManager;
import me.clearedspore.feature.setting.SettingsManager;
import me.clearedspore.storage.PlayerData;
import me.clearedspore.util.P;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.nametag.NameTagManager;
import me.neznamy.tab.api.tablist.TabListFormatManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class VanishManager implements Listener {

    private final Set<Player> vanished = Collections.synchronizedSet(new HashSet<>());
    private BukkitTask vanishTask;
    private final NameTagManager nameTagManager;
    private final TabListFormatManager tablist;
    private final ChannelManager channelManager;
    private final JavaPlugin plugin;
    private final PlayerData playerData;
    private boolean tabEnabled;

    public VanishManager(NameTagManager nameTagManager, TabListFormatManager tablist, ChannelManager channelManager, JavaPlugin plugin, PlayerData playerData) {
        this.nameTagManager = nameTagManager;
        this.tablist = tablist;
        this.channelManager = channelManager;
        this.plugin = plugin;
        this.playerData = playerData;
        this.tabEnabled = plugin.getConfig().getBoolean("vanish.tab") && Bukkit.getPluginManager().isPluginEnabled("TAB");

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }


    public void setVanished(Player player, boolean enabled){
        if (enabled){
            vanished.add(player);

            TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
            if(tabPlayer != null && tabEnabled) {
                String suffix = tablist.getOriginalSuffix(tabPlayer);
                String playerTag = plugin.getConfig().getString("vanish.tag");
                playerData.setMainSuffix(player, suffix);
                nameTagManager.setSuffix(tabPlayer, playerTag);
                tablist.setSuffix(tabPlayer, playerTag);
            }

            for(Player onlinePlayer : Bukkit.getOnlinePlayers()){
                String leaveMessage = plugin.getConfig().getString("vanish.leave");
                leaveMessage = leaveMessage.replace("%player%", player.getName());
                if(!leaveMessage.equals("null")) {
                    onlinePlayer.sendMessage(CC.send(leaveMessage));
                }
                if(onlinePlayer.equals(player)) {
                    continue;
                } else if(onlinePlayer.hasPermission(P.vanish_see)) {
                    onlinePlayer.showPlayer(plugin, player);
                } else {
                    onlinePlayer.hidePlayer(plugin, player);
                }
            }

            startActionbar();

        } else {
            vanished.remove(player);

            TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
            if(tabPlayer != null && tabEnabled) {
                String suffix = playerData.getMainSuffix(player);
                nameTagManager.setSuffix(tabPlayer, suffix);
                tablist.setSuffix(tabPlayer, suffix);
            }

            for(Player onlinePlayer : Bukkit.getOnlinePlayers()){
                onlinePlayer.showPlayer(plugin, player);
                String joinMessage = plugin.getConfig().getString("vanish.join");
                joinMessage = joinMessage.replace("%player%", player.getName());
                if(!joinMessage.equals("null")) {
                    onlinePlayer.sendMessage(CC.send(joinMessage));
                }
            }

            if (vanished.isEmpty()) {
                stopActionbar();
            }
        }
    }

    public void toggleVanished(Player player){
        synchronized (vanished) {
            if(vanished.contains(player)){
                setVanished(player, false);
            } else {
                setVanished(player, true);
            }
        }
    }

    public boolean isVanished(Player player){
        return vanished.contains(player);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();

        if(player.hasPermission(P.vanish) && SettingsManager.isSettingEnabled(playerData, player, "vanish_on_join", true)){
            setVanished(player, true);
            event.setJoinMessage(null);
        }

        synchronized (vanished) {
            for(Player vanishedPlayer : new HashSet<>(vanished)) {
                if(vanishedPlayer != null && vanishedPlayer.isOnline()) {
                    if(player.hasPermission(P.vanish_see)) {
                        player.showPlayer(plugin, vanishedPlayer);
                    } else {
                        player.hidePlayer(plugin, vanishedPlayer);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event){
        Player player = event.getPlayer();

        if(isVanished(player)){
            setVanished(player, false);
        }

    }


    public void startActionbar() {
        if (vanishTask != null && !vanishTask.isCancelled()) {
            vanishTask.cancel();
        }

        vanishTask = new BukkitRunnable() {
            @Override
            public void run() {
                synchronized (vanished) {
                    for (Player player : new HashSet<>(vanished)) {
                        if (player != null && player.isOnline()) {
                            String channelID = channelManager.getPlayerChannel(player);
                            Channel channel = channelManager.getChannel(channelID);
                            String channelName = (channel != null) ? channel.getName() : "&aglobal";
                            player.sendActionBar(CC.sendBlue("vanished &a| &fchannel " + channelName));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void stopActionbar() {
        if (vanishTask != null) {
            vanishTask.cancel();
            vanishTask = null;
        }
    }


}
