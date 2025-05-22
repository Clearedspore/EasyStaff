package me.clearedspore.menu.reportmenu.reportplayer;

import me.clearedspore.easyAPI.menu.PaginatedMenu;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.menu.reportmenu.evidence.EvidenceMenu;
import me.clearedspore.menu.reportmenu.reportplayer.item.CustomReasonItem;
import me.clearedspore.feature.reports.ReportManager;
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

public class ReportPlayerMenu extends PaginatedMenu {

    private final OfflinePlayer target;
    private final ReportManager reportManager;
    private final JavaPlugin plugin;

    public ReportPlayerMenu(JavaPlugin plugin, OfflinePlayer target, ReportManager reportManager) {
        super(plugin);
        this.plugin = plugin;
        this.target = target;
        this.reportManager = reportManager;
    }

    @Override
    public String getMenuName() {
        return "Report | Report a player ";
    }

    @Override
    public int getRows() {
        return 4;
    }

    @Override
    public void createItems() {
        List<String> reportReasonList = plugin.getConfig().getStringList("report.reasons");

        if(!plugin.getConfig().getBoolean("report.default-reason")) {
            setGlobalMenuItem(5, 1, new CustomReasonItem(plugin, target, reportManager));
        }

        for(String reasons : reportReasonList){
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName(CC.sendBlue(reasons));
            List<String> lore = new ArrayList<>();
            lore.add(CC.sendWhite(""));
            lore.add(CC.sendWhite("Click to report " + target.getName() + " for " + reasons));
            meta.setLore(lore);
            item.setItemMeta(meta);
            addItem(item);
        }
    }

    @Override
    protected void onInventoryClickEvent(Player player, ClickType clickType, InventoryClickEvent inventoryClickEvent) {
        ItemStack item = inventoryClickEvent.getCurrentItem();
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String reason = item.getItemMeta().getDisplayName();
            new EvidenceMenu(plugin, reason, target, reportManager).open(player);
        }
    }
}
