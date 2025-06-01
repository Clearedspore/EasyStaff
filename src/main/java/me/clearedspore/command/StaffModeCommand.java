package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.staffmode.StaffModeManager;
import me.clearedspore.util.P;
import org.bukkit.entity.Player;

@CommandAlias("staffmode|sm|staff|modmode")
@CommandPermission(P.staffmode)
public class StaffModeCommand extends BaseCommand {
    
    private final StaffModeManager staffModeManager;
    
    public StaffModeCommand(StaffModeManager staffModeManager) {
        this.staffModeManager = staffModeManager;
    }
    
    @Default
    public void onStaffMode(Player player) {
        if (staffModeManager.isInStaffMode(player)) {
            staffModeManager.disableStaffMode(player);
        } else {
            staffModeManager.enableStaffMode(player);
        }
    }
    
    @Subcommand("enable")
    @CommandCompletion("@staffModes")
    public void onEnable(Player player, @Optional String modeName) {
        if (modeName == null) {
            staffModeManager.enableStaffMode(player);
        } else {
            staffModeManager.enableStaffMode(player, modeName);
        }
    }
    
    @Subcommand("disable")
    public void onDisable(Player player) {
        staffModeManager.disableStaffMode(player);
    }
    
    @Subcommand("list")
    public void onList(Player player) {
        player.sendMessage(CC.sendBlue("Available Staff Modes:"));
        for (String mode : staffModeManager.getAvailableModesForPlayer(player)) {
            player.sendMessage(CC.sendGreen(" - " + mode));
        }
    }
}