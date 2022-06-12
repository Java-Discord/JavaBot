package net.javadiscord.javabot.util;

import net.dv8tion.jda.api.utils.MarkdownSanitizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	/**
	 * Capitalizes the given word.
	 *
	 * @param word The word to capitalize.
	 * @return The capitalized word.
	 */
	public static String capitalize(String word) {
		if (word == null || word.isEmpty()) return word;
		return word.substring(0, 1).toUpperCase() + word.substring(1);
	}

	/**
	 * Enum that represents the possible ways source text can be indented.
	 */
	public enum IndentationScheme {
		/**
		 * Indentation of two spaces.
		 */
		TWO_SPACES,
		/**
		 * Indentation of four spaces.
		 */
		FOUR_SPACES,
		/**
		 * Indentation of tabs.
		 */
		TABS
	}

	/**
	 * "Squishes" the indentation of a line down to a 2-space indentation,
	 * which is more compact for viewing source code in certain contexts.
	 * @param line The line of code to squish.
	 * @param indentationScheme The indentation scheme of the source code to
	 *                          which this line belongs.
	 * @return The source code line, with indentation squished.
	 */
	public static String squishIndentation(String line, IndentationScheme indentationScheme) {
		return line.replaceFirst("^[ \t]+", " ".repeat(getIndentLevel(line, indentationScheme) * 2));
	}

	/**
	 * Determines the type of indentation used in the given source code text.
	 * @param source The source code to analyze.
	 * @return The indentation scheme that's used for indenting this code.
	 */
	public static IndentationScheme determineIndentationScheme(String source) {
		Pattern indentPattern = Pattern.compile("^(\\s+)");
		List<String> indentationWhitespaces = source.lines()
				.filter(s -> !s.isBlank())
				.map(s -> {
					Matcher m = indentPattern.matcher(s);
					if (m.find()) {
						return m.group(1);
					}
					return null;
				})
				.filter(Objects::nonNull)
				.toList();
		long tabCount = indentationWhitespaces.stream().filter(s -> s.matches("^\t+ ?")).count();
		long space2Count = indentationWhitespaces.stream().filter(s -> s.matches("^(?> {2})+ ?")).count();
		long space4Count = indentationWhitespaces.stream().filter(s -> s.matches("^(?> {4})+ ?")).count();
		// If there are more tab indentations than any other, use this.
		if (tabCount > space2Count && tabCount > space4Count) return IndentationScheme.TABS;
		// If there are more 2-space indentations than 4-space, then we know it is 2-space indentation.
		if (space2Count > space4Count) return IndentationScheme.TWO_SPACES;
		// Otherwise, it must be 4-space indentation.
		return IndentationScheme.FOUR_SPACES;
	}

	/**
	 * Gets the indentation level of a line of source code, based on a variety
	 * of possible indentation patterns.
	 * @param line The line of code.
	 * @param indentationScheme The indentation scheme that is used for the
	 *                          source code that the given line belongs to.
	 * @return An integer representing the indentation level.
	 */
	public static int getIndentLevel(String line, IndentationScheme indentationScheme) {
		switch (indentationScheme) {
			case TABS -> {
				Pattern tabIndentPattern = Pattern.compile("^\t+");
				Matcher mTab = tabIndentPattern.matcher(line);
				if (mTab.find()) {
					return mTab.end();
				}
			}
			case TWO_SPACES -> {
				Pattern space2IndentPattern = Pattern.compile("^[ {2}]+");
				Matcher mSpace2 = space2IndentPattern.matcher(line);
				if (mSpace2.find()) {
					return mSpace2.end() / 2;
				}
			}
			case FOUR_SPACES -> {
				Pattern space4IndentPattern = Pattern.compile("^[ {4}]+");
				Matcher mSpace4 = space4IndentPattern.matcher(line);
				if (mSpace4.find()) {
					return mSpace4.end() / 4;
				}
			}
		}
		return 0;
	}
}
