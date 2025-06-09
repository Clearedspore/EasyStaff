package me.clearedspore.feature.punishment.menu.history.punishment;

import me.clearedspore.easyAPI.menu.PaginatedMenu;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.punishment.Punishment;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.feature.punishment.menu.history.item.*;
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

public class KickHistoryMenu extends PaginatedMenu {

    private final OfflinePlayer target;
    private final Player viewer;
    private final PunishmentManager punishmentManager;
    private final JavaPlugin plugin;

    public KickHistoryMenu(JavaPlugin plugin, OfflinePlayer target, Player viewer, PunishmentManager punishmentManager) {
        super(plugin);
        this.target = target;
        this.viewer = viewer;
        this.plugin = plugin;
        this.punishmentManager = punishmentManager;
    }



    @Override
    public String getMenuName() {
        return "Punishments | Kicks";
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
        setGlobalMenuItem(6, 1, new MenuItem(plugin, viewer, target, punishmentManager));
        setGlobalMenuItem(7, 1, new StatsItem(target, viewer, plugin , punishmentManager));
        List<Punishment> kicks = punishmentManager.getAllKicks(target);

        if (kicks.isEmpty()) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(CC.sendRed("No Kicks Found"));
            List<String> lore = new ArrayList<>();
            lore.add(CC.sendWhite("This player has no kicks."));
            meta.setLore(lore);
            item.setItemMeta(meta);
            addItem(item);
        } else {
            for (Punishment punishments : kicks) {
                ItemStack item = new ItemStack(Material.RED_WOOL);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(CC.sendBlue("Kick: " + punishments.ID()));
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
                    lore.add(CC.sendWhite("Punished on &e" + punishments.getFormattedCreationDate()));
                    
                    FileConfiguration playerConfig = punishmentManager.getPlayerData().getPlayerConfig(target);
                    if (playerConfig != null && playerConfig.contains("punishments.kicks." + punishments.ID() + ".offenseCount") && viewer.hasPermission(P.punish_staff)) {
                        int offenseCount = playerConfig.getInt("punishments.kicks." + punishments.ID() + ".offenseCount");
                        lore.add(CC.sendWhite("Offense: &e#" + (offenseCount)));
                    }
                } else if(isPunishmentHidden && !haspermission){
                    lore.add(CC.sendRed("This punishment is hidden"));
                    lore.add("");
                    lore.add(CC.sendWhite("Issuer: &e?"));
                    lore.add(CC.sendWhite("Reason: &e?"));
                    lore.add(CC.sendWhite("Punished on &e?"));
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
                
                if (displayName.startsWith(CC.sendBlue("Kick: "))) {
                    String punishmentId = displayName.substring(displayName.indexOf("Kick: ") + 6).trim();
                    boolean isNowHidden = punishmentManager.getHiddenPunishmentManager().togglePunishmentVisibility(target, punishmentId);
                    
                    if (isNowHidden) {
                        player.sendMessage(CC.sendGreen("Punishment " + punishmentId + " is now hidden."));
                    } else {
                        player.sendMessage(CC.sendGreen("Punishment " + punishmentId + " is now visible."));
                    }

                    new KickHistoryMenu(plugin, target, viewer, punishmentManager).open(player);
                }
            }
        }
    }
}