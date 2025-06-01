package me.clearedspore.feature.setting.settings;

import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.alertManager.Alert;
import me.clearedspore.feature.alertManager.AlertManager;
import me.clearedspore.feature.setting.Setting;
import me.clearedspore.feature.setting.SettingsManager;
import me.clearedspore.storage.PlayerData;
import me.clearedspore.util.P;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class StaffNotfySetting extends Setting {

    private final PlayerData playerData;
    private final AlertManager alertManager;

    public StaffNotfySetting(PlayerData playerData, AlertManager alertManager) {
        super("staff_notification", true, playerData);
        this.playerData = playerData;
        this.alertManager = alertManager;
    }

    @Override
    public ItemStack getMenuItem(Player player) {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(CC.sendBlue("Staff notifications"));

        List<String> lore = new ArrayList<>();
        lore.add(CC.send("&7Toggle receiving staff notifications"));
        lore.add("");

        lore.add(CC.send(isEnabled(player) ? "&aenabled &7 - You will receive staff notifications" : "&cdisabled &7 - You will not receive staff notifications"));

        lore.add("");
        lore.add(CC.sendBlue("Click to toggle"));

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;

    }

    @Override
    protected void onToggle(Player player, boolean newState) {
        if (newState) {
            player.sendMessage(CC.sendBlue("You will receive staff notification"));
            SettingsManager.setSettingEnabled(playerData, player, "staff_notification", true);
            alertManager.setAlertEnabled(player, Alert.STAFF, true);
        } else {
            SettingsManager.setSettingEnabled(playerData, player, "staff_notification", false);
            player.sendMessage(CC.sendBlue("You will not receive staff notifications"));
            alertManager.setAlertEnabled(player, Alert.STAFF, false);
        }
    }

    @Override
    public Boolean meetsRequirement(Player player) {
        return player.hasPermission(P.vanish);
    }
}
