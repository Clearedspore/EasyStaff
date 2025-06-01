package me.clearedspore.feature.alertManager;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class XRayDetector implements Listener {
    private final JavaPlugin plugin;
    private final AlertManager alertManager;
    private final Set<Material> monitoredBlocks = new HashSet<>();
    private final int veinDetectionRadius;
    

    private final Map<String, Set<UUID>> alertedVeins = new HashMap<>();

    private BukkitTask cleanupTask;

    public XRayDetector(JavaPlugin plugin, AlertManager alertManager) {
        this.plugin = plugin;
        this.alertManager = alertManager;

        FileConfiguration config = plugin.getConfig();
        List<String> monitoredBlocksList = config.getStringList("alerts.monitored-blocks");

        for (String blockName : monitoredBlocksList) {
            try {
                Material material = Material.valueOf(blockName);
                monitoredBlocks.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid block type in config: " + blockName);
            }
        }
        
        veinDetectionRadius = config.getInt("alerts.vein-detection-radius", 5);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        cleanupTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            alertedVeins.clear();
        }, 12000L, 12000L);
    }

    public void cleanup() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }
        alertedVeins.clear();
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material blockType = block.getType();
        Player player = event.getPlayer();
        
        if (monitoredBlocks.contains(blockType) && !player.getGameMode().equals(GameMode.CREATIVE)) {
            VeinResult veinResult = calculateVein(block);
            int veinSize = veinResult.getSize();

            Block centerBlock = veinResult.getCenterBlock();
            String veinId = centerBlock.getWorld().getName() + ":" + 
                            centerBlock.getX() + ":" + 
                            centerBlock.getY() + ":" + 
                            centerBlock.getZ();

            Set<UUID> alertedPlayers = alertedVeins.computeIfAbsent(veinId, k -> new HashSet<>());
            
            if (!alertedPlayers.contains(player.getUniqueId())) {
                alertManager.xRayAlert(player, veinSize, block);

                alertedPlayers.add(player.getUniqueId());
            }
        }
    }

    private VeinResult calculateVein(Block startBlock) {
        Material targetMaterial = startBlock.getType();
        Set<Block> checkedBlocks = new HashSet<>();
        Queue<Block> blocksToCheck = new LinkedList<>();

        blocksToCheck.add(startBlock);
        checkedBlocks.add(startBlock);

        int minX = startBlock.getX(), maxX = startBlock.getX();
        int minY = startBlock.getY(), maxY = startBlock.getY();
        int minZ = startBlock.getZ(), maxZ = startBlock.getZ();

        while (!blocksToCheck.isEmpty() && checkedBlocks.size() <= 100) {
            Block currentBlock = blocksToCheck.poll();

            minX = Math.min(minX, currentBlock.getX());
            maxX = Math.max(maxX, currentBlock.getX());
            minY = Math.min(minY, currentBlock.getY());
            maxY = Math.max(maxY, currentBlock.getY());
            minZ = Math.min(minZ, currentBlock.getZ());
            maxZ = Math.max(maxZ, currentBlock.getZ());

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;

                        if (Math.abs(x) + Math.abs(y) + Math.abs(z) > veinDetectionRadius) continue;
                        
                        Block adjacentBlock = currentBlock.getRelative(x, y, z);

                        if (adjacentBlock.getType() == targetMaterial && !checkedBlocks.contains(adjacentBlock)) {
                            checkedBlocks.add(adjacentBlock);
                            blocksToCheck.add(adjacentBlock);
                        }
                    }
                }
            }
        }

        int centerX = (minX + maxX) / 2;
        int centerY = (minY + maxY) / 2;
        int centerZ = (minZ + maxZ) / 2;
        Block centerBlock = startBlock.getWorld().getBlockAt(centerX, centerY, centerZ);
        
        return new VeinResult(checkedBlocks.size(), centerBlock);
    }

    private static class VeinResult {
        private final int size;
        private final Block centerBlock;
        
        public VeinResult(int size, Block centerBlock) {
            this.size = size;
            this.centerBlock = centerBlock;
        }
        
        public int getSize() {
            return size;
        }
        
        public Block getCenterBlock() {
            return centerBlock;
        }
    }
}