package me.clearedspore.command.channel;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.easyAPI.util.StringUtil;
import me.clearedspore.feature.channels.Channel;
import me.clearedspore.feature.channels.ChannelManager;
import me.clearedspore.util.PS;
import org.bukkit.entity.Player;

import java.util.Locale;

public class DynamicChannelCommand extends BaseCommand {

    private final ChannelManager channelManager;
    private final String channelId;
    private final String commandName;


    public DynamicChannelCommand(ChannelManager channelManager, String channelId, String commandName) {
        this.channelManager = channelManager;
        this.channelId = channelId;
        this.commandName = commandName;
    }
    
    public String getCommandName() {
        return commandName;
    }

    @Default
    @CommandAlias("__dynamic_channel_command")
    public void onCommand(Player player) {
        Channel channel = channelManager.getChannel(channelId);

        if (channel == null) {
            player.sendMessage(CC.sendRed("Channel not found."));
            return;
        }

        if (!player.hasPermission(channel.getPermission())) {
            player.sendMessage(CC.sendRed("You don't have permission to use this channel."));
            return;
        }
        
        String currentChannel = channelManager.getPlayerChannel(player);
        boolean toggled = channelManager.togglePlayerChannel(player, channelId);
        
        if (toggled) {
            if (currentChannel.equals(channelId)) {
                player.sendMessage(CC.sendBlue("You are now talking in &fglobal &#00CCDEchat."));
            } else {
                player.sendMessage(CC.sendBlue("You are now talking in " + channel.getName() + " &#00CCDEchat."));
            }
        } else {
            player.sendMessage(CC.sendRed("Failed to toggle channel."));
        }
    }


    @Default
    @CommandAlias("__dynamic_channel_command_with_message")
    @CommandCompletion("@nothing")
    public void onCommandWithMessage(Player player, String... messageParts) {
        String message = StringUtil.joinWithSpaces(messageParts);

        if (messageParts.length == 0) {
            onCommand(player);
            return;
        }
        
        Channel channel = channelManager.getChannel(channelId);
        
        if (channel == null) {
            player.sendMessage(CC.sendRed("Channel not found."));
            return;
        }
        
        if (!player.hasPermission(channel.getPermission())) {
            player.sendMessage(CC.sendRed("You don't have permission to use this channel."));
            return;
        }

        String fullMessage = String.join(" ", message);

        boolean sent = channelManager.sendChannelMessage(player, channelId, fullMessage);
        
        if (!sent) {
            player.sendMessage(CC.sendRed("Failed to send message to channel."));
        }
    }
}