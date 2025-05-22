package me.clearedspore.menu.reportmenu;

import me.clearedspore.easyAPI.menu.PaginatedMenu;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.menu.reportmenu.managereport.ManageReportMenu;
import me.clearedspore.feature.reports.Report;
import me.clearedspore.feature.reports.ReportManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ReportsMenu extends PaginatedMenu {

    private final ReportManager reportManager;
    private final JavaPlugin plugin;

    public ReportsMenu(JavaPlugin plugin, ReportManager reportManager) {
        super(plugin);
        this.plugin = plugin;
        this.reportManager = reportManager;
    }

    @Override
    public String getMenuName() {
        return "Reports | reports";
    }

    @Override
    public int getRows() {
        return 4;
    }
    @Override
    public void createItems() {
        List<Report> reports = reportManager.getAllReports();

        for (Report report : reports) {
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName(CC.sendBlue("Report: " + report.getReportId()));
            boolean hasEvidence = report.hasEvidence();
            meta.setEnchantmentGlintOverride(hasEvidence);
            List<String> lore = new ArrayList<>();
            lore.add(CC.sendWhite(""));
            lore.add(CC.sendWhite("Issuer: &e" + report.getIssuer().getName()));
            lore.add(CC.sendWhite("Suspect: &e" + report.getSuspect().getName()));
            lore.add(CC.sendWhite("Reason: &e" + report.getReason()));
            lore.add(CC.sendWhite("Created: &e" + report.getTimeSinceCreation()));
            if(hasEvidence){
                lore.add(CC.sendWhite("Evidence: &e" + report.getEvidence()));
            }
            lore.add(CC.sendWhite(""));
            lore.add(CC.sendWhite("Left click to manage"));
            lore.add(CC.sendWhite("Right click to view evidence"));


            meta.setLore(lore);
            item.setItemMeta(meta);

            addItem(item);
        }
    }

    @Override
    protected void onInventoryClickEvent(Player player, ClickType clickType, InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        ItemMeta itemMeta = clickedItem.getItemMeta();
        String displayName = itemMeta.getDisplayName();
        if (displayName.startsWith(CC.sendBlue("Report: "))) {
            String reportId = displayName.substring(CC.sendBlue("Report: ").length()).trim();

            if (clickType.isLeftClick()) {
                new ManageReportMenu(plugin, reportId, reportManager).open(player);
                return;
            }


            if (clickType.isRightClick()) {
                player.closeInventory();
                reportManager.showEvidence(player, reportId);
            }
        }
    }

}
