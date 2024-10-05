package net.discordjug.javabot.util;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReference;
import net.dv8tion.jda.api.entities.messages.MessageSnapshot;

/**
 * Utility class for JDA messages.
 */
public class MessageUtils {
	
	private MessageUtils() {
		//prevent instantiation
	}
	
	/**
	 * Gets the actual content of a message.
	 * In case of forwarded messages, this gets the content of the forwarded message.
	 * @param msg the message to check
	 * @return the content of the passed message
	 */
	public static String getMessageContent(Message msg) {
		//see https://github.com/discord-jda/JDA/releases/tag/v5.1.2
		MessageReference messageReference = msg.getMessageReference();
		if (messageReference != null && messageReference.getType() == MessageReference.MessageReferenceType.FORWARD) {
			MessageSnapshot snapshot = msg.getMessageSnapshots().get(0);
			if (snapshot != null) {
				return snapshot.getContentRaw();
			}
		}
		return msg.getContentRaw();
	}
}