package net.javadiscord.javabot.util;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for generating colors.
 */
public final class ColorUtils {
	private ColorUtils() {
	}

	/**
	 * Generates a random pastel color.
	 *
	 * @return A random pastel color.
	 */
	public static Color randomPastel() {
		Random rand = ThreadLocalRandom.current();
		float hue = rand.nextFloat();
		float saturation = (rand.nextInt(2000) + 1000) / 10000f;
		float luminance = 0.9f;
		return Color.getHSBColor(hue, saturation, luminance);
	}

	public static String toString(Color color) {
		if (color == null) return null;
		return "#" + Integer.toHexString(color.getRGB()).substring(2).toUpperCase();
	}
}
