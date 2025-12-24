package net.discordjug.javabot.data.h2db.message_cache.model;

import java.util.ArrayList;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.discordjug.javabot.util.MessageUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;

/**
 * Represents a cached Message.
 */
@Getter
@EqualsAndHashCode
@ToString
public class CachedMessage {
	private final long messageId;
	private final long authorId;
	private String messageContent;
	private List<String> attachments=new ArrayList<>();
	
	private CachedMessage(long messageId, long authorId) {
		this.messageId = messageId;
		this.authorId = authorId;
	}
	
	/**
	 * Creates a {@link CachedMessage} with the given information.
	 * @param messageId The Discord ID of the message
	 * @param authorId The Discord ID of the message author
	 * @param messageContent the textual content of the message
	 * @param attachments The attachment URLs
	 */
	public CachedMessage(long messageId, long authorId, String messageContent, List<String> attachments) {
		super();
		this.messageId = messageId;
		this.authorId = authorId;
		this.messageContent = messageContent;
		this.attachments = List.copyOf(attachments);
	}

	/**
	 * Converts a {@link Message} object to a {@link CachedMessage}.
	 *
	 * @param message The {@link Message} to convert.
	 * @return The built {@link CachedMessage}.
	 */
	public static CachedMessage of(Message message) {
		CachedMessage cachedMessage = new CachedMessage(message.getIdLong(), message.getAuthor().getIdLong());
		cachedMessage.init(message);
		return cachedMessage;
	}
	
	/**
	 * Resets the current {@link CachedMessage} to have the content of the passed {@link Message}.
	 * @param message the {@link Message} this object is set to.
	 */
	public void init(Message message) {
		messageContent = MessageUtils.getMessageContent(message).trim();
		this.attachments = message
				.getAttachments()
				.stream()
				.map(Attachment::getUrl)
				.toList();
	}
	
	public void setMessageContent(String messageContent) {
		this.messageContent = messageContent;
	}
}
