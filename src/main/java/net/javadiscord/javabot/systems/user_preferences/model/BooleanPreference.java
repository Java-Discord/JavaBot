package net.javadiscord.javabot.systems.user_preferences.model;

import net.dv8tion.jda.api.interactions.commands.Command;

public final class BooleanPreference implements PreferenceType{
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
