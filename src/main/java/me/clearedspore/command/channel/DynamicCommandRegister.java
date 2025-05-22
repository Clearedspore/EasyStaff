package me.clearedspore.command.channel;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.easyAPI.util.Logger;
import me.clearedspore.feature.channels.Channel;
import me.clearedspore.feature.channels.ChannelManager;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;


public class DynamicCommandRegister {

    public static void registerDynamicCommands(PaperCommandManager commandManager, ChannelManager channelManager, Logger logger) {
        logger.info("Registering dynamic channel commands...");

        for (Map.Entry<String, List<String>> entry : channelManager.getCommandToChannelMap().entrySet()) {
            String command = entry.getKey();
            String channelId = entry.getValue().get(0);

            try {
                BaseCommand dynamicCommand = createCommandClass(command, channelId, channelManager);

                if (dynamicCommand instanceof DynamicChannelCommand) {
                    commandManager.getCommandReplacements().addReplacement("%commandname%", command);
                }

                commandManager.registerCommand(dynamicCommand);
            } catch (Exception e) {
                logger.error("Failed to register dynamic command: " + command);
                logger.error(e.getMessage());
                e.printStackTrace();
            }
        }
        logger.info("Channel commands loaded.");
    }

    private static BaseCommand createCommandClass(String commandName, String channelId, ChannelManager channelManager) {
        if (commandName.equals("staffchat") || commandName.equals("sc")) {
            return new StaffChatCommand(channelManager, channelId, commandName);
        } else if (commandName.equals("adminchat") || commandName.equals("ac")) {
            return new AdminChatCommand(channelManager, channelId, commandName);
        } else {
            return new DynamicChannelCommand(channelManager, channelId, commandName);
        }
    }
    

    @CommandAlias("staffchat|sc")
    public static class StaffChatCommand extends BaseCommand {
        private final ChannelManager channelManager;
        private final String channelId;
        private final String commandName;
        
        public StaffChatCommand(ChannelManager channelManager, String channelId, String commandName) {
            this.channelManager = channelManager;
            this.channelId = channelId;
            this.commandName = commandName;
        }
        
        @Default
        public void onCommand(Player player) {
            handleChannelToggle(player);
        }
        
        @Default
        @CommandCompletion("@nothing")
        public void onCommandWithMessage(Player player, String[] message) {
            handleChannelMessage(player, message);
        }
        
        private void handleChannelToggle(Player player) {
            Channel channel = channelManager.getChannel(channelId);
            
            if (channel == null) {
                player.sendMessage(CC.sendRed("Staff channel not found."));
                return;
            }
            
            if (!player.hasPermission(channel.getPermission())) {
                player.sendMessage(CC.sendRed("You don't have permission to use staff chat."));
                return;
            }
            
            String currentChannel = channelManager.getPlayerChannel(player);
            boolean toggled = channelManager.togglePlayerChannel(player, channelId);
            
            if (toggled) {
                if (currentChannel.equals(channelId)) {
                    player.sendMessage(CC.sendBlue("You are now talking in &fglobal &bchat."));
                } else {
                    player.sendMessage(CC.translate("You are now talking in " + channel.getName() + " &fchat."));
                }
            } else {
                player.sendMessage(CC.sendRed("Failed to toggle channel."));
            }
        }
        
        private void handleChannelMessage(Player player, String[] message) {
            if (message.length == 0) {
                handleChannelToggle(player);
                return;
            }
            
            Channel channel = channelManager.getChannel(channelId);
            
            if (channel == null) {
                player.sendMessage(CC.sendRed("Staff channel not found."));
                return;
            }
            
            if (!player.hasPermission(channel.getPermission())) {
                player.sendMessage(CC.sendRed("You don't have permission to use staff chat."));
                return;
            }
            
            String fullMessage = String.join(" ", message);
            boolean sent = channelManager.sendChannelMessage(player, channelId, fullMessage);
            
            if (!sent) {
                player.sendMessage(CC.sendRed("Failed to send message to staff chat."));
            }
        }
    }
    

