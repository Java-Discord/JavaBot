package com.javadiscord.javabot.properties.config.guild;

import com.javadiscord.javabot.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HelpConfig extends GuildConfigItem {
	/**
	 * The string which is prefixed to any open help channel, where users are
	 * free to ask a question.
	 */
	private String openChannelPrefix = "\uD83D\uDFE2";

	/**
	 * The string which is prefixed to any reserved help channel, where a user
	 * has already asked a question and is in the process of getting an answer.
	 */
	private String reservedChannelPrefix = "⛔";

	/**
	 * The string which is prefixed to any inactive reserved help channel, where
	 * a user has asked a question but the channel has been inactive for a set
	 * amount of time.
	 */
	private String inactiveChannelPrefix = "\uD83D\uDFE0";

	/**
	 * The number of seconds of inactivity before a channel is considered inactive.
	 */
	private int inactivityTimeoutSeconds = 1_800;

	/**
	 * The number of seconds to wait between each help channel update check.
	 */
	private long updateIntervalSeconds = 60;
}
