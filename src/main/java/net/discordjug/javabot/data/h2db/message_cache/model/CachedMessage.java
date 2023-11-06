package net.discordjug.javabot.data.h2db.message_cache.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;

/**
 * Represents a cached Message.
 */
@Data
public class CachedMessage {
	private long messageId;
	private long authorId;
	private String messageContent;
	private List<String> attachments=new ArrayList<>();

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
		cachedMessage.attachments = message
				.getAttachments()
				.stream()
				.map(Attachment::getUrl)
				.toList();
		return cachedMessage;
	}

}
