package me.clearedspore.command.punishment;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.discord.DiscordManager;
import me.clearedspore.util.P;
import org.bukkit.entity.Player;

@CommandPermission(P.punish)
@CommandAlias("evidence")
public class EvidenceCommand extends BaseCommand {

    private final DiscordManager discordManager;

    public EvidenceCommand(DiscordManager discordManager) {
        this.discordManager = discordManager;
    }

    @Default
    @Syntax("<punishmentId> <evidenceLink>")
    @Description("Provide evidence for a punishment")
    private void onProvideEvidence(Player player, String punishmentId, String evidenceLink) {
        if (punishmentId == null || punishmentId.isEmpty()) {
            player.sendMessage(CC.sendRed("You must provide a punishment ID!"));
            return;
        }

        if (evidenceLink == null || evidenceLink.isEmpty()) {
            player.sendMessage(CC.sendRed("You must provide an evidence link!"));
            return;
        }

        if (!isValidUrl(evidenceLink)) {
            player.sendMessage(CC.sendRed("Please provide a valid URL for the evidence!"));
            return;
        }

        boolean success = discordManager.provideEvidence(player, punishmentId, evidenceLink);
        
        if (!success) {
            player.sendMessage(CC.sendRed("Failed to provide evidence. The punishment may not exist or evidence has already been provided."));
        }
    }
    private boolean isValidUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }
}