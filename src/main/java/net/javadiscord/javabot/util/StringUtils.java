package net.javadiscord.javabot.util;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Contains some utility methods for dealing with Strings.
 */
public class StringUtils {

	private StringUtils() {
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
}
