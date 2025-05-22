package me.clearedspore.feature.punishment.menu.history.item;

import me.clearedspore.easyAPI.menu.Item;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.feature.punishment.menu.history.punishment.BanHistoryMenu;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class StatsItem extends Item {

    private final OfflinePlayer target;
    private final Player viewer;
    private final JavaPlugin plugin;
    private final PunishmentManager punishmentManager;

    public StatsItem(OfflinePlayer target, Player viewer, JavaPlugin plugin, PunishmentManager punishmentManager) {
        this.target = target;
        this.viewer = viewer;
        this.plugin = plugin;
        this.punishmentManager = punishmentManager;
    }

    @Override
    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.BLACK_WOOL);
        ItemMeta meta = item.getItemMeta();

        if(punishmentManager.getAllPunishments(target).size() > 1){
            item.setAmount(punishmentManager.getAllPunishments(target).size());
        }

        meta.setDisplayName(CC.sendBlue("Stats"));
        List<String> lore = new ArrayList<>();
        lore.add(CC.sendWhite("All punishments: &e" + punishmentManager.getAllPunishments(target).size()));
        lore.add(CC.sendWhite(""));
        lore.add(CC.sendWhite("Bans: &e" + punishmentManager.getAllBans(target).size()));
        lore.add(CC.sendWhite("Mutes: &e" + punishmentManager.getAllMutes(target).size()));
        lore.add(CC.sendWhite("Kicks: &e" + punishmentManager.getAllKicks(target).size()));
        lore.add(CC.sendWhite("Warns: &e" + punishmentManager.getAllWarns(target).size()));
        lore.add(CC.sendWhite(""));
        lore.add(CC.sendWhite("Active Bans: &e" + punishmentManager.getActiveBans(target).size()));
        lore.add(CC.sendWhite("Active Mutes: &e" + punishmentManager.getActiveMutes(target).size()));
        lore.add(CC.sendWhite("Active Warns: &e" + punishmentManager.getActiveWarns(target).size()));

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    @Override
    public void onClickEvent(Player player, ClickType clickType) {

    }
}
