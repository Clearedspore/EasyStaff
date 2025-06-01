package me.clearedspore.command.report;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.menu.reportmenu.evidence.EvidenceMenu;
import me.clearedspore.menu.reportmenu.reportplayer.ReportPlayerMenu;
import me.clearedspore.feature.reports.ReportManager;
import me.clearedspore.util.P;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@CommandPermission(P.report_player)
@CommandAlias("report")
public class ReportCommand extends BaseCommand {

    private final JavaPlugin plugin;
    private final ReportManager reportManager;

    public ReportCommand(JavaPlugin plugin, ReportManager reportManager) {
        this.plugin = plugin;
        this.reportManager = reportManager;
    }

    @Default
    @CommandCompletion("@players @reportReasons")
    @Syntax("<player> <reason>")
    private void onReportPlayer(Player player, String targetName, @Optional String reasonParts) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        String reason = (reasonParts != null) ? reasonParts.trim() : "";

        if (reason.isEmpty()) {
            new ReportPlayerMenu(plugin, target, reportManager).open(player);
        } else {
            List<String> reportReasonList = plugin.getConfig().getStringList("report.reasons");
            if (plugin.getConfig().getBoolean("report.default-reason")) {
                if (!reportReasonList.contains(reason)) {
                    player.sendMessage(CC.sendRed("You must provide a reason from the list!"));
                    return;
                }
            }

            new EvidenceMenu(plugin, reason, target, reportManager).open(player);
        }
    }
}
