package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import me.clearedspore.feature.setting.SettingsManager;
import me.clearedspore.util.P;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@CommandPermission(P.staff_settings)
@CommandAlias("staff-settings|staff-options")
public class StaffSettingsCommand extends BaseCommand {

    private final SettingsManager settingsManager;
    private final JavaPlugin plugin;

    public StaffSettingsCommand(SettingsManager settingsManager, JavaPlugin plugin) {
        this.settingsManager = settingsManager;
        this.plugin = plugin;
    }


    @Default
    private void onSettingMenu(Player player){
        settingsManager.openSettingsMenu(player);
    }
}
