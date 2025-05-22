package me.clearedspore.feature.channels;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class DiscordChannelInfo {

    private JDA jda;

    public DiscordChannelInfo(String botToken) throws Exception {
        jda = JDABuilder.createDefault(botToken).build();
        jda.awaitReady();
    }

    public String getChannelNameById(String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        return channel != null ? channel.getName() : "Unknown Channel";
    }
}