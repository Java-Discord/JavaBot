package net.discordjug.javabot.systems.user_preferences.model;

import net.dv8tion.jda.api.interactions.commands.Command;

/**
 * Represents a {@link Preference} of the {@link Boolean} type.
 */
public final class BooleanPreference implements PreferenceType {
	@Override
	public String[] getAllowedChoices() {
		return new String[]{
				"true", "false"
		};
	}

	@Override
	public Command.Choice[] getDefaultChoices() {
		return new Command.Choice[]{
				new Command.Choice("Enable", "true"),
				new Command.Choice("Disable", "false")
		};
	}
}
