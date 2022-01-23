package net.javadiscord.javabot.util;

import java.awt.*;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for generating colors.
 */
public class ColorUtils {

	private ColorUtils() {}

	/**
	 * Generates a random pastel color.
	 * @return A random pastel color.
	 */
	public static Color randomPastel() {
		Random rand = ThreadLocalRandom.current();
		float hue = rand.nextFloat();
		float saturation = (rand.nextInt(2000) + 1000) / 10000f;
		float luminance = 0.9f;
		return Color.getHSBColor(hue, saturation, luminance);
	}
}
