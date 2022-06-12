package net.javadiscord.javabot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the {@link StringUtils} class.
 */
public class StringUtilsTest {

	/**
	 * Tests the {@link StringUtils#buildTextProgressBar(double, int)} method.
	 */
	@Test
	public void testBuildTextProgressBar() {
		assertThrows(IllegalArgumentException.class, () -> StringUtils.buildTextProgressBar(0.5, 4));
		assertEquals("[   ]", StringUtils.buildTextProgressBar(0, 5));
		assertEquals("[   ]", StringUtils.buildTextProgressBar(-100, 5));
		assertEquals("[███]", StringUtils.buildTextProgressBar(1, 5));
		assertEquals("[███]", StringUtils.buildTextProgressBar(3, 5));
		assertEquals("[    ]", StringUtils.buildTextProgressBar(0.1, 6));
		assertEquals("[    ]", StringUtils.buildTextProgressBar(0.24, 6));
		assertEquals("[█   ]", StringUtils.buildTextProgressBar(0.25, 6));
		assertEquals("[█   ]", StringUtils.buildTextProgressBar(0.45, 6));
		assertEquals("[██  ]", StringUtils.buildTextProgressBar(0.5, 6));
		assertEquals("[██  ]", StringUtils.buildTextProgressBar(0.65, 6));
		assertEquals("[███ ]", StringUtils.buildTextProgressBar(0.75, 6));
		assertEquals("[███ ]", StringUtils.buildTextProgressBar(0.95, 6));
		assertEquals("[████]", StringUtils.buildTextProgressBar(1, 6));
	}
}
