package net.javadiscord.javabot.systems.user_preferences.model;

import net.dv8tion.jda.api.interactions.commands.Command;

public interface PreferenceType {
	default String[] getAllowedChoices() {
		return new String[0];
	}

	default Command.Choice[] getDefaultChoices() {
		return new Command.Choice[0];
	}
}
