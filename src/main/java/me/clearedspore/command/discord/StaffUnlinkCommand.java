package me.clearedspore.command.discord;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.discord.DiscordManager;
import me.clearedspore.util.PS;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@CommandAlias("staff-unlink")
@CommandPermission(PS.discord_link)
public class StaffUnlinkCommand extends BaseCommand {

    private final DiscordManager discordManager;
    private final JavaPlugin plugin;

    public StaffUnlinkCommand(DiscordManager discordManager, JavaPlugin plugin) {
        this.discordManager = discordManager;
        this.plugin = plugin;
    }

    @Default
    public void onUnlink(Player player) {
        if (!discordManager.isEnabled()) {
            player.sendMessage(CC.send("&c[Discord] &fDiscord integration is not enabled on this server."));
            return;
        }

        if (!discordManager.isLinked(player)) {
            player.sendMessage(CC.send("&c[Discord] &fYour Discord account is not linked. Use &b/staff-link&f to link it."));
            return;
        }

        boolean success = discordManager.unlinkDiscordAccount(player);
        if (success) {
            player.sendMessage(CC.send(plugin.getConfig().getString("discord.messages.unlink-success", "&a[Discord] &fYour Discord account has been unlinked.")));
        } else {
            player.sendMessage(CC.send("&c[Discord] &fFailed to unlink your Discord account. Please try again later."));
        }
    }
}