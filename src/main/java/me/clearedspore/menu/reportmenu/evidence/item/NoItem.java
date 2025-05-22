package me.clearedspore.menu.reportmenu.evidence.item;

import me.clearedspore.easyAPI.menu.Item;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.reports.ReportManager;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class NoItem extends Item {

    private final ReportManager reportManager;
    private final OfflinePlayer suspect;
    private final String reason;
    private final JavaPlugin plugin;

    public NoItem(ReportManager reportManager, OfflinePlayer suspect, String reason, JavaPlugin plugin) {
        this.reportManager = reportManager;
        this.suspect = suspect;
        this.reason = reason;
        this.plugin = plugin;
    }

    @Override
    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(CC.sendRed("No"));
        List<String> lore = new ArrayList<>();
        lore.add(CC.sendWhite("Click if you don't want to provide evidence (not recommended)"));

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    @Override
    public void onClickEvent(Player player, ClickType clickType) {
        reportManager.createReport(player, suspect, reason);
        player.closeInventory();
        player.sendMessage(CC.sendBlue("You have reported " + suspect.getName() + " for &f" + reason));
        player.sendMessage(CC.sendBlue("Thank you for helping us to keep the server safe!"));
    }
}
