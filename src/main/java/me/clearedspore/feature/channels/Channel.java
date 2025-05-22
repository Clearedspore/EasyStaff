package me.clearedspore.feature.channels;

import java.util.List;


public class Channel {
    private final String id;
    private final List<String> commands;
    private final String discordWebhook;
    private final String name;
    private final String prefix;
    private final String permission;


    public Channel(String id, List<String> commands, String discordWebhook, String name, String prefix, String permission) {
        this.id = id;
        this.commands = commands;
        this.discordWebhook = discordWebhook;
        this.name = name;
        this.prefix = prefix;
        this.permission = permission;
    }


    public Channel(String id, List<String> commands, String name, String prefix, String permission) {
        this.id = id;
        this.commands = commands;
        this.discordWebhook = null;
        this.name = name;
        this.prefix = prefix;
        this.permission = permission;
    }

    public String getDiscordId() {
        return discordWebhook;
    }

    public String getId() {
        return id;
    }

    public List<String> getCommands() {
        return commands;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getPermission() {
        return permission;
    }
}