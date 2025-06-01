package me.clearedspore.command.channel;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.channels.Channel;
import me.clearedspore.feature.channels.ChannelManager;
import me.clearedspore.util.P;
import org.bukkit.entity.Player;


@CommandAlias("channel")
@CommandPermission(P.channel)
public class ChannelCommand extends BaseCommand {

    private final ChannelManager channelManager;


    public ChannelCommand(ChannelManager channelManager) {
        this.channelManager = channelManager;
    }


    @Subcommand("list")
    @CommandPermission(P.channel_list)
    public void onList(Player player) {
        player.sendMessage(CC.sendBlue("Available channels:"));
        
        for (Channel channel : channelManager.getAllChannels()) {
            if (player.hasPermission(channel.getPermission())) {
                String currentChannel = channelManager.getPlayerChannel(player);
                String status = currentChannel.equals(channel.getId()) ? " &a(active)" : "";
                
                player.sendMessage(CC.translate("&f- " + channel.getName() + "&f: /" + channel.getCommands().get(0) + status));
            }
        }
        
        String currentChannel = channelManager.getPlayerChannel(player);
        String status = currentChannel.equals("global") ? " &a(active)" : "";
        player.sendMessage(CC.translate("&f- &fGlobal&f: /channel global" + status));
    }

    @Subcommand("global")
    public void onGlobal(Player player) {
        String currentChannel = channelManager.getPlayerChannel(player);
        
        if (currentChannel.equals("global")) {
            player.sendMessage(CC.sendRed("You are already in global chat."));
            return;
        }
        
        channelManager.setPlayerChannel(player, "global");
        player.sendMessage(CC.sendBlue("You are now talking in &fglobal &bchat."));
    }


    @Subcommand("reload")
    @CommandPermission(P.channel_reload)
    public void onReload(Player player) {
        channelManager.reloadChannels();
        player.sendMessage(CC.sendBlue("Channels configuration reloaded."));

        player.sendMessage(CC.sendBlue("Note: You need to restart the server for new channels to be fully registered."));
    }
}