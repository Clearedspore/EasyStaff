package me.clearedspore.menu.reportmenu.managereport.item;

import me.clearedspore.easyAPI.menu.Item;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.menu.reportmenu.reason.ReportReasonMenu;
import me.clearedspore.feature.reports.Report;
import me.clearedspore.feature.reports.ReportManager;
import me.clearedspore.feature.reports.ReportStatus;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class DenyItem extends Item {

    private final ReportManager reportManager;
    private final String reportID;
    private final JavaPlugin plugin;

    public DenyItem(ReportManager reportManager, String reportID, JavaPlugin plugin) {
        this.reportManager = reportManager;
        this.reportID = reportID;
        this.plugin = plugin;
    }

    @Override
    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(CC.sendRed("Deny"));
        List<String> lore = new ArrayList<>();
        lore.add(CC.sendWhite(""));
        lore.add(CC.sendWhite("Click to deny " + reportID));
        lore.add(CC.sendWhite(""));
        Report report = reportManager.getReportById(reportID);
        lore.add(CC.sendWhite("Info:"));
        lore.add(CC.sendWhite("Reason: &e" + report.getReason()));
        lore.add(CC.sendWhite("Issuer: &e" + report.getIssuer().getName()));
        lore.add(CC.sendWhite("Suspect: &e" + report.getSuspect().getName()));


        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    @Override
    public void onClickEvent(Player player, ClickType clickType) {
        new ReportReasonMenu(plugin, reportID, reportManager, ReportStatus.DENIED).open(player);
    }
}
