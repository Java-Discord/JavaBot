package net.javadiscord.javabot.systems.user_preferences.model;

/**
 * Contains all preferences users can set.
 */
public enum Preference {
	/**
	 * Enables/Disables QOTW reminders.
	 */
	QOTW_REMINDER("Question of the Week Reminder");

	private final String name;

	Preference(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
