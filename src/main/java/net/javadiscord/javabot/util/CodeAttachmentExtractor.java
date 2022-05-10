package net.javadiscord.javabot.util;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.javadiscord.javabot.Bot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A utility that extracts source code from Discord message attachments.
 */
@Slf4j
public class CodeAttachmentExtractor {
	private final Function<Message.Attachment, Boolean> filterFunction;

	/**
	 * Constructs the extractor with a specified filter function.
	 * @param filterFunction A function that determines whether a message
	 *                       attachment should be parsed for source text.
	 */
	public CodeAttachmentExtractor(Function<Message.Attachment, Boolean> filterFunction) {
		this.filterFunction = filterFunction;
	}

	/**
	 * Constructs the extractor with a default filter function that simply
	 * allows all attachments to be parsed.
	 */
	public CodeAttachmentExtractor() {
		this(attachment -> true);
	}

	/**
	 * Extracts source text from a message and formats it into an ordered list
	 * of code block strings, ready to be sent as messages in a text channel.
	 * @param msg The message to extract code from.
	 * @return A future containing an ordered list of code block strings. Note
	 * that this future may complete exceptionally if it was not possible to
	 * retrieve source texts from the message's attachments.
	 */
	public CompletableFuture<List<String>> extractCodeBlocks(Message msg) {
		return fetchSource(msg)
			.thenApply(sources -> sources.stream()
				.flatMap(s -> processSource(s).stream())
				.toList());
	}

	/**
	 * Fetches a list of source texts from a message's attachments, filtered by
	 * this extractor's {@link CodeAttachmentExtractor#filterFunction}.
	 * @param msg The message to fetch content from.
	 * @return A future containing an ordered list of strings, that being the
	 * list of source texts obtained from the message's attachments.
	 */
	public CompletableFuture<List<String>> fetchSource(Message msg) {
		return CompletableFuture.supplyAsync(
			() -> msg.getAttachments().stream()
				.filter(filterFunction::apply)
				.map(atc -> atc.retrieveInputStream().thenApply(in -> {
						try (InputStream is = in) {
							return new String(is.readAllBytes(), StandardCharsets.UTF_8);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}).join()
			).toList(),
			Bot.asyncPool
		);
	}

	/**
	 * Processes a source code string into a series of smaller code block
	 * snippets for use in discord messages.
	 * @param source The source to process.
	 * @return A list of code block strings.
	 */
	private List<String> processSource(String source) {
		StringUtils.IndentationScheme indentation = StringUtils.determineIndentationScheme(source);
		List<String> blocks = new ArrayList<>(5);
		StringBuilder sb = new StringBuilder(2000);
		int currentBlockLength = 0;
		String[] lines = source.split("\\r?\\n");
		for (int i = 0; i < lines.length; i++) {
			String line = StringUtils.squishIndentation(lines[i], indentation);
			// Append the line to the current block, if we can fit it.
			if (currentBlockLength + line.length() + 1 < 1900) {
				sb.append(line).append('\n');
				currentBlockLength += line.length() + 1;
			} else {
				// Otherwise, finalize this block and start a new one.
				blocks.add(String.format("```java\n%s```", sb));
				sb.setLength(0);
				if (line.length() > 1900) {
					line = String.format("// Line %d omitted because it was too long.", i + 1);
				}
				sb.append(line).append('\n');
				currentBlockLength = line.length() + 1;
			}
		}
		if (currentBlockLength > 0) {
			blocks.add(String.format("```java\n%s```", sb));
		}
		return blocks;
	}
}
