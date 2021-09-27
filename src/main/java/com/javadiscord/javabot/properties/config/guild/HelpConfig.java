package com.javadiscord.javabot.properties.config.guild;

import com.javadiscord.javabot.help.AlphabetNamingStrategy;
import com.javadiscord.javabot.help.AnimalNamingStrategy;
import com.javadiscord.javabot.help.ChannelNamingStrategy;
import com.javadiscord.javabot.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Category;

/**
 * Configuration for the guild's help system.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HelpConfig extends GuildConfigItem {
	/**
	 * The id of the channel category that all help channels are in.
	 */
	private long categoryId;

	/**
	 * The strategy to use when naming help channels.
	 */
	private String channelNamingStrategy = "animal";

	/**
	 * If true, the system will manage a fixed set of help channels which are
	 * created in advance. If false, the system will create and remove channels
	 * as needed to maintain the {@link HelpConfig#preferredOpenChannelCount}.
	 * Note that if this is true, the preferred open channel count is ignored.
	 */
	private boolean recycleChannels = false;

	/**
	 * The string which is shown as the 'topic' for open channels.
	 */
	private String openChannelTopic = "Ask your question here!";

	/**
	 * The message that's sent in a recycled help channel to tell users that it
	 * is now open for someone to ask a question.
	 */
	private String reopenedChannelMessage = "**This channel is no longer reserved. Feel free to ask your question here!**";

	/**
	 * The number of open help channels to maintain. If fewer than this many
	 * open channels exist, the system will try to create more.
	 */
	private int preferredOpenChannelCount = 3;

	/**
	 * The string which is prefixed to any open help channel, where users are
	 * free to ask a question.
	 */
	private String openChannelPrefix = "\uD83D\uDFE2";

	/**
	 * The string which is prefixed to any reserved help channel, where a user
	 * has already asked a question and is in the process of getting an answer.
	 */
	private String reservedChannelPrefix = "\u26D4";

	/**
	 * The number of minutes of inactivity before a channel is considered inactive.
	 */
	private int inactivityTimeoutMinutes = 30;

	/**
	 * The number of minutes of inactivity before a previously inactive channel
	 * is removed. This is measured from the time at which the bot determined
	 * the channel to be inactive.
	 */
	private int removeTimeoutMinutes = 60;

	/**
	 * The number of seconds to wait between each help channel update check.
	 */
	private long updateIntervalSeconds = 60;

	public Category getHelpChannelCategory() {
		return getGuild().getCategoryById(this.categoryId);
	}

	public ChannelNamingStrategy getChannelNamingStrategy() {
		return switch (this.channelNamingStrategy) {
			case "alphabet" -> new AlphabetNamingStrategy();
			case "animal" -> new AnimalNamingStrategy();
			default -> throw new IllegalArgumentException("Invalid channel naming strategy.");
		};
	}
}
