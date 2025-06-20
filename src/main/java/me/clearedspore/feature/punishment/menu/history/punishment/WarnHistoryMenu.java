package me.clearedspore.feature.punishment.menu.history.punishment;

import me.clearedspore.easyAPI.menu.PaginatedMenu;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.punishment.Punishment;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.feature.punishment.menu.history.item.*;
import me.clearedspore.feature.punishment.menu.history.warnmenu.RemoveWarnMenu;
import me.clearedspore.util.P;
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

public class WarnHistoryMenu extends PaginatedMenu {

    private final OfflinePlayer target;
    private final Player viewer;
    private final PunishmentManager punishmentManager;
    private final JavaPlugin plugin;

    public WarnHistoryMenu(JavaPlugin plugin, OfflinePlayer target, Player viewer, PunishmentManager punishmentManager) {
        super(plugin);
        this.target = target;
        this.viewer = viewer;
        this.plugin = plugin;
        this.punishmentManager = punishmentManager;
    }



    @Override
    public String getMenuName() {
        return "Punishments | Warns";
    }

    @Override
    public int getRows() {
        return 6;
    }

    @Override
    public void createItems() {
        setGlobalMenuItem(3, 1, new BanItem(target, viewer, plugin, punishmentManager));
        setGlobalMenuItem(4, 1, new MenuItem(plugin, viewer, target, punishmentManager));
        setGlobalMenuItem(5, 1, new MuteItem(target, viewer, plugin, punishmentManager));
        setGlobalMenuItem(6, 1, new KickItem(target, viewer, plugin, punishmentManager));
        setGlobalMenuItem(7, 1, new StatsItem(target, viewer, plugin , punishmentManager));
        List<Punishment> warns = punishmentManager.getAllWarns(target);

        if (warns.isEmpty()) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(CC.sendRed("No Warns Found"));
            List<String> lore = new ArrayList<>();
            lore.add(CC.sendWhite("This player has no warns."));
            meta.setLore(lore);
            item.setItemMeta(meta);
            addItem(item);
        } else {
            for (Punishment punishments : warns) {
                boolean active = punishments.active();
                ItemStack item = new ItemStack((active ? Material.LIME_WOOL : Material.RED_WOOL));
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(CC.sendBlue("Warn: " + punishments.ID()));
                List<String> lore = new ArrayList<>();
                lore.add("");
                
                boolean isPunishmentHidden = punishmentManager.getHiddenPunishmentManager().isPunishmentHidden(target, punishments.ID());
                boolean haspermission = viewer.hasPermission(P.punishments_hide);

                if (!isPunishmentHidden || haspermission) {
                    if (viewer.hasPermission(P.punish_staff)) {
                        String issuerName = (punishments.issuer() != null) ? punishments.issuer().getName() : "CONSOLE";
                        lore.add(CC.sendWhite("Issuer: &e" + issuerName));
                    }
                    lore.add(CC.sendWhite("Reason: &e" + punishments.reason()));
                    lore.add(CC.sendWhite("Active: &e" + punishments.active()));
                    lore.add(CC.sendWhite("Punished on &e" + punishments.getFormattedCreationDate()));
                    
                    FileConfiguration playerConfig = punishmentManager.getPlayerData().getPlayerConfig(target);
                    if (playerConfig != null && playerConfig.contains("punishments.warns." + punishments.ID() + ".offenseCount") && viewer.hasPermission(P.punish_staff)) {
                        int offenseCount = playerConfig.getInt("punishments.warns." + punishments.ID() + ".offenseCount");
                        lore.add(CC.sendWhite("Offense: &e#" + (offenseCount)));
                    }

                    if (active) {
                        lore.add("");
                        if (viewer.hasPermission(P.unwarn)) {
                            if (!target.equals(viewer)) {
                                lore.add(CC.sendGreen("» Click to remove warning " + punishments.ID()));
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
                            lore.add(CC.sendWhite("Removal Issuer: &e" + (punishments.removalIssuer() != null ? punishments.removalIssuer().getName() : "Unknown")));
                        }
                        lore.add(CC.sendWhite("Removal Reason: &e" + (punishments.removalReason() != null ? punishments.removalReason() : "Unknown")));
                    }
                } else if(isPunishmentHidden && !haspermission){
                    lore.add(CC.sendRed("This punishment is hidden"));
                    lore.add("");
                    lore.add(CC.sendWhite("Issuer: &e?"));
                    lore.add(CC.sendWhite("Reason: &e?"));
                    lore.add(CC.sendWhite("Active: &e" + active));
                    lore.add(CC.sendWhite("Punished on &e?"));
                    
                    if (active) {
                        lore.add("");
                        if (viewer.hasPermission(P.unwarn)) {
                            if (!target.equals(viewer)) {
                                lore.add(CC.sendWhite("Click to remove the warning"));
                            } else if (plugin.getConfig().getBoolean("punishments.remove-own") || viewer.hasPermission(P.removeown_bypass)) {
                                lore.add(CC.sendWhite("Click to remove your own warning"));
                            } else {
                                lore.add(CC.sendRed("You can't remove your own punishment!"));
                            }
                        }
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
        if (clickedItem != null && clickedItem.hasItemMeta()) {
            ItemMeta itemMeta = clickedItem.getItemMeta();
            String displayName = itemMeta.getDisplayName();

            if (displayName.startsWith(CC.sendBlue("Warn: "))) {
                String warnID = displayName.substring(displayName.indexOf("Warn: ") + 6).trim();

                if (clickType == ClickType.RIGHT && player.hasPermission(P.punishments_hide)) {
                    boolean isNowHidden = punishmentManager.getHiddenPunishmentManager().togglePunishmentVisibility(target, warnID);
                    
                    if (isNowHidden) {
                        player.sendMessage(CC.sendGreen("Punishment " + warnID + " is now hidden."));
                    } else {
                        player.sendMessage(CC.sendGreen("Punishment " + warnID + " is now visible."));
                    }

                    new WarnHistoryMenu(plugin, target, viewer, punishmentManager).open(player);
                    return;
                }

                if (clickType == ClickType.LEFT && player.hasPermission(P.unwarn)) {
                    if (punishmentManager.isWarnActive(target, warnID)) {
                        if (!plugin.getConfig().getBoolean("punishments.remove-own") && target.equals(viewer) && !viewer.hasPermission(P.removeown_bypass)) {
                            viewer.sendMessage(CC.sendRed("You cannot remove your own punishment!"));
                            return;
                        }
                        new RemoveWarnMenu(plugin, warnID, target, punishmentManager).open(player);
                    }
                }
            }
        }
    }
}