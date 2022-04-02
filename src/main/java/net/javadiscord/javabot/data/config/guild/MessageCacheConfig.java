package net.javadiscord.javabot.data.config.guild;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.TextChannel;
import net.javadiscord.javabot.data.config.GuildConfigItem;

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
	private long messageCacheLogChannelId;

	/**
	 * Channels the Bot should ignore.
	 */
	private Long[] excludedChannels;

	/**
	 * Users the Bot should ignore.
	 */
	private Long[] excludedUsers;

	public TextChannel getMessageCacheLogChannel() {
		return this.getGuild().getTextChannelById(this.messageCacheLogChannelId);
	}
}
