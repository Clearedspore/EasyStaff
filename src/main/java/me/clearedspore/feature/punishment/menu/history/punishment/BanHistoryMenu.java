package me.clearedspore.feature.punishment.menu.history.punishment;

import me.clearedspore.easyAPI.menu.PaginatedMenu;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.punishment.Punishment;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.feature.punishment.menu.history.item.*;
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

public class BanHistoryMenu extends PaginatedMenu {

    private final OfflinePlayer target;
    private final Player viewer;
    private final PunishmentManager punishmentManager;
    private final JavaPlugin plugin;

    public BanHistoryMenu(JavaPlugin plugin, OfflinePlayer target, Player viewer, PunishmentManager punishmentManager) {
        super(plugin);
        this.target = target;
        this.viewer = viewer;
        this.plugin = plugin;
        this.punishmentManager = punishmentManager;
    }



    @Override
    public String getMenuName() {
        return "Punishments | Bans";
    }

    @Override
    public int getRows() {
        return 6;
    }

    @Override
    public void createItems() {
        setGlobalMenuItem(3, 1, new MenuItem(plugin, viewer, target, punishmentManager));
        setGlobalMenuItem(4, 1, new WarnItem(target, viewer, plugin, punishmentManager));
        setGlobalMenuItem(5, 1, new MuteItem(target, viewer, plugin, punishmentManager));
        setGlobalMenuItem(6, 1, new KickItem(target, viewer, plugin, punishmentManager));
        setGlobalMenuItem(7, 1, new StatsItem(target, viewer, plugin , punishmentManager));
        List<Punishment> bans = punishmentManager.getAllBans(target);

        if (bans.isEmpty()) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(CC.sendRed("No Bans Found"));
            List<String> lore = new ArrayList<>();
            lore.add(CC.sendWhite("This player has no bans."));
            meta.setLore(lore);
            item.setItemMeta(meta);
            addItem(item);
        } else {
            for (Punishment punishments : bans) {
                boolean active = punishments.active();
                ItemStack item = new ItemStack((active ? Material.LIME_WOOL : Material.RED_WOOL));
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(CC.sendBlue("Ban: " + punishments.ID()));
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
                    lore.add(CC.sendWhite("Active: &e" + active));
                    lore.add(CC.sendWhite("Punished on &e" + punishments.getFormattedCreationDate()));

                    FileConfiguration playerConfig = punishmentManager.getPlayerData().getPlayerConfig(target);
                    if (playerConfig != null && playerConfig.contains("punishments.bans." + punishments.ID() + ".offenseCount") && viewer.hasPermission(P.punish_staff)) {
                        int offenseCount = playerConfig.getInt("punishments.bans." + punishments.ID() + ".offenseCount");
                        lore.add(CC.sendWhite("Offense: &e#" + (offenseCount)));
                    }

                    if (active && punishments.isTemporary()) {
                        lore.add(CC.sendWhite("Expires in: &e" + TimeUtil.formatRemainingTime(punishments.getRemainingTime())));
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
                    
                    if (active && punishments.isTemporary()) {
                        lore.add(CC.sendWhite("Expires in: &e?"));
                    }
                    
                    if (!active) {
                        lore.add(CC.sendWhite(""));
                        lore.add(CC.sendWhite("Removal Issuer: &e?"));
                        lore.add(CC.sendWhite("Removal Reason: &e?"));
                    }
                }

                if (viewer.hasPermission(P.punishments_hide)) {
                    lore.add("");
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
        if (!player.hasPermission(P.punishments_hide)) {
            return;
        }
        
        if (clickType == ClickType.RIGHT) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.hasItemMeta()) {
                ItemMeta meta = clickedItem.getItemMeta();
                String displayName = meta.getDisplayName();
                
                if (displayName.startsWith(CC.sendBlue("Ban: "))) {
                    String punishmentId = displayName.substring(displayName.indexOf("Ban: ") + 5).trim();
                    boolean isNowHidden = punishmentManager.getHiddenPunishmentManager().togglePunishmentVisibility(target, punishmentId);
                    
                    if (isNowHidden) {
                        player.sendMessage(CC.sendGreen("Punishment " + punishmentId + " is now hidden."));
                    } else {
                        player.sendMessage(CC.sendGreen("Punishment " + punishmentId + " is now visible."));
                    }

                    new BanHistoryMenu(plugin, target, viewer, punishmentManager).open(player);
                }
            }
        }
    }
}
