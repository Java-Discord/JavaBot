package net.discordjug.javabot.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for the {@link IndentationHelper} class.
 */
public class IndentationHelperTest {
	/**
	 * Tests the {@link IndentationHelper#formatIndentation(String, IndentationHelper.IndentationType)} method.
	 * @throws IOException if any I/O error occurs indicating an issue with the test
	 */
	@Test
	public void testFormatIndentation() throws IOException {
		String[] unformatted = null;
		String[] formatted = null;
		unformatted = StringResourceCache.load("/Unformatted Strings.txt").split("----");
		formatted = StringResourceCache.load("/Formatted Strings.txt").split("----");

		for (int i = 0, k = 0; i < unformatted.length; i++) {
			assertEquals(formatted[k++], IndentationHelper.formatIndentation(unformatted[i], IndentationHelper.IndentationType.FOUR_SPACES), "Method failed to format a text with four spaces correctly");
			assertEquals(formatted[k++], IndentationHelper.formatIndentation(unformatted[i], IndentationHelper.IndentationType.TWO_SPACES), "Method failed to format a text with two spaces correctly");
			assertEquals(formatted[k++], IndentationHelper.formatIndentation(unformatted[i], IndentationHelper.IndentationType.TABS), "Method failed to format a text with tabs correctly.");
			assertEquals(formatted[k++], IndentationHelper.formatIndentation(unformatted[i], IndentationHelper.IndentationType.NULL), "Method returned a String not matching the input");
		}
	}
}
