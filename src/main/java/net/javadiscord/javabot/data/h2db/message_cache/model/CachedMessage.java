package net.javadiscord.javabot.data.h2db.message_cache.model;

import lombok.Data;

/**
 * Represents a cached Message.
 */
@Data
public class CachedMessage {
	private long messageId;
	private long authorId;
	private String messageContent;
}
