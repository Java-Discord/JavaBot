package net.javadiscord.javabot.data.h2db.message_cache.model;

import lombok.Data;
import net.dv8tion.jda.api.entities.Message;

/**
 * Represents a cached Message.
 */
@Data
public class CachedMessage {
	private long messageId;
	private long authorId;
	private String messageContent;

	/**
	 * Converts a {@link Message} object to a {@link CachedMessage}.
	 *
	 * @param message The {@link Message} to convert.
	 * @return The built {@link CachedMessage}.
	 */
	public static CachedMessage of(Message message) {
		CachedMessage cachedMessage = new CachedMessage();
		cachedMessage.setMessageId(message.getIdLong());
		cachedMessage.setAuthorId(message.getAuthor().getIdLong());
		cachedMessage.setMessageContent(message.getContentRaw().trim());
		return cachedMessage;
	}

}
