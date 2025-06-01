package me.clearedspore.command.report;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.menu.reportmenu.ReportsMenu;
import me.clearedspore.menu.reportmenu.managereport.ManageReportMenu;
import me.clearedspore.menu.reportmenu.reason.ReportReasonMenu;
import me.clearedspore.feature.reports.ReportManager;
import me.clearedspore.feature.reports.ReportStatus;
import me.clearedspore.util.P;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@CommandAlias("reports|getreports")
@CommandPermission(P.reports)
public class ReportListCommand extends BaseCommand {

    private final ReportManager reportManager;
    private final JavaPlugin plugin;

    public ReportListCommand(ReportManager reportManager, JavaPlugin plugin) {
        this.reportManager = reportManager;
        this.plugin = plugin;
    }

    @Default
    public void onReportList(Player player, @Optional String arg1, @Optional String arg2) {
        if (arg1 == null || arg1.isEmpty()) {
            new ReportsMenu(plugin, reportManager).open(player);
            return;
        }

        if (arg1.equalsIgnoreCase("accept")) {
            if (arg2 == null || arg2.isEmpty()) {
                player.sendMessage(CC.sendRed("Please provide a valid report ID."));
                return;
            }
            new ReportReasonMenu(plugin, arg2, reportManager, ReportStatus.ACCEPTED).open(player);
        } else if (arg1.equalsIgnoreCase("deny")) {
            if (arg2 == null || arg2.isEmpty()) {
                player.sendMessage(CC.sendRed("Please provide a valid report ID."));
                return;
            }
            new ReportReasonMenu(plugin, arg2, reportManager, ReportStatus.DENIED).open(player);
        } else if (arg1.equalsIgnoreCase("evidence")) {
            if (arg2 == null || arg2.isEmpty()) {
                player.sendMessage(CC.sendRed("Please provide a valid report ID."));
                return;
            }
            reportManager.showEvidence(player, arg2);
        } else {
            new ManageReportMenu(plugin, arg1, reportManager).open(player);
        }
    }
}
