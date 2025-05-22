package me.clearedspore.feature.punishment.menu.history.warnmenu;

import me.clearedspore.easyAPI.menu.Menu;
import me.clearedspore.feature.punishment.PunishmentManager;
import me.clearedspore.feature.punishment.menu.history.warnmenu.item.CancelItem;
import me.clearedspore.feature.punishment.menu.history.warnmenu.item.ConfirmItem;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

public class RemoveWarnMenu extends Menu {

    private final String warnID;
    private final OfflinePlayer target;
    private final PunishmentManager punishmentManager;
    private final JavaPlugin plugin;

    public RemoveWarnMenu(JavaPlugin plugin, String warnID, OfflinePlayer target, PunishmentManager punishmentManager) {
        super(plugin);
        this.warnID = warnID;
        this.target = target;
        this.punishmentManager = punishmentManager;
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "Punishments | remove warn";
    }

    @Override
    public int getRows() {
        return 3;
    }

    @Override
    public void setMenuItems() {
        setMenuItem(2, 2, new ConfirmItem(warnID, target, punishmentManager, plugin));


        setMenuItem(8, 2, new CancelItem(plugin));
    }

    @Override
    public void createItems() {

    }
}
