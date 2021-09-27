package com.javadiscord.javabot.help;

import com.javadiscord.javabot.properties.config.guild.HelpConfig;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * This manager is responsible for all the main interactions that affect the
 * help system's channels.
 */
@Slf4j
public class HelpChannelManager {
	private final HelpConfig config;

	public HelpChannelManager(HelpConfig config) {
		this.config = config;
	}

	public boolean isOpen(TextChannel channel) {
		return channel.getName().startsWith(config.getOpenChannelPrefix());
	}

	public boolean isReserved(TextChannel channel) {
		return channel.getName().startsWith(config.getReservedChannelPrefix());
	}

	/**
	 * Opens a text channel so that it is ready for a new question.
	 */
	public void openNew() {
		var category = config.getHelpChannelCategory();
		if (category == null) throw new IllegalStateException("Missing help channel category. Cannot open a new help channel.");
		String name = this.config.getChannelNamingStrategy().getName(category.getTextChannels(), config);
		category.createTextChannel(name).queue(channel -> {
			channel.getManager().setPosition(0).setTopic(this.config.getOpenChannelTopic()).queue();
			log.info("Created new help channel {}.", channel.getAsMention());
		});
	}

	/**
	 * Reserves a text channel for a user.
	 * @param channel The channel to reserve.
	 * @param reservingUser The user who is reserving the channel.
	 */
	public void reserve(TextChannel channel, User reservingUser) {
		String rawChannelName = channel.getName().substring(config.getOpenChannelPrefix().length());
		channel.getManager()
				.setName(config.getReservedChannelPrefix() + rawChannelName)
				.setPosition(Objects.requireNonNull(channel.getParent()).getTextChannels().size())
				.setTopic(String.format(
						"Reserved for %s\n(_id=%s_)",
						reservingUser.getAsTag(),
						reservingUser.getId()
				)).queue(unused -> {
					if (config.getReservedChannelMessage() != null) {
						channel.sendMessage(config.getReservedChannelMessage()).queue();
					}
				});
		log.info("Reserved channel {} for {}.", channel.getAsMention(), reservingUser.getAsTag());
		if (!this.config.isRecycleChannels()) {
			// Open a new channel right away to maintain the preferred number of open channels.
			openNew();
		}
	}

	/**
	 * Gets the owner of a reserved channel.
	 * @param channel The channel to get the owner of.
	 * @return The user who reserved the channel, or null.
	 */
	public User getReservedChannelOwner(TextChannel channel) {
		var pattern = Pattern.compile("\\(_id=(\\d+)_\\)");
		if (channel.getTopic() != null) {
			var matcher = pattern.matcher(channel.getTopic());
			if (matcher.find()) {
				String id = matcher.group(1);
				return channel.getJDA().retrieveUserById(id).complete();
			}
		}
		return null;
	}

	/**
	 * Unreserves a channel after it no longer needs to be reserved.
	 * @param channel The channel to unreserve.
	 */
	public void unreserveChannel(TextChannel channel) {
		if (this.config.isRecycleChannels()) {
			String rawName = channel.getName().substring(this.config.getReservedChannelPrefix().length());
			channel.getManager()
					.setName(this.config.getOpenChannelPrefix() + rawName)
					.setTopic(this.config.getOpenChannelTopic())
					.setPosition(0).queue();
			channel.sendMessage(this.config.getReopenedChannelMessage()).queue();
		} else {
			channel.delete().queue();
		}
	}
}
