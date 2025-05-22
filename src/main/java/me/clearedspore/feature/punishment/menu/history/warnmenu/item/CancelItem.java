package me.clearedspore.feature.punishment.menu.history.warnmenu.item;

import me.clearedspore.easyAPI.menu.Item;
import me.clearedspore.easyAPI.util.CC;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class CancelItem extends Item {


    private final JavaPlugin plugin;

    public CancelItem(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(CC.sendRed("Click to cancel"));
        List<String> lore = new ArrayList<>();
        lore.add(CC.sendWhite(""));
        lore.add(CC.sendWhite("Click to cancel"));

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    @Override
    public void onClickEvent(Player player, ClickType clickType) {
        player.closeInventory();
    }
}
