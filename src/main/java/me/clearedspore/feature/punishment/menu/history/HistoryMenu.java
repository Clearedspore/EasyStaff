package me.clearedspore.feature.punishment.menu.history;

import me.clearedspore.easyAPI.menu.Menu;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.feature.punishment.menu.history.item.*;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class HistoryMenu extends Menu {

    private final OfflinePlayer target;
    private final Player viewer;
    private final JavaPlugin plugin;
    private final PunishmentManager punishmentManager;

    public HistoryMenu(JavaPlugin plugin, OfflinePlayer target, Player viewer, PunishmentManager punishmentManager) {
        super(plugin);
        this.target = target;
        this.plugin = plugin;
        this.viewer = viewer;
        this.punishmentManager = punishmentManager;
    }



    @Override
    public String getMenuName() {
        return "Punishments | " + target.getName();
    }

    @Override
    public int getRows() {
        return 3;
    }

    @Override
    public void setMenuItems() {
        setMenuItem(3, 2, new BanItem(target, viewer, plugin, punishmentManager));
        setMenuItem(4, 2, new WarnItem(target, viewer, plugin, punishmentManager));
        setMenuItem(5, 2, new MuteItem(target, viewer, plugin, punishmentManager));
        setMenuItem(6, 2, new KickItem(target, viewer, plugin, punishmentManager));
        setMenuItem(7, 2, new StatsItem(target, viewer, plugin , punishmentManager));
    }

    @Override
    public void createItems() {

    }
}
