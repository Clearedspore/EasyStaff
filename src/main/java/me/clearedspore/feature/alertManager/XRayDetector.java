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

import java.util.*;

public class XRayDetector implements Listener {
    private final JavaPlugin plugin;
    private final AlertManager alertManager;
    private final Set<Material> monitoredBlocks = new HashSet<>();
    private final int veinDetectionRadius;

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
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material blockType = block.getType();


        Player player = event.getPlayer();
        if (monitoredBlocks.contains(blockType) && !player.getGameMode().equals(GameMode.CREATIVE)) {

            int veinSize = calculateVeinSize(block);

            alertManager.xRayAlert(player, veinSize, block);
        }
    }

    private int calculateVeinSize(Block startBlock) {
        Material targetMaterial = startBlock.getType();
        Set<Block> checkedBlocks = new HashSet<>();
        Queue<Block> blocksToCheck = new LinkedList<>();

        blocksToCheck.add(startBlock);
        checkedBlocks.add(startBlock);

        while (!blocksToCheck.isEmpty() && checkedBlocks.size() <= 100) {
            Block currentBlock = blocksToCheck.poll();

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
        
        return checkedBlocks.size();
    }
}