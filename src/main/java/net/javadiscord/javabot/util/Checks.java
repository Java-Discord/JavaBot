package net.javadiscord.javabot.util;

import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

public class Checks {
	private Checks() {}

	public static boolean checkLongInput(@NotNull OptionMapping mapping) {
		try {
			mapping.getAsLong();
			return true;
		} catch (IllegalStateException | NumberFormatException e) {
			return false;
		}
	}

	public static boolean checkGuild(@NotNull Interaction interaction) {
		return interaction.isFromGuild() && interaction.getGuild() != null;
	}
}
