package net.discordjug.javabot.listener.filter;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

/**
 * A class containing modifiable content of a message that has been received.
 * @param event 		The event associated with receiving the message
 * @param messageText 	The text associated with the message
 * @param attachments 	The attachments associated with the message
 * @param embeds 		The embeds associated with the message
 */
public record MessageContent(MessageReceivedEvent event,
							StringBuilder messageText,
							List<Message.Attachment> attachments,
							List<MessageEmbed> embeds) {
}
