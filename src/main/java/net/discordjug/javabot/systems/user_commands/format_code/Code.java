package net.discordjug.javabot.systems.user_commands.format_code;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds a piece of code and its {@link Language}, and turns it into
 * Discord-friendly representations that respect Discord's 2000-character limit.
 */
public class Code {

	/**
	 * Maximum characters per chunk. Discord's hard limit per message is 2000;
	 * the remaining headroom covers the surrounding ```language fences.
	 */
	private static final int MAX_SIZE = 1980;

	private final Language language;
	private final String content;

	/**
	 * Creates a code block for the given language and content.
	 *
	 * @param language the language the code is written in, used for syntax highlighting
	 * @param content  the raw, already-sanitized code to format
	 */
	public Code(Language language, String content) {
		this.language = language;
		this.content = content;
	}

	public String getContent() {
		return content;
	}

	/**
	 * Splits {@link #content} into pieces that each fit within {@link #MAX_SIZE},
	 * breaking on newlines where possible so lines are not cut in half.
	 *
	 * @return the content split into chunks that each fit within the limit
	 */
	private List<String> toDiscordChunks() {
		List<String> chunks = new ArrayList<>();
		String remaining = content;

		while (remaining.length() > MAX_SIZE) {
			int split = remaining.lastIndexOf('\n', MAX_SIZE);
			if (split <= 0) {
				// No newline in range (or only at the very start) -> hard cut,
				// guaranteeing progress so this can never infinite-loop.
				chunks.add(remaining.substring(0, MAX_SIZE));
				remaining = remaining.substring(MAX_SIZE);
			} else {
				chunks.add(remaining.substring(0, split));
				remaining = remaining.substring(split + 1); // +1 consumes the '\n'
			}
		}
		chunks.add(remaining);
		return chunks;
	}

	/**
	 * Splits the content into chunks that each fit within Discord's character limit and wraps
	 * every chunk in a language-tagged code block.
	 *
	 * @return the formatted code-block messages, one per Discord message
	 */
	public List<String> toDiscordMessages() {
		return toDiscordChunks()
				.stream()
				.map(chunk -> String.format("```%s\n%s\n```", language.getDiscordName(), chunk))
				.toList();
	}
}
