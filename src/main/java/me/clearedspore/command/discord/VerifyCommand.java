package me.clearedspore.command.discord;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.discord.DiscordManager;
import me.clearedspore.util.P;
import org.bukkit.entity.Player;

@CommandAlias("staff-verify")
@CommandPermission(P.discord_link)
public class VerifyCommand extends BaseCommand {

    private final DiscordManager discordManager;

    public VerifyCommand(DiscordManager discordManager) {
        this.discordManager = discordManager;
    }

    @Default
    public void onVerify(Player player, String verificationCode) {
        if (!discordManager.isEnabled()) {
            player.sendMessage(CC.send("&c[Discord] &fDiscord integration is not enabled on this server."));
            return;
        }

        boolean success = discordManager.confirmLinking(player, verificationCode);
        if (success) {
            player.sendMessage(CC.send("&a[Discord] &fYour Discord account has been successfully linked."));
        } else {
            player.sendMessage(CC.send("&c[Discord] &fInvalid verification code or linking process failed."));
        }
    }
}