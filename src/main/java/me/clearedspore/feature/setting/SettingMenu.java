package me.clearedspore.feature.setting;

import me.clearedspore.easyAPI.menu.PaginatedMenu;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu for displaying and managing all available settings
 */
public class SettingMenu extends PaginatedMenu {

    private final List<Setting> availableSettings;
    private final Player player;

    public SettingMenu(JavaPlugin plugin, Player player, List<Setting> settings) {
        super(plugin);
        this.player = player;
        this.availableSettings = new ArrayList<>();
        for (Setting setting : settings) {
            Boolean meetsRequirement = setting.meetsRequirement(player);
            if (meetsRequirement == null || meetsRequirement) {
                availableSettings.add(setting);
            }
        }
    }

    @Override
    public String getMenuName() {
        return ChatColor.GOLD + "Settings";
    }

    @Override
    public int getRows() {
        return 4;
    }

    @Override
    public void createItems() {
        for (Setting setting : availableSettings) {
            addItem(setting.getMenuItem(player));
        }
    }

    @Override
    protected void onInventoryClickEvent(Player player, ClickType clickType, InventoryClickEvent event) {

        int slot = event.getSlot();
        int start = getCurrentPage() * getItemsPerPage();

        if (slot > 8) {
            int adjustedSlot = slot - 9;
            int itemIndex = start + adjustedSlot;

            if (itemIndex >= 0 && itemIndex < availableSettings.size()) {
                Setting setting = availableSettings.get(itemIndex);
                setting.toggle((Player) event.getWhoClicked());
                reloadItems();
            }
        }
    }
}
