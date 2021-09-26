package com.javadiscord.javabot.help;

import com.javadiscord.javabot.Bot;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

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
		Category category = channel.getParent();
		if (category == null || !category.equals(config.getHelpChannelCategory())) return;
		var channelManager = new HelpChannelManager(config);

		// If a message was sent in an open text channel, reserve it.
		if (channel.getName().startsWith(config.getOpenChannelPrefix())) {
			channelManager.reserve(channel, event.getAuthor());
		}
	}
}
