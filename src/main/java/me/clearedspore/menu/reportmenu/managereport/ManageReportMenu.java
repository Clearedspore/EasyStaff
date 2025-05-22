package me.clearedspore.menu.reportmenu.managereport;

import me.clearedspore.easyAPI.menu.Menu;
import me.clearedspore.menu.reportmenu.managereport.item.AcceptItem;
import me.clearedspore.menu.reportmenu.managereport.item.DenyItem;
import me.clearedspore.feature.reports.ReportManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ManageReportMenu extends Menu {

    private final String reportID;
    private final JavaPlugin plugin;
    private final ReportManager reportManager;

    public ManageReportMenu(JavaPlugin plugin, String reportID, ReportManager reportManager) {
        super(plugin);
        this.reportID = reportID;
        this.plugin = plugin;
        this.reportManager = reportManager;
    }

    @Override
    public String getMenuName() {
        return "Reports | manage report";
    }

    @Override
    public int getRows() {
        return 3;
    }

    @Override
    public void setMenuItems() {
        setMenuItem(2,2, new AcceptItem(reportID, reportManager, plugin));
        setMenuItem(8,2, new DenyItem(reportManager, reportID, plugin));
    }

    @Override
    public void createItems() {

    }
}
