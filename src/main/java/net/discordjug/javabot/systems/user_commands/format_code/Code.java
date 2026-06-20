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

	private Language language;
	private final String content;

	public Code(Language language, String content) {
		this.language = language;
		this.content = content;
	}

	public String getContent() {
		return content;
	}

	public Language getLanguage() {
		return language;
	}

	public void setLanguage(Language language) {
		this.language = language;
	}

	/**
	 * Splits {@link #content} into pieces that each fit within {@link #MAX_SIZE},
	 * breaking on newlines where possible so lines are not cut in half.
	 */
	public List<String> toDiscordChunks() {
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

	/** Wraps each chunk in a language-tagged Discord code block. */
	public List<String> toDiscordMessages() {
		return toDiscordChunks()
				.stream()
				.map(chunk -> String.format("```%s\n%s\n```", language.getDiscordName(), chunk))
				.toList();
	}
}
