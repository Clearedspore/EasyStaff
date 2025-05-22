package me.clearedspore.feature.setting;

import me.clearedspore.storage.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public abstract class Setting {
    
    private final String name;
    private final boolean defaultEnabled;
    private final PlayerData playerData;

    public Setting(String name, boolean defaultEnabled, PlayerData playerData) {
        this.name = name;
        this.defaultEnabled = defaultEnabled;
        this.playerData = playerData;
    }

    public String getName() {
        return name;
    }
    

    public boolean getDefaultEnabled() {
        return defaultEnabled;
    }
    

    public boolean isEnabled(Player player) {
        return SettingsManager.isSettingEnabled(playerData, player, name, defaultEnabled);
    }
    

    public void setEnabled(Player player, boolean enabled) {
        SettingsManager.setSettingEnabled(playerData, player, name, enabled);
    }
    

    public boolean toggle(Player player) {
        boolean currentState = isEnabled(player);
        onToggle(player, !currentState);
        return !currentState;
    }

    public abstract ItemStack getMenuItem(Player player);

    protected abstract void onToggle(Player player, boolean newState);

    public Boolean meetsRequirement(Player player) {
        return null;
    }
}