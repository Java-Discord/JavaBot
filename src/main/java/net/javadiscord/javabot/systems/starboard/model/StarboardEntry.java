package net.javadiscord.javabot.systems.starboard.model;

import lombok.Data;

@Data
public class StarboardEntry {
	private long originalMessageId;
	private long guildId;
	private long channelId;
	private long authorId;
	private long starboardMessageId;
}
