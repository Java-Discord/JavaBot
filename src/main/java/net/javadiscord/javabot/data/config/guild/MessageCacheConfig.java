package net.javadiscord.javabot.data.config.guild;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.javadiscord.javabot.data.config.GuildConfigItem;

import java.util.List;

/**
 * Configuration for the bot's message cache.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageCacheConfig extends GuildConfigItem {
	/**
	 * The amount of message that can be cached at once.
	 */
	private int maxCachedMessages = 1000;

	/**
	 * ID of the Message Cache log channel.
	 */
	private long messageCacheLogChannelId = 0;

	/**
	 * The amount of messages after which the DB is synchronized with the local cache.
	 */
	private int messageSynchronizationInterval = 50;

	/**
	 * Channels the Bot should ignore.
	 */
	private List<Long> excludedChannels = List.of();

	/**
	 * Users the Bot should ignore.
	 */
	private List<Long> excludedUsers = List.of();

	public TextChannel getMessageCacheLogChannel() {
		return this.getGuild().getTextChannelById(this.messageCacheLogChannelId);
	}
}
