package me.clearedspore.feature.punishment.menu.history.item;

import me.clearedspore.easyAPI.menu.Item;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.feature.punishment.menu.history.punishment.KickHistoryMenu;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class KickItem extends Item {

    private final OfflinePlayer target;
    private final Player viewer;
    private final JavaPlugin plugin;
    private final PunishmentManager punishmentManager;

    public KickItem(OfflinePlayer target, Player viewer, JavaPlugin plugin, PunishmentManager punishmentManager) {
        this.target = target;
        this.viewer = viewer;
        this.plugin = plugin;
        this.punishmentManager = punishmentManager;
    }

    @Override
    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.YELLOW_WOOL);
        ItemMeta meta = item.getItemMeta();

        if(punishmentManager.getAllKicks(target).size() > 1){
            item.setAmount(punishmentManager.getAllKicks(target).size());
        }

        meta.setDisplayName(CC.sendBlue("Kicks"));
        List<String> lore = new ArrayList<>();
        lore.add(CC.send("&f-----------------"));
        lore.add(CC.sendWhite("Click to check " + target.getName() + "'s kick history"));
        lore.add(CC.send("&f-----------------"));

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    @Override
    public void onClickEvent(Player player, ClickType clickType) {
        new KickHistoryMenu(plugin, target, viewer, punishmentManager).open(player);
    }
}
