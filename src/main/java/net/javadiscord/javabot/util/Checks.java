package net.javadiscord.javabot.util;

import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class Checks {
	private Checks() {}

	public static boolean checkLongInput(OptionMapping mapping) {
		try {
			mapping.getAsLong();
			return true;
		} catch (IllegalStateException | NumberFormatException e) {
			return false;
		}
	}
}
