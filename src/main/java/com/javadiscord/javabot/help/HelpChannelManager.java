package com.javadiscord.javabot.help;

import com.javadiscord.javabot.properties.config.guild.HelpConfig;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;

public class HelpChannelManager {
	/**
	 * Opens a text channel so that it is ready for a new question.
	 * @param channel The channel to open.
	 * @param config The configuration properties.
	 */
	public void open(TextChannel channel, HelpConfig config) {
		String rawName = channel.getName().substring(config.getReservedChannelPrefix().length());
		channel.getManager().setName(config.getOpenChannelPrefix() + rawName).queue();
		channel.getManager().setPosition(0).queue();
		removeAllMessages(channel);
	}

	/**
	 * Utility method to remove all messages from a channel.
	 * @param channel The channel to remove messages from.
	 */
	private void removeAllMessages(TextChannel channel) {
		List<Message> messages;
		do {
			messages = channel.getHistory().retrievePast(50).complete();
			if (messages.isEmpty()) break;
			if (messages.size() == 1) {
				channel.deleteMessageById(messages.get(0).getIdLong()).complete();
			} else {
				channel.deleteMessages(messages).complete();
			}
		} while (!messages.isEmpty());
	}
}
