package me.clearedspore.feature.staffmode;

import me.clearedspore.EasyStaff;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.easyAPI.util.Logger;
import me.clearedspore.storage.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StaffModeManager implements Listener {
    private final EasyStaff plugin;
    private final Logger logger;
    private final PlayerData playerData;
    private final VanishManager vanishManager;
    private final Map<String, StaffMode> staffModes;
    private final Map<UUID, StaffModeState> playerStates;
    private final StaffModeItemManager itemManager;

    public StaffModeManager(EasyStaff plugin, Logger logger, PlayerData playerData, VanishManager vanishManager) {
        this.plugin = plugin;
        this.logger = logger;
        this.playerData = playerData;
        this.vanishManager = vanishManager;
        this.staffModes = new HashMap<>();
        this.playerStates = new HashMap<>();
        this.itemManager = new StaffModeItemManager(plugin);
        
        loadStaffModes();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getPluginManager().registerEvents(itemManager, plugin);

        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Server.NAMED_SOUND_EFFECT,
                PacketType.Play.Server.ENTITY_SOUND,
                PacketType.Play.Server.STOP_SOUND,
                PacketType.Play.Server.BLOCK_ACTION,
                PacketType.Play.Server.WORLD_EVENT,
                PacketType.Play.Server.WORLD_PARTICLES,
                PacketType.Play.Server.ENTITY_EFFECT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                if (isInStaffMode(player)) {
                    if (event.getPacketType() == PacketType.Play.Server.NAMED_SOUND_EFFECT ||
                        event.getPacketType() == PacketType.Play.Server.ENTITY_SOUND ||
                        event.getPacketType() == PacketType.Play.Server.STOP_SOUND) {
                        event.setCancelled(true);
                    } else if (event.getPacketType() == PacketType.Play.Server.WORLD_EVENT) {
                        int effectId = event.getPacket().getIntegers().read(0);
                        if (effectId >= 1000) {
                            event.setCancelled(true);
                        }
                    } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_EFFECT) {
                        event.setCancelled(true);
                    }
                }
            }
        });

        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.MONITOR,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.BLOCK_DIG,
                PacketType.Play.Client.ARM_ANIMATION,
                PacketType.Play.Client.USE_ENTITY,
                PacketType.Play.Client.USE_ITEM) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
            }
        });
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        StaffMode mode = getPlayerStaffMode(player);

        if (mode != null) {
            Action action = event.getAction();
            ItemStack item = player.getInventory().getItemInMainHand();

            event.setCancelled(true);

            if (item != null && itemManager.isStaffItem(item)) {
                if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                }
            } else if (mode.isInventory()) {
                event.setCancelled(false);
            }
        }
    }
    
    public void loadStaffModes() {
        File modesFolder = new File(plugin.getDataFolder(), "modes");
        if (!modesFolder.exists()) {
            modesFolder.mkdirs();
        }
        
        File[] modeFiles = modesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (modeFiles != null) {
            for (File file : modeFiles) {
                try {
                    StaffMode mode = new StaffMode(file);
                    staffModes.put(mode.getName().toLowerCase(), mode);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        }

    }
    
    public boolean enableStaffMode(Player player) {
        if (isInStaffMode(player)) {
            player.sendMessage(CC.sendRed("You are already in staff mode!"));
            return false;
        }
        
        StaffMode bestMode = getBestModeForPlayer(player);
        if (bestMode == null) {
            player.sendMessage(CC.sendRed("You don't have permission to use any staff mode!"));
            return false;
        }
        
        return enableStaffMode(player, bestMode);
    }
    
    public boolean enableStaffMode(Player player, String modeName) {
        StaffMode mode = staffModes.get(modeName.toLowerCase());
        if (mode == null) {
            player.sendMessage(CC.sendRed("Staff mode '" + modeName + "' does not exist!"));
            return false;
        }
        
        if (!player.hasPermission(mode.getPermission())) {
            player.sendMessage(CC.sendRed("You don't have permission to use this staff mode!"));
            return false;
        }
        
        return enableStaffMode(player, mode);
    }
    
    private boolean enableStaffMode(Player player, StaffMode mode) {
        StaffModeState state = playerStates.get(player.getUniqueId());

        if (state == null) {
            state = new StaffModeState(
                player.getLocation(),
                player.getInventory().getContents().clone(),
                player.getGameMode()
            );
            playerStates.put(player.getUniqueId(), state);
        }

        boolean wasVanishedBefore = vanishManager.isVanished(player);

        player.setGameMode(mode.getGamemode());
        player.setInvulnerable(mode.isInvulnerable());

        player.getInventory().clear();
        for (Map.Entry<String, Integer> entry : mode.getItems().entrySet()) {
            ItemStack item = itemManager.getItem(entry.getKey());
            if (item != null) {
                player.getInventory().setItem(entry.getValue() - 1, item);
            }
        }

        for (String command : mode.getEnableCommands()) {
            executeCommand(command, player);
        }

        if (mode.isVanished() && !wasVanishedBefore) {
            vanishManager.setVanished(player, true);
        }

        if(mode.isFlight()){
            player.setAllowFlight(true);
        }

        saveStaffModeToPlayerData(player, mode.getName());
        
        player.sendMessage(CC.sendGreen("You are now in " + mode.getName() + "!"));
        return true;
    }
    
    public boolean disableStaffMode(Player player) {
        if (!isInStaffMode(player)) {
            player.sendMessage(CC.sendRed("You are not in staff mode!"));
            return false;
        }
        
        StaffMode currentMode = getPlayerStaffMode(player);
        if (currentMode == null) {
            player.sendMessage(CC.sendRed("Failed to determine your current staff mode!"));
            return false;
        }

        for (String command : currentMode.getDisableCommands()) {
            executeCommand(command, player);
        }

        if(currentMode.isFlight()){
            player.setAllowFlight(false);
        }

        if (vanishManager.isVanished(player)) {
            vanishManager.setVanished(player, false);

            player.sendMessage(CC.sendBlue("Your vanish has been disabled."));
        }

        itemManager.onStaffModeExit(player);


        StaffModeState state = playerStates.get(player.getUniqueId());
        if (state != null) {
            player.getInventory().clear();
            player.setGameMode(state.getGameMode());
            player.getInventory().setContents(state.getInventory());
            player.teleport(state.getLocation());
            player.setInvulnerable(false);
            playerStates.remove(player.getUniqueId());
        }

        removeStaffModeFromPlayerData(player);
        
        player.sendMessage(CC.sendGreen("You are no longer in staff mode!"));
        return true;
    }
    
    private void executeCommand(String command, Player player) {
        if (command.startsWith("%console%")) {
            String consoleCommand = command.substring(9).replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), consoleCommand);
        } else if (command.startsWith("%player_cmd%")) {
            String playerCommand = command.substring(12);
            player.performCommand(playerCommand);
        }
    }
    
    public StaffMode getBestModeForPlayer(Player player) {
        StaffMode bestMode = null;
        int highestWeight = -1;
        
        for (StaffMode mode : staffModes.values()) {
            if (player.hasPermission(mode.getPermission()) && mode.getWeight() > highestWeight) {
                bestMode = mode;
                highestWeight = mode.getWeight();
            }
        }
        
        return bestMode;
    }
    
    public boolean isInStaffMode(Player player) {
        FileConfiguration playerConfig = playerData.getPlayerConfig(player);
        return playerConfig != null && playerConfig.contains("staffmode");
    }
    
    public StaffMode getPlayerStaffMode(Player player) {
        if (!isInStaffMode(player)) {
            return null;
        }
        
        FileConfiguration playerConfig = playerData.getPlayerConfig(player);
        String modeName = playerConfig.getString("staffmode");
        return staffModes.get(modeName.toLowerCase());
    }
    
    private void saveStaffModeToPlayerData(Player player, String modeName) {
        FileConfiguration playerConfig = playerData.getPlayerConfig(player);
        playerConfig.set("staffmode", modeName.toLowerCase());

        StaffModeState state = playerStates.get(player.getUniqueId());
        if (state != null) {
            if (playerConfig.contains("staffmode-data.inventory")) {
                playerConfig.set("staffmode-data.inventory", null);
            }

            Location loc = state.getLocation();
            playerConfig.set("staffmode-data.location.world", loc.getWorld().getName());
            playerConfig.set("staffmode-data.location.x", loc.getX());
            playerConfig.set("staffmode-data.location.y", loc.getY());
            playerConfig.set("staffmode-data.location.z", loc.getZ());
            playerConfig.set("staffmode-data.location.yaw", loc.getYaw());
            playerConfig.set("staffmode-data.location.pitch", loc.getPitch());

            playerConfig.set("staffmode-data.gamemode", state.getGameMode().toString());

            int itemCount = 0;
            for (int i = 0; i < state.getInventory().length; i++) {
                ItemStack item = state.getInventory()[i];
                if (item != null) {
                    playerConfig.set("staffmode-data.inventory." + i, item);
                    itemCount++;
                }
            }

            playerConfig.set("staffmode-data.was-vanished", vanishManager.isVanished(player));
        } else {
        }
        
        playerData.savePlayerData(playerConfig, playerData.getPlayerFile(player));
    }
    
    private void removeStaffModeFromPlayerData(Player player) {
        FileConfiguration playerConfig = playerData.getPlayerConfig(player);
        playerConfig.set("staffmode", null);
        playerConfig.set("staffmode-data", null);
        playerData.savePlayerData(playerConfig, playerData.getPlayerFile(player));
    }
    
    public List<String> getAvailableModesForPlayer(Player player) {
        List<String> modes = new ArrayList<>();
        for (StaffMode mode : staffModes.values()) {
            if (player.hasPermission(mode.getPermission())) {
                modes.add(mode.getName());
            }
        }
        return modes;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        FileConfiguration playerConfig = playerData.getPlayerConfig(player);
        if (playerConfig != null && playerConfig.contains("staffmode")) {
            String modeName = playerConfig.getString("staffmode");
            StaffMode mode = staffModes.get(modeName.toLowerCase());
            
            if (mode != null && player.hasPermission(mode.getPermission())) {
                if (playerConfig.contains("staffmode-data")) {
                    try {
                        String worldName = playerConfig.getString("staffmode-data.location.world");
                        double x = playerConfig.getDouble("staffmode-data.location.x");
                        double y = playerConfig.getDouble("staffmode-data.location.y");
                        double z = playerConfig.getDouble("staffmode-data.location.z");
                        float yaw = (float) playerConfig.getDouble("staffmode-data.location.yaw");
                        float pitch = (float) playerConfig.getDouble("staffmode-data.location.pitch");
                        Location location = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);

                        GameMode gameMode = GameMode.valueOf(playerConfig.getString("staffmode-data.gamemode"));

                        ItemStack[] inventory = new ItemStack[36];
                        if (playerConfig.contains("staffmode-data.inventory")) {
                            for (String key : playerConfig.getConfigurationSection("staffmode-data.inventory").getKeys(false)) {
                                int slot = Integer.parseInt(key);
                                if (slot >= 0 && slot < inventory.length) {
                                    inventory[slot] = playerConfig.getItemStack("staffmode-data.inventory." + key);
                                }
                            }
                        }

                        StaffModeState state = new StaffModeState(location, inventory, gameMode);
                        playerStates.put(player.getUniqueId(), state);

                        final Location finalLocation = location;

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.getInventory().clear();
                            player.getInventory().setContents(inventory);
                            player.teleport(finalLocation);
                            enableStaffMode(player, mode);
                        }, 10L);
                        
                        return;
                    } catch (Exception e) {
                        logger.error("Failed to load staff mode state for player: " + player.getName());
                        logger.error(e.getMessage());
                    }
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    enableStaffMode(player, mode);
                }, 5L);
            } else {
                removeStaffModeFromPlayerData(player);
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (isInStaffMode(player)) {
            StaffMode currentMode = getPlayerStaffMode(player);
            if (currentMode != null) {
                saveStaffModeToPlayerData(player, currentMode.getName());
            }
        }
        
        playerStates.remove(player.getUniqueId());
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        StaffMode mode = getPlayerStaffMode(player);
        
        if (mode != null) {
            if (!mode.isBlockBreak()) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        StaffMode mode = getPlayerStaffMode(player);
        
        if (mode != null) {
            if (!mode.isBlockPlace()) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            StaffMode mode = getPlayerStaffMode(player);
            
            if (mode != null && mode.isInvulnerable()) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            StaffMode mode = getPlayerStaffMode(player);
            
            if (mode != null && !mode.isPvp()) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            StaffMode mode = getPlayerStaffMode(player);
            
            if (mode != null && !mode.isItemPickup()) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        StaffMode mode = getPlayerStaffMode(player);
        
        if (mode != null && !mode.isItemDrop()) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            StaffMode mode = getPlayerStaffMode(player);
            
            if (mode != null && !mode.isInventory()) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        StaffMode mode = getPlayerStaffMode(player);
        
        if (mode != null && !mode.isChat()) {
            event.setCancelled(true);
            player.sendMessage(CC.sendRed("You cannot chat while in this staff mode!"));
        }
    }
    
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        
        if (isInStaffMode(player)) {
            event.setCancelled(true);
            ItemStack item = player.getInventory().getItemInMainHand();
            if (itemManager.isStaffItem(item)) {
                itemManager.handleItemUse(player, event.getRightClicked(), item);
            }
        }
    }
    

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isInStaffMode(player)) {
        }
    }
    

    @EventHandler
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (isInStaffMode(player)) {
            event.setCancelled(true);
        }
    }
    

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (isInStaffMode(player)) {
            event.setCancelled(true);
        }
    }
    
    private static class StaffModeState {
        private final Location location;
        private final ItemStack[] inventory;
        private final GameMode gameMode;
        
        public StaffModeState(Location location, ItemStack[] inventory, GameMode gameMode) {
            this.location = location.clone();

            this.inventory = new ItemStack[inventory.length];
            for (int i = 0; i < inventory.length; i++) {
                if (inventory[i] != null) {
                    this.inventory[i] = inventory[i].clone();
                }
            }
            
            this.gameMode = gameMode;
        }
        
        public Location getLocation() {
            return location;
        }
        
        public ItemStack[] getInventory() {
            ItemStack[] clonedInventory = new ItemStack[inventory.length];
            for (int i = 0; i < inventory.length; i++) {
                if (inventory[i] != null) {
                    clonedInventory[i] = inventory[i].clone();
                }
            }
            return clonedInventory;
        }
        
        public GameMode getGameMode() {
            return gameMode;
        }
    }
}
