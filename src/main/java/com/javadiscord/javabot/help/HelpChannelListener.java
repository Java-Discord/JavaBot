package com.javadiscord.javabot.help;

import com.javadiscord.javabot.Bot;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

/**
 * This listener is responsible for handling messages that are sent in one or
 * more designated help channels.
 */
public class HelpChannelListener extends ListenerAdapter {

	@Override
	public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
		if (event.getAuthor().isBot() || event.getAuthor().isSystem()) return;

		var config = Bot.config.get(event.getGuild()).getHelp();
		TextChannel channel = event.getChannel();
		var manager = new HelpChannelManager(config);

		// If a message was sent in an open text channel, reserve it.
		if (config.getOpenChannelCategory().equals(channel.getParent())) {
			// Check if the message that was sent is referencing another.
			// If so, try and reserve the channel for the original message's author.
			if (event.getMessage().getMessageReference() != null) {
				event.getMessage().getMessageReference().resolve().queue(message -> {
					if (message.getAuthor().isBot() || message.getAuthor().isSystem()) return;
					event.getGuild().retrieveMember(message.getAuthor()).queue(member -> {
						if (member != null) {
							try {
								manager.reserve(channel, member.getUser(), message);
							} catch (SQLException e) {
								e.printStackTrace();
								channel.sendMessage("An error occurred and this channel could not be reserved.").queue();
							}
						}
					});
				});
			// Otherwise, it's just a single message, so check if the author is allowed to reserve.
			} else if (manager.mayUserReserveChannel(event.getAuthor())) {
				try {
					manager.reserve(channel, event.getAuthor(), event.getMessage());
				} catch (SQLException e) {
					e.printStackTrace();
					channel.sendMessage("An error occurred and this channel could not be reserved.").queue();
				}
			} else {
				event.getMessage().reply(config.getReservationNotAllowedMessage()).queue();
			}
		} else if (config.getDormantChannelCategory().equals(channel.getParent())) {
			// Prevent anyone from sending messages in dormant channels.
			event.getMessage().delete().queue();
		}
	}
}
