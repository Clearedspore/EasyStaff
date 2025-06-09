package me.clearedspore.feature.staffmode;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import me.clearedspore.EasyStaff;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.invsee.InvseeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

public class StaffModeItemManager implements Listener {
    private final EasyStaff plugin;
    private final Map<String, ItemStack> items;
    private final Map<String, BiConsumer<Player, Entity>> itemHandlers;
    private final Map<Player, Long> Cooldowns;
    private static final long COOLDOWN_TICKS = 10L;

    private final Map<UUID, Set<UUID>> glowingPlayers = new HashMap<>();
    private static final String GLOW_TEAM_PREFIX = "glow_";

    public StaffModeItemManager(EasyStaff plugin) {
        this.plugin = plugin;
        this.items = new HashMap<>();
        this.itemHandlers = new HashMap<>();
        this.Cooldowns = new HashMap<>();

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

        ItemStack punishItem = createItem(Material.IRON_AXE, "&aPunish player", "&7Right click a player to punish them");
        items.put("punish", punishItem);

        ItemStack invsee = createItem(Material.BOOK, "&aInspect Player", "&7right click to view a players inventory");
        items.put("invsee", invsee);

        ItemStack randomTeleportItem = createItem(Material.ENDER_PEARL, "&aRandom Teleport", "&7Right-click to teleport to a random player");
        items.put("randomteleport", randomTeleportItem);
        
//        ItemStack glowItem = createItem(Material.GLOWSTONE_DUST, "&aGlow Player", "&7Right-click a player to make them glow blue" + "\n&7Left-click to remove glow from all players");
//        items.put("glow", glowItem);

        itemHandlers.put("invsee", (player, target) -> {
           if(target instanceof Player){
               Player targetPlayer = (Player) target;

               InvseeManager invseeManager = EasyStaff.getInstance().getInvseeManager();
               invseeManager.openInventory(player, targetPlayer);
           }
        });

        itemHandlers.put("freeze", (player, target) -> {
            if (target instanceof Player) {
                Player targetPlayer = (Player) target;

                long currentTime = System.currentTimeMillis();
                long lastUsed = Cooldowns.getOrDefault(player, 0L);

                if (currentTime - lastUsed >= COOLDOWN_TICKS * 50) {
                    player.performCommand("freeze " + targetPlayer.getName());
                    Cooldowns.put(player, currentTime);
                }
            }
        });

        itemHandlers.put("punish", (player, target) -> {
            if(target instanceof Player){
                Player targetPlayer = (Player) target;
                player.performCommand("punish " + targetPlayer.getName());
            }
        });
        
        itemHandlers.put("vanish", (player, target) -> {
            player.performCommand("vanish");
        });
        
        itemHandlers.put("randomteleport", (player, target) -> {
            Player[] onlinePlayers = Bukkit.getOnlinePlayers().toArray(new Player[0]);
            if (onlinePlayers.length > 1) {
                Player randomPlayer;
                do {
                    randomPlayer = onlinePlayers[(int) (Math.random() * onlinePlayers.length)];
                } while (randomPlayer.equals(player));

                player.performCommand("tp " + randomPlayer.getName());
                player.sendMessage(CC.sendGreen("Teleported to " + randomPlayer.getName() + "!"));
            } else {
                player.sendMessage(CC.sendRed("No other players online to teleport to!"));
            }
        });
        
        itemHandlers.put("compass", (player, target) -> {
        });
        
        itemHandlers.put("glow", (player, target) -> {
            if (target instanceof Player) {
                long currentTime = System.currentTimeMillis();
                long lastUsed = Cooldowns.getOrDefault(player, 0L);
                Player targetPlayer = (Player) target;
                if (currentTime - lastUsed >= COOLDOWN_TICKS * 50) {
                    togglePlayerGlow(player, targetPlayer);
                }
            }
        });
    }

    private void togglePlayerGlow(Player staff, Player target) {
        UUID staffUUID = staff.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        Set<UUID> glowingForStaff = glowingPlayers.computeIfAbsent(staffUUID, k -> new HashSet<>());

        if (glowingForStaff.contains(targetUUID)) {
            removeGlow(staff, target);
            glowingForStaff.remove(targetUUID);
            staff.sendMessage(CC.sendGreen("Removed glow from " + target.getName()));
        } else {
            applyGlow(staff, target);
            glowingForStaff.add(targetUUID);
            staff.sendMessage(CC.sendGreen("Applied blue glow to " + target.getName()));
        }
    }

    private void applyGlow(Player staff, Player target) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();

