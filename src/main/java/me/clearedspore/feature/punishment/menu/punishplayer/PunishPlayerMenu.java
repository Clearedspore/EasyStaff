package me.clearedspore.feature.punishment.menu.punishplayer;

import me.clearedspore.easyAPI.menu.PaginatedMenu;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.punishment.PunishmentManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class PunishPlayerMenu extends PaginatedMenu {

    private final PunishmentManager punishmentManager;
    private final OfflinePlayer target;

    public PunishPlayerMenu(JavaPlugin plugin, PunishmentManager punishmentManager, OfflinePlayer target) {
        super(plugin);
        this.punishmentManager = punishmentManager;
        this.target = target;
    }

    @Override
    public String getMenuName() {
        return "Punish | " + target.getName();
    }

    @Override
    public int getRows() {
        return 4;
    }

    @Override
    public void createItems() {
        List<String> reasonList = punishmentManager.getPunishmentReasons();

        for(String reason : reasonList){
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName(CC.sendBlue(reason));
            List<String> lore = new ArrayList<>();
            lore.add(CC.sendWhite("Click to punish " + target.getName() + " for " + reason));
            meta.setLore(lore);
            item.setItemMeta(meta);
            addItem(item);
        }
    }

    @Override
    protected void onInventoryClickEvent(Player player, ClickType clickType, InventoryClickEvent inventoryClickEvent) {
        if (inventoryClickEvent.getCurrentItem() != null && inventoryClickEvent.getCurrentItem().getType() != Material.AIR) {
            ItemStack item = inventoryClickEvent.getCurrentItem();
            String reasonWithColor = item.getItemMeta().getDisplayName();

            String reason = ChatColor.stripColor(reasonWithColor);

            punishmentManager.punishPlayer(player, target, reason, false, false);
            player.closeInventory();
        }
    }
}
