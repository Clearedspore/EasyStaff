package me.clearedspore.feature.staffmode;

import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaffMode {
    private final String name;
    private final String permission;
    private final int weight;
    private final GameMode gamemode;
    private final boolean flight;
    private final boolean invulnerable;
    private final boolean pvp;
    private final boolean blockPlace;
    private final boolean blockBreak;
    private final boolean itemPickup;
    private final boolean itemDrop;
    private final boolean silentChest;
    private final boolean inventory;
    private final List<String> enableCommands;
    private final List<String> disableCommands;
    private final boolean vanished;
    private final boolean chat;
    private final Map<String, Integer> items;

    public StaffMode(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.flight = config.getBoolean("flight");
        this.name = config.getString("name");
        this.permission = config.getString("permission");
        this.weight = config.getInt("weight");
        this.gamemode = GameMode.valueOf(config.getString("gamemode", "SURVIVAL").toUpperCase());
        this.invulnerable = config.getBoolean("invulnerable", false);
        this.pvp = config.getBoolean("pvp", false);
        this.blockPlace = config.getBoolean("block-place", false);
        this.blockBreak = config.getBoolean("block-break", false);
        this.itemPickup = config.getBoolean("item-pickup", false);
        this.itemDrop = config.getBoolean("item-drop", false);
        this.silentChest = config.getBoolean("silent-chest", false);
        this.inventory = config.getBoolean("inventory", false);
        this.enableCommands = config.getStringList("enable-commands");
        this.disableCommands = config.getStringList("disable-commands");
        this.vanished = config.getBoolean("vanished", false);
        this.chat = config.getBoolean("chat", true);
        
        this.items = new HashMap<>();
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                if (itemsSection.isConfigurationSection(key)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                    if (itemSection.getBoolean("enabled", true)) {
                        items.put(key, itemSection.getInt("slot"));
                    }
                } else {
                    items.put(key, itemsSection.getInt(key));
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission;
    }

    public int getWeight() {
        return weight;
    }

    public GameMode getGamemode() {
        return gamemode;
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public boolean isPvp() {
        return pvp;
    }

    public boolean isBlockPlace() {
        return blockPlace;
    }

    public boolean isFlight(){
        return flight;
    }

    public boolean isBlockBreak() {
        return blockBreak;
    }

    public boolean isItemPickup() {
        return itemPickup;
    }

    public boolean isItemDrop() {
        return itemDrop;
    }

    public boolean isSilentChest() {
        return silentChest;
    }

    public boolean isInventory() {
        return inventory;
    }

    public List<String> getEnableCommands() {
        return enableCommands;
    }

    public List<String> getDisableCommands() {
        return disableCommands;
    }

    public boolean isVanished() {
        return vanished;
    }

    public boolean isChat() {
        return chat;
    }

    public Map<String, Integer> getItems() {
        return items;
    }
}