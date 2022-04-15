package net.javadiscord.javabot.util;

import net.dv8tion.jda.api.utils.MarkdownSanitizer;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Contains some utility methods for dealing with Strings.
 */
public class StringUtils {

	private StringUtils() {
	}

	/**
	 * Builds and returns the standard MarkdownSanitizer used to sanitize code blocks.
	 *
	 * @return The built {@link MarkdownSanitizer}.
	 * @see net.javadiscord.javabot.listener.GitHubLinkListener
	 */
	public static MarkdownSanitizer standardSanitizer() {
		return new MarkdownSanitizer()
				.withStrategy(MarkdownSanitizer.SanitizationStrategy.REMOVE)
				.withIgnored(MarkdownSanitizer.ITALICS_A)
				.withIgnored(MarkdownSanitizer.ITALICS_U)
				.withIgnored(MarkdownSanitizer.BOLD)
				.withIgnored(MarkdownSanitizer.SPOILER)
				.withIgnored(MarkdownSanitizer.QUOTE)
				.withIgnored(MarkdownSanitizer.QUOTE_BLOCK)
				.withIgnored(MarkdownSanitizer.UNDERLINE)
				.withIgnored(MarkdownSanitizer.STRIKE);
	}

	/**
	 * Converts a {@link BufferedReader} to a {@link String}.
	 *
	 * @param in The {@link BufferedReader}.
	 * @param from The {@link String}s first line.
	 * @param to The {@link String}s last line.
	 * @return The String object.
	 * @throws IOException If an error occurs.
	 */
	public static String fromBufferedReader(BufferedReader in, int from, int to) throws IOException {
		StringBuilder output = new StringBuilder();
		String line;
		int count = 1;
		while ((line = in.readLine()) != null) {
			if (from > to || count >= from && count <= to) {
				output.append(line).append("\n");
			}
			count++;
		}
		return output.toString();
	}

	/**
	 * Builds a progress bar out of Strings.
	 *
	 * @param current The current value.
	 * @param max The max value.
	 * @param off The String that should be used for the "off" state
	 * @param on The String that should be used for the "on" state
	 * @param length The amount of repetitions.
	 * @return The formatted String.
	 */
	public static String buildProgressBar(double current, double max, String off, String on, int length) {
		if (current > max) current = max;
		double percent = current / max;
		int onLength = (int) (length * percent);
		return on.repeat(onLength) + off.repeat(length - onLength);
	}
}
