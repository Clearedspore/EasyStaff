package me.clearedspore.feature.reports;

import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.easyAPI.util.Logger;
import me.clearedspore.easyAPI.util.StringUtil;
import me.clearedspore.feature.alertManager.Alert;
import me.clearedspore.feature.alertManager.AlertManager;
import me.clearedspore.feature.discord.DiscordManager;
import me.clearedspore.util.P;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;

public class ReportManager {

    private final Map<String,  Report> reports = new HashMap<>();
    private final Logger logger;
    private final JavaPlugin plugin;
    private final File reportFile;
    private final FileConfiguration reportConfig;
    private DiscordManager discordManager;
    private final AlertManager alertManager;

    public ReportManager(Logger logger, JavaPlugin plugin, AlertManager alertManager) {
        this.logger = logger;
        this.plugin = plugin;
        this.reportFile = new File(plugin.getDataFolder(), "reports.yml");
        this.alertManager = alertManager;
        this.reportConfig = YamlConfiguration.loadConfiguration(reportFile);

        createFile(reportFile);
        loadReports();
        scheduleReportCleanup();

        if (plugin instanceof me.clearedspore.EasyStaff) {
            this.discordManager = ((me.clearedspore.EasyStaff) plugin).getDiscordManager();
        }
    }

    private String generateShortId(int length) {
        String characters = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder id = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            id.append(characters.charAt(random.nextInt(characters.length())));
        }
        return id.toString();
    }

    public void createFile(File file) {
        try {
            if (file.createNewFile()) {
                logger.info("File created: " + file.getName());
            } else {
                logger.info("File already exists.");
            }
        } catch (IOException e) {
            logger.error("An error occurred while creating the file.");
            e.printStackTrace();
        }
    }

    public void saveReports() {
        try {
            reportConfig.save(reportFile);
            logger.info("Reports saved to file: " + reportFile.getName());
        } catch (IOException e) {
            logger.error("An error occurred while saving reports.");
            e.printStackTrace();
        }
    }

    public void loadReports() {
        if (!reportConfig.contains("reports")) {
            return;
        }

        for (String reportId : reportConfig.getConfigurationSection("reports").getKeys(false)) {
            String issuerName = reportConfig.getString("reports." + reportId + ".issuer", "Unknown");
            String suspectName = reportConfig.getString("reports." + reportId + ".suspect", "Unknown");
            String reason = reportConfig.getString("reports." + reportId + ".reason", "none");
            long creationTime = reportConfig.getLong("reports." + reportId + ".creationtime", 0);
            String evidence = reportConfig.getString("reports." + reportId + ".evidence", "none");

            Player issuer = Bukkit.getPlayer(issuerName);
            OfflinePlayer suspect = Bukkit.getOfflinePlayer(suspectName);

            Report report = new Report(reportId, issuer, reason, suspect, creationTime, evidence);
            reports.put(reportId, report);
        }
        logger.info("Reports loaded from file: " + reportFile.getName());
    }

    public List<Report> getAllReports() {
        return new ArrayList<>(reports.values());
    }

    public void showEvidence(Player player, String reportID){
        if (reports.containsKey(reportID)) {
            Report report = getReportById(reportID);

            if(!report.hasEvidence()){
                player.sendMessage(CC.sendRed("There is no evidence in this report!"));
                return;
            }


            TextColor blueColor = TextColor.fromHexString("#00CCDE");
            Component message = Component.text("Evidence: \n")
                    .color(blueColor)
                    .append(Component.text("[Accept] ")
                            .color(NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/reports accept " + reportID))
                            .hoverEvent(HoverEvent.showText(Component.text(CC.sendBlue("Click to accept the report")))))
                    .append(Component.text("[Deny] ")
                            .color(NamedTextColor.RED)
                            .clickEvent(ClickEvent.runCommand("/reports deny " + reportID))
                            .hoverEvent(HoverEvent.showText(Component.text(CC.sendBlue("Click to deny the report")))));
            player.sendMessage(CC.sendWhite("-------------------------------------------"));
            player.sendMessage(message);
            player.sendMessage(CC.sendBlue(report.getEvidence()));
            player.sendMessage(CC.sendWhite("-------------------------------------------"));

        }
    }

    public void createReport(Player player, OfflinePlayer suspect, String reason){

        String formattedReason = ChatColor.stripColor(reason);

        String reportID = generateShortId(5);
        long creationTime = System.currentTimeMillis();
        Report report = new Report(
                reportID,
                player,
                formattedReason,
                suspect,
                creationTime,
                "none"
        );

        reports.put(reportID, report);

        reportConfig.set("reports." + reportID + ".issuer", player.getName());
        reportConfig.set("reports." + reportID + ".suspect", suspect.getName());
        reportConfig.set("reports." + reportID + ".reason", formattedReason);
        reportConfig.set("reports." + reportID + ".creationtime", creationTime);
        reportConfig.set("reports." + reportID + ".evidence", "none");

        saveReports();

        if (discordManager != null && discordManager.isEnabled()) {
            discordManager.sendReportNotification(report);
        }

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(P.reports_notify) && alertManager.hasAlertEnabled(player, Alert.STAFF)) {
                TextColor blueColor = TextColor.fromHexString("#00CCDE");
                Component message = Component.text(CC.sendBlue(""))
                        .color(blueColor)
                        .append(Component.text("[Accept] ")
                                .color(NamedTextColor.GREEN)
                                .clickEvent(ClickEvent.runCommand("/reports accept " + reportID))
                                .hoverEvent(HoverEvent.showText(Component.text("Click to accept the report"))))
                        .append(Component.text("[Deny] ")
                                .color(NamedTextColor.RED)
                                .clickEvent(ClickEvent.runCommand("/reports deny " + reportID))
                                .hoverEvent(HoverEvent.showText(Component.text("Click to deny the report"))))
                        .append(Component.text("[Teleport] ")
                                .color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.runCommand("/tp " + suspect.getName()))
                                .hoverEvent(HoverEvent.showText(Component.text("Click to teleport to " + suspect.getName()))))
                        .append(Component.text("[Manage]")
                                .color(NamedTextColor.YELLOW)
                                .clickEvent(ClickEvent.runCommand("/reports " + reportID))
                                .hoverEvent(HoverEvent.showText(Component.text("Click to manage the report"))));
                staff.sendMessage(CC.sendBlue("[Staff] " + player.getName() + " has reported &f" + suspect.getName() + " &#00CCDEfor &f" + reason));
                staff.sendMessage(message);
            }
        }
    }

    public void createReport(Player player, OfflinePlayer suspect, String reason,  String evidence){

        String formattedReason = ChatColor.stripColor(reason);

        String reportID = generateShortId(5);
        long creationTime = System.currentTimeMillis();
        Report report = new Report(
                reportID,
                player,
                formattedReason,
                suspect,
                creationTime,
                evidence
        );

        reports.put(reportID, report);

        reportConfig.set("reports." + reportID + ".issuer", player.getName());
        reportConfig.set("reports." + reportID + ".suspect", suspect.getName());
        reportConfig.set("reports." + reportID + ".reason", formattedReason);
        reportConfig.set("reports." + reportID + ".creationtime", creationTime);
        reportConfig.set("reports." + reportID + ".evidence", evidence);

        saveReports();

        if (discordManager != null && discordManager.isEnabled()) {
            discordManager.sendReportNotification(report);
        }

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(P.reports_notify) && alertManager.hasAlertEnabled(player, Alert.STAFF)) {
                TextColor blueColor = TextColor.fromHexString("#00CCDE");
                Component message = Component.text("")
                        .color(blueColor)
                        .append(Component.text("[Accept] ")
                                .color(NamedTextColor.GREEN)
                                .clickEvent(ClickEvent.runCommand("/reports accept " + reportID))
                                .hoverEvent(HoverEvent.showText(Component.text("Click to accept the report"))))
                        .append(Component.text("[Deny] ")
                                .color(NamedTextColor.RED)
                                .clickEvent(ClickEvent.runCommand("/reports deny " + reportID))
                                .hoverEvent(HoverEvent.showText(Component.text("Click to deny the report"))))
                        .append(Component.text("[Teleport] ")
                                .color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.runCommand("/tp " + suspect.getName()))
                                .hoverEvent(HoverEvent.showText(Component.text("Click to teleport to " + suspect.getName()))))
                        .append(Component.text("[Manage] ")
                                .color(NamedTextColor.YELLOW)
                                .clickEvent(ClickEvent.runCommand("/reports " + reportID))
                                .hoverEvent(HoverEvent.showText(Component.text("Click to manage the report"))))
                        .append(Component.text("[Evidence]")
                                .color(NamedTextColor.BLUE)
                                .clickEvent(ClickEvent.runCommand("/reports evidence " + reportID))
                                .hoverEvent(HoverEvent.showText(Component.text("Click to view the evidence of this report"))));
                staff.sendMessage(CC.sendBlue("[Staff] " + player.getName() + " has reported &f" + suspect.getName() + " &#00CCDEfor &f" + reason));
                staff.sendMessage(message);
            }
        }
    }

    public void removeReport(String reportID, Player player, ReportStatus status, String reason){
        String formattedStatus = StringUtil.capitalizeFirstLetter(status.toString());
        if (reports.containsKey(reportID)) {
            notifyReporter(reportID, reason);
            executeCommands(reportID, player, status, reason);
            Report report = getReportById(reportID);
            OfflinePlayer suspect = report.getSuspect();
            reports.remove(reportID);
            reportConfig.set("reports." + reportID, null);
            saveReports();

            if (discordManager != null && discordManager.isEnabled() && status == ReportStatus.ACCEPTED) {
                discordManager.sendReportHandledNotification(report, player.getName(), reason, true);
            } else if(discordManager != null && discordManager.isEnabled() && status == ReportStatus.DENIED){
                discordManager.sendReportHandledNotification(report, player.getName(), reason, false);
            }
            
            player.sendMessage(CC.sendBlue("You have " + formattedStatus + " the report against &f" + suspect.getName()));
            for (Player players : Bukkit.getOnlinePlayers()){
                if(players.hasPermission(P.reports_notify) && alertManager.hasAlertEnabled(player, Alert.STAFF)){
                    players.sendMessage(CC.send("&f" + player.getName() + " &#00CCDEhas " + formattedStatus + " the report against &f" + suspect.getName()));
                }
            }
            logger.info("Report with ID " + reportID + " has been " + formattedStatus + " by " + player.getName() + ".");

        } else {
            player.sendMessage(CC.sendRed("Report with ID " + reportID + " does not exist."));
        }
    }

    public void executeCommands(String reportID, Player staff, ReportStatus status, String finishedReason) {
        Report report = getReportById(reportID);
        OfflinePlayer suspect = report.getSuspect();
        Player issuer = report.getIssuer();

        List<String> commands;
        if (status == ReportStatus.ACCEPTED) {
            commands = plugin.getConfig().getStringList("report.accepted-commands");
        } else if (status == ReportStatus.DENIED) {
            commands = plugin.getConfig().getStringList("report.denied-commands");
        } else {
            return;
        }

        for (String command : commands) {
            command = command.replace("%suspect%", suspect.getName())
                    .replace("%issuer%", issuer.getName())
                    .replace("%Staff%", staff.getName());

            if (command.startsWith("%player%/")) {
                String playerCommand = command.substring("%player%/".length());
                staff.performCommand(playerCommand);
            } else if (command.startsWith("%console%/")) {
                String consoleCommand = command.substring("%console%/".length());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), consoleCommand);
            }
        }
    }

    public Report getReportById(String reportID) {
        return reports.get(reportID);
    }


    public void notifyReporter(String reportID, String reason) {
        Report report = getReportById(reportID);

        Player player = report.getIssuer();
        List<String> messageList = plugin.getConfig().getStringList("report.player-message");

        if (player.isOnline()) {
            if (messageList.isEmpty()) {
                player.sendMessage(CC.sendBlue("Your report has been handled."));
                player.sendMessage(CC.sendBlue("Thank you for making a report and keeping the server safe!"));

                Component reasonMessage = Component.text("Reason: ")
                        .color(NamedTextColor.BLUE)
                        .append(Component.text(reason).color(NamedTextColor.WHITE));

                player.sendMessage(reasonMessage);
            } else {
                for (String message : messageList) {
                    message = message.replace("%reason%", reason);
                    player.sendMessage(CC.send(message));
                }
            }
        }
    }

    public void removeOldReports() {
        long currentTime = System.currentTimeMillis();
        long oneDayInMillis = 24 * 60 * 60 * 1000;

        Iterator<Map.Entry<String, Report>> iterator = reports.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Report> entry = iterator.next();
            Report report = entry.getValue();
            if (currentTime - report.getCreationTime() > oneDayInMillis) {
                iterator.remove();
                reportConfig.set("reports." + report.getReportId(), null);
                logger.info("Removed report with ID " + report.getReportId() + " due to expiration.");
            }
        }
        saveReports();
    }

    public void scheduleReportCleanup() {
        new BukkitRunnable() {
            @Override
            public void run() {
                removeOldReports();
            }
        }.runTaskTimer(plugin, 0, 20 * 60 * 60);
    }

}
