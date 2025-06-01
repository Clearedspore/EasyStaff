package me.clearedspore.feature.staffmode;

import me.clearedspore.EasyStaff;
import me.clearedspore.easyAPI.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class StaffModeItemManager implements Listener {
    private final EasyStaff plugin;
    private final Map<String, ItemStack> items;
    private final Map<String, BiConsumer<Player, Entity>> itemHandlers;
    private final Map<Player, Long> freezeCooldowns;
    private static final long COOLDOWN_TICKS = 10L;

    public StaffModeItemManager(EasyStaff plugin) {
        this.plugin = plugin;
        this.items = new HashMap<>();
        this.itemHandlers = new HashMap<>();
        this.freezeCooldowns = new HashMap<>();

        registerDefaultItems();
    }
    
    private void registerDefaultItems() {
        ItemStack compass = createItem(Material.COMPASS, "&aTeleport Tool", "&7Right-click to teleport 5 blocks forward" + "\n&7Left-click to teleport 10 blocks forward");
        items.put("compass", compass);

        ItemStack freezeItem = createItem(Material.ICE, "&aFreeze Player", "&7Right-click a player to freeze them");
        items.put("freeze", freezeItem);

        ItemStack vanishItem = createItem(Material.ENDER_EYE, "&aToggle Vanish", "&7Right-click to toggle vanish");
        items.put("vanish", vanishItem);

        ItemStack worldEditItem = createItem(Material.WOODEN_AXE, "&aWorldEdit Wand", "&7Use for WorldEdit selections");
        items.put("worldedit", worldEditItem);

        itemHandlers.put("freeze", (player, target) -> {
            if (target instanceof Player) {
                Player targetPlayer = (Player) target;

                long currentTime = System.currentTimeMillis();
                long lastUsed = freezeCooldowns.getOrDefault(player, 0L);

                if (currentTime - lastUsed >= COOLDOWN_TICKS * 50) {
                    player.performCommand("freeze " + targetPlayer.getName());
                    freezeCooldowns.put(player, currentTime);
                }
            }
        });
        
        itemHandlers.put("vanish", (player, target) -> {
            player.performCommand("vanish");
        });
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.translate(name));
        meta.setLore(Arrays.stream(lore).map(CC::translate).toList());
        item.setItemMeta(meta);
        return item;
    }
    
    public ItemStack getItem(String name) {
        return items.get(name);
    }
    
    public void registerItem(String name, ItemStack item, BiConsumer<Player, Entity> handler) {
        items.put(name, item);
        if (handler != null) {
            itemHandlers.put(name, handler);
        }
    }
    
    public void handleItemUse(Player player, Entity target, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        
        for (Map.Entry<String, ItemStack> entry : items.entrySet()) {
            if (isSimilar(item, entry.getValue())) {
                BiConsumer<Player, Entity> handler = itemHandlers.get(entry.getKey());
                if (handler != null) {
                    handler.accept(player, target);
                }
                break;
            }
        }
    }
    
    private boolean isSimilar(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return false;
        }
        
        if (item1.getType() != item2.getType()) {
            return false;
        }
        
        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();
        
        if (meta1 == null || meta2 == null) {
            return meta1 == meta2;
        }
        
        if (meta1.hasDisplayName() != meta2.hasDisplayName()) {
            return false;
        }
        
        return !meta1.hasDisplayName() || meta1.getDisplayName().equals(meta2.getDisplayName());
    }

    public boolean isStaffItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        for (ItemStack staffItem : items.values()) {
            if (isSimilar(item, staffItem)) {
                return true;
            }
        }
        
        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null) {
            if (item.getType() == Material.COMPASS && isSimilar(item, items.get("compass"))) {
                Location currentLocation = player.getLocation();
                float yaw = currentLocation.getYaw();
                float pitch = currentLocation.getPitch();
                double radians = Math.toRadians(yaw);

                double x = currentLocation.getX();
                double y = currentLocation.getY();
                double z = currentLocation.getZ();

                if (event.getAction().isRightClick()) {
                    x -= Math.sin(radians) * 5;
                    z += Math.cos(radians) * 5;
                    player.sendMessage(CC.sendGreen("Teleported 5 blocks forward!"));
                } else if (event.getAction().isLeftClick()) {
                    x -= Math.sin(radians) * 10;
                    z += Math.cos(radians) * 10;
                    player.sendMessage(CC.sendGreen("Teleported 10 blocks forward!"));
                }

                Location newLocation = new Location(currentLocation.getWorld(), x, y, z, yaw, pitch);
                player.teleport(newLocation);
                event.setCancelled(true);
            } else if (item.getType() == Material.ENDER_EYE && isSimilar(item, items.get("vanish"))) {
                event.setCancelled(true);
                player.performCommand("vanish");
            }
        }
    }

}