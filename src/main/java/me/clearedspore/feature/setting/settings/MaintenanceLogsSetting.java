package me.clearedspore.feature.setting.settings;

import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.setting.Setting;
import me.clearedspore.feature.setting.SettingsManager;
import me.clearedspore.storage.PlayerData;
import me.clearedspore.util.PS;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceLogsSetting extends Setting {

    private final PlayerData playerData;

    public MaintenanceLogsSetting(PlayerData playerData) {
        super("maintenance_logs", true, playerData);
        this.playerData = playerData;
    }

    @Override
    public ItemStack getMenuItem(Player player) {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(CC.sendBlue("Maintenance logs"));

        List<String> lore = new ArrayList<>();
        lore.add(CC.send("&7Toggle receiving maintenance logs"));
        lore.add("");

        lore.add(CC.send(isEnabled(player) ? "&aenabled &7 - You will receive maintenance logs" : "&cdisabled &7 - You will not receive maintenance logs"));

        lore.add("");
        lore.add(CC.sendBlue("Click to toggle"));

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;

    }

    @Override
    protected void onToggle(Player player, boolean newState) {
        if (newState) {
            player.sendMessage(CC.sendBlue("You will receive maintenance logs"));
            SettingsManager.setSettingEnabled(playerData, player, "maintenance_logs", true);
        } else {
            SettingsManager.setSettingEnabled(playerData, player, "maintenance_logs", false);
            player.sendMessage(CC.sendBlue("You will not receive maintenance logs"));
        }
    }

    @Override
    public Boolean meetsRequirement(Player player) {
        return player.hasPermission(PS.maintenance_logs);
    }
}
