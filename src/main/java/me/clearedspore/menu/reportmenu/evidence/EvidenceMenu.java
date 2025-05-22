package me.clearedspore.menu.reportmenu.evidence;

import me.clearedspore.easyAPI.menu.Menu;
import me.clearedspore.menu.reportmenu.evidence.item.NoItem;
import me.clearedspore.menu.reportmenu.evidence.item.YesItem;
import me.clearedspore.feature.reports.ReportManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

public class EvidenceMenu extends Menu {

    private final String reason;
    private final JavaPlugin plugin;
    private final OfflinePlayer suspect;
    private final ReportManager reportManager;

    public EvidenceMenu(JavaPlugin plugin, String reason, OfflinePlayer suspect, ReportManager reportManager) {
        super(plugin);
        this.plugin = plugin;
        this.reason = reason;
        this.suspect = suspect;
        this.reportManager = reportManager;
    }

    @Override
    public String getMenuName() {
        return "Report | Want to provide evidence?";
    }

    @Override
    public int getRows() {
        return 3;
    }

    @Override
    public void setMenuItems() {
        setMenuItem(2, 2, new YesItem(reportManager, plugin, reason, suspect));
        setMenuItem(8, 2, new NoItem(reportManager, suspect, reason, plugin));
    }

    @Override
    public void createItems() {

    }
}
