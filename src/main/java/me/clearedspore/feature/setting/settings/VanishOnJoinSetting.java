package me.clearedspore.feature.setting.settings;

import me.clearedspore.easyAPI.util.CC;
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

public class VanishOnJoinSetting extends Setting {

    private final PlayerData playerData;

    public VanishOnJoinSetting(PlayerData playerData) {
        super("vanish_on_join", true, playerData);
        this.playerData = playerData;
    }

    @Override
    public ItemStack getMenuItem(Player player) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(CC.sendBlue("Vanish on Join"));

        List<String> lore = new ArrayList<>();
        lore.add(CC.send("&7Toggle enabling vanish on join"));
        lore.add("");

        lore.add(CC.send(isEnabled(player) ? "&aenabled &7 - You will be put in vanish on join" : "&cdisabled &7 - You will not be put in vanish on join"));

        lore.add("");
        lore.add(CC.sendBlue("Click to toggle"));

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;

    }

    @Override
    protected void onToggle(Player player, boolean newState) {
        if (newState) {
            player.sendMessage(CC.sendBlue("You will be put in vanish on join"));
            SettingsManager.setSettingEnabled(playerData, player, "vanish_on_join", true);
        } else {
            SettingsManager.setSettingEnabled(playerData, player, "vanish_on_join", false);
            player.sendMessage(CC.sendBlue("You will not be put in vanish on join"));
        }
    }

    @Override
    public Boolean meetsRequirement(Player player) {
        return player.hasPermission(P.vanish);
    }
}
