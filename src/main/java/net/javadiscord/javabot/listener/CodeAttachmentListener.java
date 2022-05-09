package net.javadiscord.javabot.listener;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Listens for messages containing "message.txt" or similar file attachments
 * containing source code, in order to format the code into one or more code
 * snippet blocks in the Discord channel instead. The code blocks are sent in
 * a series of messages that chain their replies to the original message.
 */
public class CodeAttachmentListener extends ListenerAdapter {
	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (isCodeMessage(event.getMessage())) {
			fetchSource(event.getMessage())
				.thenApply(this::processSource)
				.thenAcceptAsync(blocks -> {
					Message lastMsg = event.getMessage();
					for (var block : blocks) {
						lastMsg = lastMsg.reply(block).complete();
					}
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
		return msg.getAttachments().size() == 1 &&
				msg.getAttachments().get(0).getFileName().equals("message.txt") &&
				msg.getAttachments().get(0).getSize() < 10000;
	}

	/**
	 * Fetches the source code attached to a message.
	 * @param msg The message to fetch from.
	 * @return A future that completes when the source code is fetched.
	 */
	private CompletableFuture<String> fetchSource(Message msg) {
		return msg.getAttachments().get(0).retrieveInputStream().thenApply(in -> {
			try {
				String s = new String(in.readAllBytes());
				in.close();
				return s;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * Processes a source code string into a series of smaller code block
	 * snippets for use in discord messages.
	 * @param source The source to process.
	 * @return A list of code block strings.
	 */
	private List<String> processSource(String source) {
		List<String> blocks = new ArrayList<>(5);
		StringBuilder sb = new StringBuilder(2000);
		int currentBlockLength = 0;
		String[] lines = source.split("\\r?\\n");
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			// Append the line to the current block, if we can fit it.
			if (currentBlockLength + line.length() + 1 < 1900) {
				sb.append(line).append('\n');
				currentBlockLength += line.length() + 1;
			} else {
				// Otherwise, finalize this block and start a new one.
				blocks.add(String.format("```java%n%s```", sb));
				sb.setLength(0);
				if (line.length() > 1900) {
					line = String.format("// Line %d omitted because it was too long.", i + 1);
				}
				sb.append(line).append('\n');
				currentBlockLength = line.length() + 1;
			}
		}
		if (currentBlockLength > 0) {
			blocks.add(String.format("```java%n%s```", sb));
		}
		return blocks;
	}
}
