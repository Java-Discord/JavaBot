package net.javadiscord.javabot.systems.user_preferences.model;

/**
 * Contains all preferences users can set.
 */
public enum Preference {
	/**
	 * Enables/Disables QOTW reminders.
	 */
	QOTW_REMINDER("Question of the Week Reminder", "false", new BooleanPreference()),
	/**
	 * A boolean preference which enables/disables the close reminder in forum help posts.
	 */
	FORUM_CLOSE_REMINDER("Help Forum Close Reminder", "true", new BooleanPreference());

	private final String name;
	private final String defaultState;
	private final PreferenceType type;

	Preference(String name, String defaultState, PreferenceType type) {
		this.name = name;
		this.defaultState = defaultState;
		this.type = type;
	}

	@Override
	public String toString() {
		return name;
	}

	public String getDefaultState() {
		return defaultState;
	}

	public PreferenceType getType() {
		return type;
	}
}
