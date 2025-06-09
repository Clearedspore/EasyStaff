package me.clearedspore.feature.punishment.menu.history;

import me.clearedspore.easyAPI.menu.PaginatedMenu;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.punishment.Punishment;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.feature.punishment.PunishmentType;
import me.clearedspore.feature.punishment.menu.history.item.*;
import me.clearedspore.feature.punishment.menu.history.warnmenu.RemoveWarnMenu;
import me.clearedspore.util.P;
import me.clearedspore.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class HistoryMenu extends PaginatedMenu {

    private final OfflinePlayer target;
    private final Player viewer;
    private final PunishmentManager punishmentManager;
    private final JavaPlugin plugin;

    public HistoryMenu(JavaPlugin plugin, OfflinePlayer target, Player viewer, PunishmentManager punishmentManager) {
        super(plugin);
        this.target = target;
        this.viewer = viewer;
        this.plugin = plugin;
        this.punishmentManager = punishmentManager;
    }



    @Override
    public String getMenuName() {
        return "Punishments | " + target.getName();
    }

    @Override
    public int getRows() {
        return 6;
    }

    @Override
    public void createItems() {
        setGlobalMenuItem(3, 1, new BanItem(target, viewer, plugin, punishmentManager));
        setGlobalMenuItem(4, 1, new WarnItem(target, viewer, plugin, punishmentManager));
        setGlobalMenuItem(5, 1, new MuteItem(target, viewer, plugin, punishmentManager));
        setGlobalMenuItem(6, 1, new KickItem(target, viewer, plugin, punishmentManager));
        setGlobalMenuItem(7, 1, new StatsItem(target, viewer, plugin , punishmentManager));

        List<Punishment> punishments = punishmentManager.getAllPunishments(target);

        if (punishments.isEmpty()) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(CC.sendRed("No punishments found"));
            List<String> lore = new ArrayList<>();
            lore.add(CC.sendWhite("This player has no punishments."));
            meta.setLore(lore);
            item.setItemMeta(meta);
            addItem(item);
        } else {
            punishments.sort((p1, p2) -> {
                if (p1.active() && !p2.active()) {
                    return -1;
                } else if (!p1.active() && p2.active()) {
                    return 1;
                } else {
                    return Long.compare(p2.creationTime(), p1.creationTime());
                }
            });

            for (Punishment punishment : punishments) {
                boolean active = punishment.active();
                ItemStack item = new ItemStack((active ? Material.LIME_WOOL : Material.RED_WOOL));
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(CC.sendBlue(punishment.punishmentType() + ": " + punishment.ID()));
                List<String> lore = new ArrayList<>();
                lore.add("");

                boolean isPunishmentHidden = punishmentManager.getHiddenPunishmentManager().isPunishmentHidden(target, punishment.ID());
                boolean haspermission = viewer.hasPermission(P.punishments_hide);

                if (!isPunishmentHidden || haspermission) {
                    if (viewer.hasPermission(P.punish_staff)) {
                        String issuerName = (punishment.issuer() != null) ? punishment.issuer().getName() : "CONSOLE";
                        lore.add(CC.sendWhite("Issuer: &e" + issuerName));
                    }
                    lore.add(CC.sendWhite("Reason: &e" + punishment.reason()));
                    lore.add(CC.sendWhite("Active: &e" + active));
                    lore.add(CC.sendWhite("Punished on &e" + punishment.getFormattedCreationDate()));

                    FileConfiguration playerConfig = punishmentManager.getPlayerData().getPlayerConfig(target);
                    if (playerConfig != null && playerConfig.contains("punishments.punishments." + punishment.ID() + ".offenseCount") && viewer.hasPermission(P.punish_staff)) {
                        int offenseCount = playerConfig.getInt("punishments.punishments." + punishment.ID() + ".offenseCount");
                        lore.add(CC.sendWhite("Offense: &e#" + (offenseCount)));
                    }

                    if (active && punishment.isTemporary()) {
                        lore.add(CC.sendWhite("Expires in: &e" + TimeUtil.formatRemainingTime(punishment.getRemainingTime())));
                    }

                    if (active && punishment.punishmentType() == PunishmentType.WARN) {
                        lore.add("");
                        if (viewer.hasPermission(P.unwarn)) {
                            if (!target.equals(viewer)) {
                                lore.add(CC.sendGreen("» Click to remove warning " + punishment.ID()));
                            } else if (plugin.getConfig().getBoolean("punishments.remove-own") || viewer.hasPermission(P.removeown_bypass)) {
                                lore.add(CC.sendGreen("» Click to remove your own warning"));
                            } else {
                                lore.add(CC.sendRed("You can't remove your own punishment!"));
                            }
                        }
                    }

                    if (!active) {
                        lore.add(CC.sendWhite(""));
                        if (viewer.hasPermission(P.punish_staff)) {
                                lore.add(CC.sendWhite("Removal Issuer: &e" + (punishment.removalIssuer() != null ? punishment.removalIssuer().getName() : "Unknown")));
                        }
                            lore.add(CC.sendWhite("Removal Reason: &e" + (punishment.removalReason() != null ? punishment.removalReason() : "Unknown")));
                    }
                } else if(isPunishmentHidden && !haspermission){
                    lore.add(CC.sendRed("This punishment is hidden"));
                    lore.add("");
                    lore.add(CC.sendWhite("Issuer: &e?"));
                    lore.add(CC.sendWhite("Reason: &e?"));
                    lore.add(CC.sendWhite("Active: &e" + active));
                    lore.add(CC.sendWhite("Punished on &e?"));

                    if (active && punishment.isTemporary()) {
                        lore.add(CC.sendWhite("Expires in: &e?"));
                    }

                    if (!active) {
                        lore.add(CC.sendWhite(""));
                        lore.add(CC.sendWhite("Removal Issuer: &e?"));
                        lore.add(CC.sendWhite("Removal Reason: &e?"));
                    }
                }

                if (viewer.hasPermission(P.punishments_hide)) {
                    if (isPunishmentHidden) {
                        lore.add(CC.sendRed("» Right-click to show this punishment"));
                    } else {
                        lore.add(CC.sendGreen("» Right-click to hide this punishment"));
                    }
                }

                meta.setLore(lore);
                item.setItemMeta(meta);
                addItem(item);
            }
        }
    }

    @Override
    protected void onInventoryClickEvent(Player player, ClickType clickType, InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        String displayName = meta.getDisplayName();

        if (clickType == ClickType.RIGHT && player.hasPermission(P.punishments_hide)) {
            for (PunishmentType type : PunishmentType.values()) {
                String prefix = CC.sendBlue(type + ": ");
                if (displayName.startsWith(prefix)) {
                    String punishmentId = displayName.substring(displayName.indexOf(type + ": ") + (type + ": ").length()).trim();
                    boolean isNowHidden = punishmentManager.getHiddenPunishmentManager().togglePunishmentVisibility(target, punishmentId);

                    if (isNowHidden) {
                        player.sendMessage(CC.sendGreen("Punishment " + punishmentId + " is now hidden."));
                    } else {
                        player.sendMessage(CC.sendGreen("Punishment " + punishmentId + " is now visible."));
                    }

                    new HistoryMenu(plugin, target, viewer, punishmentManager).open(player);
                    return;
                }
            }
        } else if (clickType == ClickType.LEFT && player.hasPermission(P.unwarn)) {
            if (displayName.startsWith(CC.sendBlue("WARN: "))) {
                String warnId = displayName.substring(displayName.indexOf("WARN: ") + "WARN: ".length()).trim();

                if (punishmentManager.isWarnActive(target, warnId)) {
                    if (!plugin.getConfig().getBoolean("punishments.remove-own") && target.equals(player) && !player.hasPermission(P.removeown_bypass)) {
                        player.sendMessage(CC.sendRed("You cannot remove your own punishment!"));
                        return;
                    }

                    new RemoveWarnMenu(plugin, warnId, target, punishmentManager).open(player);
                }
            }
        }
    }
}
