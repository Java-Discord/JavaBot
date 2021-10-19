package com.javadiscord.javabot.utils;

import java.awt.*;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Colors {

	public static Color randomPastel() {
		Random rand = ThreadLocalRandom.current();
		float hue = rand.nextFloat();
		float saturation = (rand.nextInt(2000) + 1000) / 10000f;
		float luminance = 0.9f;
		return Color.getHSBColor(hue, saturation, luminance);
	}
}
