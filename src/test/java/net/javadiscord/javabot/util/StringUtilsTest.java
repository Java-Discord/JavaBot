package net.javadiscord.javabot.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static net.javadiscord.javabot.util.StringUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringUtilsTest {
	@Test
	public void testSquishIndentation() {
		assertEquals("  Hello", squishIndentation("\tHello", IndentationScheme.TABS));
		assertEquals("  Hello", squishIndentation("    Hello", IndentationScheme.FOUR_SPACES));
		assertEquals("  Hello", squishIndentation("  Hello", IndentationScheme.TWO_SPACES));
		assertEquals("    Hello", squishIndentation("\t\tHello", IndentationScheme.TABS));
		assertEquals("    Hello", squishIndentation("        Hello", IndentationScheme.FOUR_SPACES));
		assertEquals("    Hello", squishIndentation("    Hello", IndentationScheme.TWO_SPACES));
	}

	@Test
	public void testDetermineIndentationScheme() {
		assertEquals(IndentationScheme.FOUR_SPACES, determineIndentationScheme(StringResourceCache.load("/string_utils_content/4-spaces.txt")));
		assertEquals(IndentationScheme.TWO_SPACES, determineIndentationScheme(StringResourceCache.load("/string_utils_content/2-spaces.txt")));
		assertEquals(IndentationScheme.TABS, determineIndentationScheme(StringResourceCache.load("/string_utils_content/tabs.txt")));
	}

	@Test
	public void testGetIndentLevel() {
		testIndentLevelWithScheme(IndentationScheme.TABS, List.of(
				new Pair<>(0, "No indentation"),
				new Pair<>(0, " No indentation"),
				new Pair<>(0, "  No indentation"),
				new Pair<>(0, "    No indentation"),
				new Pair<>(1, "\tHello world"),
				new Pair<>(1, "\t Hello world"),
				new Pair<>(1, "\t  Hello world"),
				new Pair<>(1, "\t    Hello world"),
				new Pair<>(2, "\t\tHello world")
		));
		testIndentLevelWithScheme(IndentationScheme.TWO_SPACES, List.of(
				new Pair<>(0, "No indentation"),
				new Pair<>(0, " No indentation"),
				new Pair<>(0, "\tNo indentation"),
				new Pair<>(0, " \tNo indentation"),
				new Pair<>(1, "  Hello world"),
				new Pair<>(1, "   Hello world"),
				new Pair<>(1, "  \tHello world"),
				new Pair<>(2, "    Hello world")
		));
		testIndentLevelWithScheme(IndentationScheme.FOUR_SPACES, List.of(
				new Pair<>(0, "No indentation"),
				new Pair<>(0, " No indentation"),
				new Pair<>(0, "  No indentation"),
				new Pair<>(0, "\tNo indentation"),
				new Pair<>(1, "    Hello world"),
				new Pair<>(1, "    \tHello world"),
				new Pair<>(1, "     Hello world"),
				new Pair<>(1, "      Hello world"),
				new Pair<>(2, "        Hello world")
		));
	}

	private void testIndentLevelWithScheme(IndentationScheme indentationScheme, List<Pair<Integer, String>> cases) {
		for (var c : cases) {
			assertEquals(c.first(), getIndentLevel(c.second(), indentationScheme));
		}
	}
}