    @CommandAlias("adminchat|ac")
    public static class AdminChatCommand extends BaseCommand {
        private final ChannelManager channelManager;
        private final String channelId;
        private final String commandName;
        
        public AdminChatCommand(ChannelManager channelManager, String channelId, String commandName) {
            this.channelManager = channelManager;
            this.channelId = channelId;
            this.commandName = commandName;
        }
        
        @Default
        public void onCommand(Player player) {
            handleChannelToggle(player);
        }
        
        @Default
        @CommandCompletion("@nothing")
        public void onCommandWithMessage(Player player, String[] message) {
            handleChannelMessage(player, message);
        }
        
        private void handleChannelToggle(Player player) {
            Channel channel = channelManager.getChannel(channelId);
            
            if (channel == null) {
                player.sendMessage(CC.sendRed("Admin channel not found."));
                return;
            }
            
            if (!player.hasPermission(channel.getPermission())) {
                player.sendMessage(CC.sendRed("You don't have permission to use admin chat."));
                return;
            }
            
            String currentChannel = channelManager.getPlayerChannel(player);
            boolean toggled = channelManager.togglePlayerChannel(player, channelId);
            
            if (toggled) {
                if (currentChannel.equals(channelId)) {
                    player.sendMessage(CC.sendBlue("You are now talking in &fglobal &bchat."));
                } else {
                    player.sendMessage(CC.sendBlue("You are now talking in " + channel.getName() + " &#00CCDEchat."));
                }
            } else {
                player.sendMessage(CC.sendRed("Failed to toggle channel."));
            }
        }
        
        private void handleChannelMessage(Player player, String[] message) {
            if (message.length == 0) {
                handleChannelToggle(player);
                return;
            }
            
            Channel channel = channelManager.getChannel(channelId);
            
            if (channel == null) {
                player.sendMessage(CC.sendRed("Admin channel not found."));
                return;
            }
            
            if (!player.hasPermission(channel.getPermission())) {
                player.sendMessage(CC.sendRed("You don't have permission to use admin chat."));
                return;
            }
            
            String fullMessage = String.join(" ", message);
            boolean sent = channelManager.sendChannelMessage(player, channelId, fullMessage);
            
            if (!sent) {
                player.sendMessage(CC.sendRed("Failed to send message to admin chat."));
            }
        }
    }
    

    public static class GenericChannelCommand extends BaseCommand {
        private final ChannelManager channelManager;
        private final String channelId;
        private final String commandName;
        
        public GenericChannelCommand(ChannelManager channelManager, String channelId, String commandName) {
            this.channelManager = channelManager;
            this.channelId = channelId;
            this.commandName = commandName;
        }
        
        @Default
        public void onCommand(Player player) {
            handleChannelToggle(player);
        }
        
        @Default
        @CommandCompletion("@nothing")
        public void onCommandWithMessage(Player player, String[] message) {
            handleChannelMessage(player, message);
        }
        
        protected void handleChannelToggle(Player player) {
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
                    player.sendMessage(CC.sendBlue("You are now talking in &fglobal &bchat."));
                } else {
                    player.sendMessage(CC.sendBlue("You are now talking in " + channel.getName() + " &#00CCDEchat."));
                }
            } else {
                player.sendMessage(CC.sendRed("Failed to toggle channel."));
            }
        }
        
        protected void handleChannelMessage(Player player, String[] message) {
            if (message.length == 0) {
                handleChannelToggle(player);
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

    public static class DynamicChannelCommand extends GenericChannelCommand {
        private final String commandName;
        
        public DynamicChannelCommand(ChannelManager channelManager, String channelId, String commandName) {
            super(channelManager, channelId, commandName);
            this.commandName = commandName;
        }

        @CommandAlias("%commandname")
        @Default
        public void onDynamicCommand(Player player) {
            super.handleChannelToggle(player);
        }

        @CommandAlias("%commandname")
        @Default
        @CommandCompletion("@nothing")
        public void onDynamicCommandWithMessage(Player player, String[] args) {
            super.handleChannelMessage(player, args);
        }
    }
}