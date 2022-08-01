package net.javadiscord.javabot.systems.user_preferences.model;

import net.dv8tion.jda.api.interactions.commands.Command;

/**
 * Contains all preferences users can set.
 */
public enum Preference {
	/**
	 * Enables/Disables QOTW reminders.
	 */
	QOTW_REMINDER("Question of the Week Reminder", false);

	private final String name;
	private final boolean defaultState;

	Preference(String name, boolean defaultState) {
		this.name = name;
		this.defaultState = defaultState;
	}

	@Override
	public String toString() {
		return name;
	}

	public boolean getDefaultState() {
		return defaultState;
	}

	public Command.Choice toChoice() {
		return new Command.Choice(name, String.valueOf(ordinal()));
	}
}
