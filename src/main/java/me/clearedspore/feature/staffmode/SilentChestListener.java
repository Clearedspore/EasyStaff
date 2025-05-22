package me.clearedspore.feature.staffmode;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.google.errorprone.annotations.Keep;
import me.clearedspore.EasyStaff;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class SilentChestListener implements Listener {

    private final EasyStaff plugin;
    private final StaffModeManager staffModeManager;

    private static final Set<Material> SILENT_BLOCKS = EnumSet.of(
            Material.CHEST, Material.TRAPPED_CHEST,
            Material.ENDER_CHEST, Material.BARREL,
            Material.SHULKER_BOX, Material.BLACK_SHULKER_BOX,
            Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX,
            Material.CYAN_SHULKER_BOX, Material.GRAY_SHULKER_BOX,
            Material.GREEN_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX,
            Material.LIGHT_GRAY_SHULKER_BOX, Material.LIME_SHULKER_BOX,
            Material.MAGENTA_SHULKER_BOX, Material.ORANGE_SHULKER_BOX,
            Material.PINK_SHULKER_BOX, Material.PURPLE_SHULKER_BOX,
            Material.RED_SHULKER_BOX, Material.WHITE_SHULKER_BOX,
            Material.YELLOW_SHULKER_BOX
    );

    private static final Set<Sound> SILENT_SOUNDS = new HashSet<>();

    static {
        SILENT_SOUNDS.add(Sound.BLOCK_CHEST_OPEN);
        SILENT_SOUNDS.add(Sound.BLOCK_CHEST_CLOSE);
        SILENT_SOUNDS.add(Sound.BLOCK_ENDER_CHEST_OPEN);
        SILENT_SOUNDS.add(Sound.BLOCK_ENDER_CHEST_CLOSE);
        SILENT_SOUNDS.add(Sound.BLOCK_BARREL_OPEN);
        SILENT_SOUNDS.add(Sound.BLOCK_BARREL_CLOSE);
        SILENT_SOUNDS.add(Sound.BLOCK_SHULKER_BOX_OPEN);
        SILENT_SOUNDS.add(Sound.BLOCK_SHULKER_BOX_CLOSE);
    }

    public SilentChestListener(EasyStaff plugin, StaffModeManager staffModeManager) {
        this.plugin = plugin;
        this.staffModeManager = staffModeManager;
        registerPacketListeners();
    }

    private void registerPacketListeners() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.BLOCK_ACTION) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                if (staffModeManager.isInStaffMode(player)) {
                    event.setCancelled(true);
                }
            }
        });

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.NAMED_SOUND_EFFECT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                if (staffModeManager.isInStaffMode(player)) {
                    try {
                        Sound sound = event.getPacket().getSoundEffects().read(0);
                        if (SILENT_SOUNDS.contains(sound)) {
                            event.setCancelled(true);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to read sound packet: " + e.getMessage());
                    }
                }
            }
        });
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null) {
            return;
        }

        if (staffModeManager.isInStaffMode(player) && SILENT_BLOCKS.contains(block.getType())) {
            event.setCancelled(true);

            if (block.getState() instanceof Container) {
                Container container = (Container) block.getState();
                Inventory inventory = container.getInventory();
                player.openInventory(inventory);
            }
        }
    }
}