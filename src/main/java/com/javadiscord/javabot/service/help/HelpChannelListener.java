package com.javadiscord.javabot.service.help;

import com.javadiscord.javabot.Bot;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.sql.SQLException;

/**
 * This listener is responsible for handling messages that are sent in one or
 * more designated help channels.
 */
public class HelpChannelListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getAuthor().isSystem() || event.getChannelType() != ChannelType.TEXT)
            return;

        var config = Bot.config.get(event.getGuild()).getHelp();
        TextChannel channel = event.getTextChannel();
        var manager = new HelpChannelManager(config);

        // If a message was sent in an open text channel, reserve it.
        if (config.getOpenChannelCategory().equals(channel.getParentCategory())) {
            if (manager.mayUserReserveChannel(event.getAuthor())) {
                try {
                    manager.reserve(channel, event.getAuthor(), event.getMessage());
                } catch (SQLException e) {
                    e.printStackTrace();
                    channel.sendMessage("An error occurred and this channel could not be reserved.").queue();
                }
            } else {
                event.getMessage().replyEmbeds(HelpChannelManager.getHelpChannelEmbed(
                        config.getReservationNotAllowedMessage(),
                        config.getReservedColor())
                ).queue();
            }
        } else if (config.getDormantChannelCategory().equals(channel.getParentCategory())) {
            // Prevent anyone from sending messages in dormant channels.
            event.getMessage().delete().queue();
        }
    }
}
