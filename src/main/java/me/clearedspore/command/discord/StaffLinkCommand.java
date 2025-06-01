package me.clearedspore.command.discord;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.discord.DiscordManager;
import me.clearedspore.util.ChatInputHandler;
import me.clearedspore.util.P;
import org.bukkit.entity.Player;

@CommandAlias("staff-link")
@CommandPermission(P.discord_link)
public class StaffLinkCommand extends BaseCommand {

    private final DiscordManager discordManager;
    private final ChatInputHandler chatInputHandler;

    public StaffLinkCommand(DiscordManager discordManager, ChatInputHandler chatInputHandler) {
        this.discordManager = discordManager;
        this.chatInputHandler = chatInputHandler;
    }


    @Default
    public void onLink(Player player) {
        if (!discordManager.isEnabled()) {
            player.sendMessage(CC.send("&c[Discord] &fDiscord integration is not enabled on this server."));
            return;
        }

        if (discordManager.isLinked(player)) {
            player.sendMessage(CC.send("&c[Discord] &fYour Discord account is already linked. Use &b/staff-unlink&f to unlink it first."));
            return;
        }

        player.sendMessage(CC.send("&a[Discord] &fPlease enter your Discord ID in the chat."));
        player.sendMessage(CC.send("&a[Discord] &fYou can get this by enabling Developer Mode in Discord, right-clicking your name, and selecting 'Copy ID'."));
        player.sendMessage(CC.send("&a[Discord] &fType 'cancel' to cancel this operation."));

        chatInputHandler.awaitChatInput(player, input -> {
            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage(CC.send("&c[Discord] &fOperation cancelled."));
                return;
            }

            if (!input.matches("\\d+")) {
                player.sendMessage(CC.send("&c[Discord] &fInvalid Discord ID format. Please enter a valid Discord ID."));
                return;
            }

            boolean initiated = discordManager.initiateLinking(player, input);
            if (initiated) {
                player.sendMessage(CC.send("&a[Discord] &fA verification code has been sent to the discord linking channel"));
                player.sendMessage(CC.send("&a[Discord] &fRun /staff-verify (code) to finish linking your account"));
            } else {
                player.sendMessage(CC.send("&c[Discord] &fFailed to initiate the linking process. This Discord ID might already be linked to another account."));
            }
        });
    }
}