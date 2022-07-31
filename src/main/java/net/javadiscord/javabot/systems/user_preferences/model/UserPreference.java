package net.javadiscord.javabot.systems.user_preferences.model;

import lombok.Data;

/**
 * Data class which represents a single user preference.
 */
@Data
public class UserPreference {
	private long userId;
	private int ordinal;
	private boolean enabled;

	public Preference getPreference() {
		return Preference.values()[ordinal];
	}
}
