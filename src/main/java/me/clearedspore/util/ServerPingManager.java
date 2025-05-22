package me.clearedspore.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedServerPing;
import me.clearedspore.EasyStaff;
import me.clearedspore.easyAPI.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServerPingManager implements Listener {
    private final JavaPlugin plugin;
    private boolean customPlayerCountEnabled;
    private String playerCountText;
    private List<String> hoverText = new ArrayList<>();
    private ProtocolManager protocolManager;

    private String originalPlayerCountText;
    private List<String> originalHoverText = new ArrayList<>();
    private boolean wasCustomPlayerCountEnabled;

    public ServerPingManager(JavaPlugin plugin) {
        this.plugin = plugin;

        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            setupProtocolLib();
        } else {
            plugin.getLogger().warning("ProtocolLib not found! Custom player count hover text will not work properly.");
            plugin.getLogger().warning("Please install ProtocolLib for full functionality.");
        }
    }

    
    private void setupProtocolLib() {
        this.protocolManager = ProtocolLibrary.getProtocolManager();

        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Status.Server.SERVER_INFO) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (!customPlayerCountEnabled) return;
                
                PacketContainer packet = event.getPacket();
                WrappedServerPing ping = packet.getServerPings().read(0);

                String formattedText = CC.translate(playerCountText
                        .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                        .replace("{max}", String.valueOf(Bukkit.getMaxPlayers())));

                if (!hoverText.isEmpty()) {
                    List<WrappedGameProfile> samplePlayers = new ArrayList<>();
                    
                    for (String line : hoverText) {
                        String formattedLine = CC.translate(line
                                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                                .replace("{max}", String.valueOf(Bukkit.getMaxPlayers())));

                        UUID uuid = UUID.nameUUIDFromBytes(formattedLine.getBytes());
                        samplePlayers.add(new WrappedGameProfile(uuid, formattedLine));
                    }
                    
                    ping.setPlayers(samplePlayers);
                }

                ping.setVersionName(formattedText);
                ping.setVersionProtocol(-1);
                
                packet.getServerPings().write(0, ping);
            }
        });
    }

    public void setMaintenanceMode(boolean enabled) {
        if (enabled) {
            this.originalPlayerCountText = this.playerCountText;
            this.originalHoverText = new ArrayList<>(this.hoverText);
            this.wasCustomPlayerCountEnabled = this.customPlayerCountEnabled;

            List<String> maintenanceHoverText = plugin.getConfig().getStringList("maintenance.message");
            if (maintenanceHoverText.isEmpty()) {
                maintenanceHoverText = new ArrayList<>();
                maintenanceHoverText.add("&cMaintenance!");
                maintenanceHoverText.add("");
                maintenanceHoverText.add("&fMaintenance has been enabled please be patient while we resolve the issues");
                maintenanceHoverText.add("&fJoin our discord for updates: &bdiscord.gg/");
            }

            String maintenanceText = plugin.getConfig().getString("maintenance.server-ping.text", "&c&lMaintenance");
            this.playerCountText = maintenanceText;
            this.hoverText = maintenanceHoverText;

            this.customPlayerCountEnabled = true;

            plugin.getConfig().set("server-ping.custom-player-count.enabled", true);
            plugin.getConfig().set("server-ping.custom-player-count.maintenance-text", this.playerCountText);
            plugin.getConfig().set("server-ping.custom-player-count.maintenance-hover", this.hoverText);

        } else {
            this.playerCountText = this.originalPlayerCountText;
            this.hoverText = new ArrayList<>(this.originalHoverText);
            this.customPlayerCountEnabled = this.wasCustomPlayerCountEnabled;

            plugin.getConfig().set("server-ping.custom-player-count.enabled", this.customPlayerCountEnabled);
        }

        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null && protocolManager != null) {
            protocolManager.removePacketListeners(plugin);
            setupProtocolLib();
        }
    }
}