        Scoreboard scoreboard;
        if (staff.getScoreboard() == Bukkit.getScoreboardManager().getMainScoreboard()) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            staff.setScoreboard(scoreboard);
        } else {
            scoreboard = staff.getScoreboard();
        }

        String teamName = GLOW_TEAM_PREFIX + staff.getUniqueId().toString().substring(0, 8);

        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
            team.setColor(ChatColor.BLUE);
        }

        team.addEntry(target.getName());

        sendGlowPacket(staff, target, true);
    }

    private void removeGlow(Player staff, Player target) {
        Scoreboard scoreboard = staff.getScoreboard();

        String teamName = GLOW_TEAM_PREFIX + staff.getUniqueId().toString().substring(0, 8);
        Team team = scoreboard.getTeam(teamName);
        
        if (team != null) {
            team.removeEntry(target.getName());
        }

        sendGlowPacket(staff, target, false);
    }

    private void sendGlowPacket(Player observer, Player target, boolean glowing) {
        try {
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);

            packet.getIntegers().write(0, target.getEntityId());

            WrappedDataWatcher watcher = new WrappedDataWatcher();

            WrappedDataWatcher.Serializer byteSerializer = WrappedDataWatcher.Registry.get(Byte.class);

            WrappedDataWatcher targetWatcher = WrappedDataWatcher.getEntityWatcher(target);
            byte entityFlags = 0;

            if (targetWatcher.hasIndex(0)) {
                try {
                    entityFlags = targetWatcher.getByte(0);
                } catch (Exception e) {
                    entityFlags = 0;
                }
            }
            

            if (glowing) {
                entityFlags |= 0x40;
            } else {
                entityFlags &= ~0x40;
            }

            watcher.setObject(0, byteSerializer, entityFlags);

            packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

            protocolManager.sendServerPacket(observer, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeAllGlows(Player staff) {
        UUID staffUUID = staff.getUniqueId();
        Set<UUID> glowingForStaff = glowingPlayers.get(staffUUID);
        
        if (glowingForStaff != null && !glowingForStaff.isEmpty()) {
            Set<UUID> glowingCopy = new HashSet<>(glowingForStaff);
            
            for (UUID targetUUID : glowingCopy) {
                Player target = Bukkit.getPlayer(targetUUID);
                if (target != null && target.isOnline()) {
                    removeGlow(staff, target);
                }
            }
            
            glowingForStaff.clear();
            staff.sendMessage(CC.sendGreen("Removed glow from all players"));
        }
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

        if (item != null && isStaffItem(item)) {
            event.setCancelled(true);
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
            } else if (item.getType() == Material.GLOWSTONE_DUST && isSimilar(item, items.get("glow"))) {
                if (event.getAction().isLeftClick()) {
                    removeAllGlows(player);
                } else if (event.getAction().isRightClick()) {
                    for (Map.Entry<String, ItemStack> entry : items.entrySet()) {
                        if (isSimilar(item, entry.getValue())) {
                            BiConsumer<Player, Entity> handler = itemHandlers.get(entry.getKey());
                            if (handler != null) {
                                handler.accept(player, null);
                            }
                            break;
                        }
                    }
                }
            } else if (event.getAction().isRightClick()) {
                for (Map.Entry<String, ItemStack> entry : items.entrySet()) {
                    if (isSimilar(item, entry.getValue())) {
                        BiConsumer<Player, Entity> handler = itemHandlers.get(entry.getKey());
                        if (handler != null) {
                            handler.accept(player, null);
                        }
                        break;
                    }
                }
            }
        }
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (glowingPlayers.containsKey(playerUUID)) {

            glowingPlayers.remove(playerUUID);
        }

        for (Map.Entry<UUID, Set<UUID>> entry : glowingPlayers.entrySet()) {
            if (entry.getValue().contains(playerUUID)) {
                Player staff = Bukkit.getPlayer(entry.getKey());
                if (staff != null && staff.isOnline()) {
                    Scoreboard scoreboard = staff.getScoreboard();
                    String teamName = GLOW_TEAM_PREFIX + staff.getUniqueId().toString().substring(0, 8);
                    Team team = scoreboard.getTeam(teamName);
                    if (team != null) {
                        team.removeEntry(player.getName());
                    }
                }
                entry.getValue().remove(playerUUID);
            }
        }
    }
    


    public void onStaffModeExit(Player player) {
        removeAllGlows(player);
        glowingPlayers.remove(player.getUniqueId());

        Scoreboard scoreboard = player.getScoreboard();
        String teamName = GLOW_TEAM_PREFIX + player.getUniqueId().toString().substring(0, 8);
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            team.unregister();
        }
        
        if (scoreboard != Bukkit.getScoreboardManager().getMainScoreboard()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }
}