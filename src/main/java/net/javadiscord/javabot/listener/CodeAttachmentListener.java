package net.javadiscord.javabot.listener;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.util.CodeAttachmentExtractor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Listens for messages containing "message.txt" or similar file attachments
 * containing source code, in order to format the code into one or more code
 * snippet blocks in the Discord channel instead. The code blocks are sent in
 * a series of messages that chain their replies to the original message.
 */
@Slf4j
public class CodeAttachmentListener extends ListenerAdapter {
	private static final Function<Message.Attachment, Boolean> attachmentFilter = attachment ->
			attachment.getFileName().equalsIgnoreCase("message.txt") ||
			Set.of(".java", ".html", ".d", ".c", ".js", ".css").contains(attachment.getFileName().toLowerCase());

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (isCodeMessage(event.getMessage())) {
			new CodeAttachmentExtractor(attachmentFilter)
				.extractCodeBlocks(event.getMessage())
				.handleAsync((blocks, throwable) -> {
					if (throwable != null) {
						log.warn("An error occurred while fetching source code from a message attachment.", throwable);
					} else {
						Message lastMsg = event.getMessage();
						for (var block : blocks) {
							lastMsg = lastMsg.reply(block).allowedMentions(Collections.emptySet()).complete();
						}
					}
					return null;
				});
		}
	}

	/**
	 * Checks if a message is considered to be a valid "code" message that we
	 * can process into code blocks. A message is a code message if it contains
	 * exactly 1 attachment whose name is "message.txt" and whose size does not
	 * exceed 10KB.
	 * @param msg The message to check.
	 * @return True if the message is eligible for processing, or false otherwise.
	 */
	private boolean isCodeMessage(Message msg) {
		if (msg.getAttachments().isEmpty()) return false;
		int totalSize = 0;
		boolean containsCode = false;
		for (var atc : msg.getAttachments()) {
			totalSize += atc.getSize();
			if (attachmentFilter.apply(atc)) {
				containsCode = true;
			}
		}
		return totalSize < 10000 && containsCode;
	}


}
