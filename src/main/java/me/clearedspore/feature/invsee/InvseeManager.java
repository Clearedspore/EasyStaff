package me.clearedspore.feature.invsee;

import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.util.P;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class InvseeManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, UUID> viewers = new HashMap<>();
    private final Map<UUID, Inventory> openInventories = new HashMap<>();
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> updateTasks = new HashMap<>();

    private static final int XP_BUTTON_SLOT = 45;
    private static final int CLEAR_BUTTON_SLOT = 46;
    private static final int FREEZE_BUTTON_SLOT = 47;

    public InvseeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }


    public void openInventory(Player viewer, Player target) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§8" + target.getName() + "'s Inventory");

        viewers.put(viewer.getUniqueId(), target.getUniqueId());
        openInventories.put(viewer.getUniqueId(), inventory);

        updateInventoryContents(viewer, target, inventory);

        viewer.openInventory(inventory);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (viewer.isOnline() && target.isOnline() && viewer.getOpenInventory().getTopInventory().equals(inventory)) {
                updateInventoryContents(viewer, target, inventory);
            } else {
                cancelUpdateTask(viewer.getUniqueId());
            }
        }, 10L, 10L);
        
        updateTasks.put(viewer.getUniqueId(), task);
    }


    private void updateInventoryContents(Player viewer, Player target, Inventory inventory) {
        ItemStack[] targetContents = target.getInventory().getContents();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, targetContents[i]);
        }

        ItemStack[] armorContents = target.getInventory().getArmorContents();
        for (int i = 0; i < 4; i++) {
            inventory.setItem(36 + i, armorContents[3 - i]);
        }

        inventory.setItem(40, target.getInventory().getItemInOffHand());

        addButtons(viewer, target, inventory);
    }


    private void addButtons(Player viewer, Player target, Inventory inventory) {
        boolean isAdmin = viewer.hasPermission(P.invsee_admin);

        ItemStack xpButton = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta xpMeta = xpButton.getItemMeta();
        xpMeta.setDisplayName(CC.sendGreen("Experience Information"));
        List<String> xpLore = new ArrayList<>();
        xpLore.add(CC.sendWhite("Level: §e" + target.getLevel()));
        xpLore.add(CC.sendWhite("XP: §e" + Math.round(target.getExp() * 100) + "%"));
        xpLore.add(CC.sendWhite("Total XP: §e" + target.getTotalExperience()));
        xpMeta.setLore(xpLore);
        xpButton.setItemMeta(xpMeta);
        inventory.setItem(XP_BUTTON_SLOT, xpButton);

        if (isAdmin) {
            ItemStack clearButton = new ItemStack(Material.BARRIER);
            ItemMeta clearMeta = clearButton.getItemMeta();
            clearMeta.setDisplayName(CC.sendRed("Clear Inventory"));
            List<String> clearLore = new ArrayList<>();
            clearLore.add(CC.sendWhite("Click to clear the player's inventory"));
            clearLore.add(CC.sendWhite("§cThis action cannot be undone!"));
            clearMeta.setLore(clearLore);
            clearButton.setItemMeta(clearMeta);
            inventory.setItem(CLEAR_BUTTON_SLOT, clearButton);

            boolean isFrozen = frozenPlayers.contains(target.getUniqueId());
            ItemStack freezeButton = new ItemStack(isFrozen ? Material.PACKED_ICE : Material.ICE);
            ItemMeta freezeMeta = freezeButton.getItemMeta();
            freezeMeta.setDisplayName(isFrozen ? CC.sendAqua("Unfreeze Inventory") : CC.sendAqua("Freeze Inventory"));
            List<String> freezeLore = new ArrayList<>();
            freezeLore.add(CC.sendWhite("Status: " + (isFrozen ? "§cFrozen" : "§aNot Frozen")));
            freezeLore.add(CC.sendWhite("Click to " + (isFrozen ? "unfreeze" : "freeze") + " the player's inventory"));
            freezeMeta.setLore(freezeLore);
            freezeButton.setItemMeta(freezeMeta);
            inventory.setItem(FREEZE_BUTTON_SLOT, freezeButton);
        }

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        
        for (int i = 45; i < 54; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }


    private void cancelUpdateTask(UUID viewerUuid) {
        BukkitTask task = updateTasks.remove(viewerUuid);
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerUuid = player.getUniqueId();

        if (viewers.containsKey(playerUuid) && event.getView().getTopInventory().equals(openInventories.get(playerUuid))) {
            UUID targetUuid = viewers.get(playerUuid);
            Player target = Bukkit.getPlayer(targetUuid);
            
            if (target == null || !target.isOnline()) {
                player.closeInventory();
                return;
            }
            
            int slot = event.getRawSlot();
            boolean isTopInventory = event.getClickedInventory() == event.getView().getTopInventory();

            if (isTopInventory && slot >= 45 && slot < 54) {
                event.setCancelled(true);
                
                if (slot == XP_BUTTON_SLOT) {
                    return;
                }

                if (player.hasPermission(P.invsee_admin)) {
                    if (slot == CLEAR_BUTTON_SLOT) {
                        clearTargetInventory(target);
                        player.sendMessage(CC.sendGreen("You cleared " + target.getName() + "'s inventory."));
                        return;
                    } else if (slot == FREEZE_BUTTON_SLOT) {
                        toggleInventoryFreeze(target);
                        boolean isFrozen = frozenPlayers.contains(targetUuid);
                        player.sendMessage(CC.sendGreen("You " + (isFrozen ? "froze" : "unfroze") + " " + target.getName() + "'s inventory."));

                        updateInventoryContents(player, target, event.getView().getTopInventory());
                        return;
                    }
                }
                return;
            }

            boolean isAdmin = player.hasPermission(P.invsee_admin);

            if (!isAdmin && isTopInventory) {
                event.setCancelled(true);
                return;
            }

            if (frozenPlayers.contains(targetUuid) && isTopInventory) {
                event.setCancelled(true);
                return;
            }

            if (isTopInventory && slot >= 36 && slot <= 40) {
                event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                ItemStack cursorItem = event.getCursor();

                int targetSlot;
                if (slot >= 36 && slot <= 39) {
                    targetSlot = 39 - (slot - 36);
                } else {
                    targetSlot = 40;
                }

                if (cursorItem == null || cursorItem.getType() == Material.AIR) {
                    if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                        target.getInventory().setItem(targetSlot, null);
                        player.setItemOnCursor(clickedItem);
                    }
                } else {

                    target.getInventory().setItem(targetSlot, cursorItem);
                    player.setItemOnCursor(clickedItem);
                }
                

                target.updateInventory();

                updateInventoryContents(player, target, event.getView().getTopInventory());
                return;
            }

            if (isTopInventory && slot >= 0 && slot < 36) {
                event.setCancelled(true);

                ItemStack clickedItem = event.getCurrentItem();
                ItemStack cursorItem = event.getCursor();

                switch (event.getClick()) {
                    case LEFT:
                    case RIGHT:
                        if (cursorItem == null || cursorItem.getType() == Material.AIR) {
                            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                                target.getInventory().setItem(slot, null);
                                player.setItemOnCursor(clickedItem.clone());
                            }
                        } else {
                            target.getInventory().setItem(slot, cursorItem.clone());
                            player.setItemOnCursor(null);
                        }
                        break;
                    case SHIFT_LEFT:
                    case SHIFT_RIGHT:
                        if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                            target.getInventory().setItem(slot, null);

                            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(clickedItem.clone());

                            if (!leftover.isEmpty()) {
                                target.getInventory().setItem(slot, leftover.get(0));
                            }
                        }
                        break;
                    case SWAP_OFFHAND:
                        ItemStack playerOffhand = player.getInventory().getItemInOffHand();
                        if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                            target.getInventory().setItem(slot, playerOffhand != null ? playerOffhand.clone() : null);
                            player.getInventory().setItemInOffHand(clickedItem.clone());
                        } else if (playerOffhand != null && playerOffhand.getType() != Material.AIR) {
                            target.getInventory().setItem(slot, playerOffhand.clone());
                            player.getInventory().setItemInOffHand(null);
                        }
                        break;
                    case NUMBER_KEY:
                        int hotbarSlot = event.getHotbarButton();
                        ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
                        
                        if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                            target.getInventory().setItem(slot, hotbarItem != null ? hotbarItem.clone() : null);
                            player.getInventory().setItem(hotbarSlot, clickedItem.clone());
                        } else if (hotbarItem != null && hotbarItem.getType() != Material.AIR) {
                            target.getInventory().setItem(slot, hotbarItem.clone());
                            player.getInventory().setItem(hotbarSlot, null);
                        }
                        break;
                    default:
                        break;
                }
                player.updateInventory();
                target.updateInventory();

                updateInventoryContents(player, target, event.getView().getTopInventory());
                return;
            }

            if (!isTopInventory && isAdmin) {

                return;
            }
        }

        if (frozenPlayers.contains(playerUuid)) {
            event.setCancelled(true);
            player.sendMessage(CC.sendRed("Your inventory has been frozen by a staff member."));
        }
    }


    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerUuid = player.getUniqueId();

        if (viewers.containsKey(playerUuid) && event.getView().getTopInventory().equals(openInventories.get(playerUuid))) {
            UUID targetUuid = viewers.get(playerUuid);
            Player target = Bukkit.getPlayer(targetUuid);
            
            if (target == null || !target.isOnline()) {
                player.closeInventory();
                return;
            }

            if (!player.hasPermission(P.invsee_admin)) {
                event.setCancelled(true);
                return;
            }

            if (frozenPlayers.contains(targetUuid)) {
                event.setCancelled(true);
                return;
            }

            boolean affectsTopInventory = false;
            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) {
                    affectsTopInventory = true;
                    break;
                }
            }
            
            if (affectsTopInventory) {
                event.setCancelled(true);

                ItemStack draggedItem = event.getOldCursor();
                
                if (draggedItem != null && draggedItem.getType() != Material.AIR) {
                    for (int slot : event.getRawSlots()) {
                        if (slot < event.getView().getTopInventory().getSize()) {
                            if (slot >= 45 || (slot >= 36 && slot <= 40)) {
                                continue;
                            }

                            int amount = 1;
                            if (event.getType() == org.bukkit.event.inventory.DragType.EVEN) {
                                amount = draggedItem.getAmount() / event.getRawSlots().size();
                                if (amount < 1) amount = 1;
                            }

                            ItemStack itemToPlace = draggedItem.clone();
                            itemToPlace.setAmount(amount);

                            target.getInventory().setItem(slot, itemToPlace);
                        }
                    }

                    target.updateInventory();

                    updateInventoryContents(player, target, event.getView().getTopInventory());
                }
            }
        }

        if (frozenPlayers.contains(playerUuid)) {
            event.setCancelled(true);
            player.sendMessage(CC.sendRed("Your inventory has been frozen by a staff member."));
        }
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        if (viewers.containsKey(playerUuid) && event.getInventory().equals(openInventories.get(playerUuid))) {
            viewers.remove(playerUuid);
            openInventories.remove(playerUuid);
            cancelUpdateTask(playerUuid);
        }
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        if (viewers.containsKey(playerUuid)) {
            viewers.remove(playerUuid);
            openInventories.remove(playerUuid);
            cancelUpdateTask(playerUuid);
        }

        if (frozenPlayers.contains(playerUuid)) {
            frozenPlayers.remove(playerUuid);
        }

        for (Map.Entry<UUID, UUID> entry : new HashMap<>(viewers).entrySet()) {
            if (entry.getValue().equals(playerUuid)) {
                UUID viewerUuid = entry.getKey();
                Player viewer = Bukkit.getPlayer(viewerUuid);
                
                if (viewer != null && viewer.isOnline()) {
                    viewer.closeInventory();
                    viewer.sendMessage(CC.sendRed(player.getName() + " has disconnected."));
                }
                
                viewers.remove(viewerUuid);
                openInventories.remove(viewerUuid);
                cancelUpdateTask(viewerUuid);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (frozenPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(CC.sendRed("Your inventory has been frozen by a staff member."));
        }
    }

    private void clearTargetInventory(Player target) {
        target.getInventory().clear();
        target.updateInventory();
        target.sendMessage(CC.sendRed("Your inventory has been cleared by a staff member."));
    }


    private void toggleInventoryFreeze(Player target) {
        UUID targetUuid = target.getUniqueId();
        
        if (frozenPlayers.contains(targetUuid)) {
            frozenPlayers.remove(targetUuid);
            target.sendMessage(CC.sendGreen("Your inventory has been unfrozen."));
        } else {
            frozenPlayers.add(targetUuid);
            target.sendMessage(CC.sendRed("Your inventory has been frozen by a staff member."));
        }
    }

    public boolean isInventoryFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }
}