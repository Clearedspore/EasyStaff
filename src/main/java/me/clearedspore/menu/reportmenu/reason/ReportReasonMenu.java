package me.clearedspore.menu.reportmenu.reason;

import me.clearedspore.easyAPI.menu.PaginatedMenu;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.reports.ReportManager;
import me.clearedspore.feature.reports.ReportStatus;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ReportReasonMenu extends PaginatedMenu {

    private final JavaPlugin plugin;
    private final String reportID;
    private final ReportManager reportManager;
    private final ReportStatus reportStatus;

    public ReportReasonMenu(JavaPlugin plugin, String reportID, ReportManager reportManager, ReportStatus reportStatus) {
        super(plugin);
        this.plugin = plugin;
        this.reportID = reportID;
        this.reportManager = reportManager;
        this.reportStatus = reportStatus;
    }

    @Override
    public String getMenuName() {
        return "Report | reason";
    }

    @Override
    public int getRows() {
        return 4;
    }

    @Override
    public void createItems() {
        List<String> reasonList = plugin.getConfig().getStringList("report.finished-reasons");

        for (String reason : reasonList) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName(CC.sendBlue(reason));
            List<String> lore = new ArrayList<>();
            lore.add(CC.sendWhite("Click to use " + reason + " as the report completion reason"));
            meta.setLore(lore);

            item.setItemMeta(meta);
            addItem(item);
        }
    }

    @Override
    protected void onInventoryClickEvent(Player player, ClickType clickType, InventoryClickEvent inventoryClickEvent) {
        if(inventoryClickEvent.getCurrentItem() != null && inventoryClickEvent.getCurrentItem().getType() != Material.AIR) {
        ItemStack item = inventoryClickEvent.getCurrentItem();
            String reason = item.getItemMeta().getDisplayName();

            player.closeInventory();
            reportManager.removeReport(reportID, player, reportStatus, reason);
        }
    }
}
